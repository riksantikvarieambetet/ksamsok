package se.raa.ksamsok.harvest;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokUriPrefix;




/**
 * Handler för xml-parsning som lagrar poster i repositoryt och gör commit med jämna mellanrum
 * (för att inte oracle ska få spunk, derby klarar det). Formatet på xml:en ska vara samma som
 * för RawWrite, dvs i princip OAI-PMH med en omslutande tagg.
 */
public class OAIPMHHandler extends DefaultHandler {

	public static final String VIAF = "VIAF";
	public static final String KUNGLIGA_BIBLIOTEKET = "Kungliga biblioteket";
	public static final String HASHTAG = "#";
	public static final String SLASH = "/";
	public static final String KULTURARVSDATA_PERIOD_AUTH_URI = "http://kulturarvsdata.se/resurser/aukt/srdb/period#";
	public static final String MIS_AUTH_URI = "http://mis.historiska.se/rdf/period";
	public static final String VIAF_AUTH_URI = "http://viaf.org/viaf";
	public static final String LIBRIS_AUTH_URI = "http://libris.kb.se/resource/auth";
	private static final String AGENT = "agent";
	private static final String FROM_PERIOD = "fromPeriod";
	private static final String TO_PERIOD = "toPeriod";
	private static final String NAME_ID = "nameId";
	private static final String NAME_AUTH = "nameAuth";
	private static final String TO_PERIOD_ID = "toPeriodId";
	private static final String FROM_PERIOD_ID = "fromPeriodId";
	private static final String PERIOD_AUTH = "periodAuth";
	private static final String RDF_NS_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String RESOURCE = "resource";
	private static final String CDATA = "CDATA";

	/**
 * 
 * Exempel på tagg som har flera "second parts":
 *		
 *		<soch:toPeriodId>Vikingatid</soch:toPeriodId>
 *		<soch:periodAuth>http://kulturarvsdata.se/resurser/aukt/srdb/period#V</soch:periodAuth>
 *		<soch:fromPeriodId>Vikingatid</soch:fromPeriodId>
 * 
 * Ovanstånde ska slås ihop till två separata taggar:
 * 
 * 		<soch:toPeriod>http://kulturarvsdata.se/resurser/aukt/srdb/period#V/Vikingatid</soch:toPeriod>
 *		<soch:fromPeriod>http://kulturarvsdata.se/resurser/aukt/srdb/period#V/Vikingatid</soch:fromPeriodId>
 * 
 * 
 */

	private class DeprecatedTag {
		String uri;
		String name;
		Attributes attributes;
		String content;
		
		public String getUri() {
			return uri;
		}
		public void setUri(String uri) {
			this.uri = uri;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Attributes getAttributes() {
			return attributes;
		}
		public void setAttributes(Attributes attributes) {
			this.attributes = attributes;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
	}
	
	//private HashMap<String, DeprecatedTag> deprecatedTags = new HashMap<>();

	private HashMap<Integer, HashMap<String, DeprecatedTag>> deprecatedTagsByLevel = new HashMap<>();


	// antal databasoperationer innan en commit görs
	private static final int XACT_LIMIT = 1000;

	// en generisk iso 8601-parser som klarar "alla" isoformat - egentligen ska vi bara stödja två
	// enl spec
	private final static DateTimeFormatter isoDateTimeParser = ISODateTimeFormat.dateTimeParser();

	Connection c;
	HarvestService service;
	ContentHelper contentHelper;
	String oaiURI;
	Timestamp datestamp;
	int mode = 0;
	private int level = 0;
	private boolean deleteRecord;
	private final StringBuffer buf = new StringBuffer();
	private final HashMap<String, String> prefixMap = new HashMap<>();
	private static final int NORMAL = 0;
	private static final int RECORD = 1;
	private static final int COPY = 2;

	private static final XMLOutputFactory xxmlf = XMLOutputFactory.newInstance();
	private XMLStreamWriter xxmlw;
	private StringWriter xsw;
	// antal genomförda databasoperationer, totalt och i nuvarande transaktion
	private int numDeleted = 0, numDeletedXact = 0;
	private int numInserted = 0, numInsertedXact = 0;
	private int numUpdated = 0, numUpdatedXact = 0;
	private final ServiceMetadata sm;
	private String errorCode;
	private final Timestamp ts;
	private final StatusService ss;
	private PreparedStatement oai2uriPst;
	private PreparedStatement updatePst;
	private PreparedStatement deleteUpdatePst;
	private PreparedStatement insertPst;

	private static final Logger logger = LogManager.getLogger(OAIPMHHandler.class);


	public OAIPMHHandler(StatusService ss, HarvestService service, ContentHelper contentHelper, ServiceMetadata sm,
		Connection c, Timestamp ts) throws Exception {
		this.ss = ss;
		this.service = service;
		this.contentHelper = contentHelper;
		this.c = c;
		this.sm = sm;
		// förbered några databas-statements som kommer användas frekvent
		this.ts = new Timestamp(ts.getTime());
		this.oai2uriPst = c.prepareStatement("select uri from content where oaiuri = ?");
		this.updatePst = c.prepareStatement("update content set deleted = null, oaiuri = ?, " +
			"serviceId = ?, changed = ?, datestamp = ?, xmldata = ?, status = ?, nativeURL = ? where uri = ?");
		this.deleteUpdatePst = c.prepareStatement("update content set status = ?, " +
			"changed = ?, deleted = ?, datestamp = ?, xmldata = null where serviceId = ? and oaiuri = ?");
		this.insertPst = c.prepareStatement("insert into content " +
			"(uri, oaiuri, serviceId, xmldata, changed, added, datestamp, status, nativeURL) " +
			"values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
	}

	public void destroy() {
		DBUtil.closeDBResources(null, oai2uriPst, null);
		DBUtil.closeDBResources(null, updatePst, null);
		DBUtil.closeDBResources(null, deleteUpdatePst, null);
		DBUtil.closeDBResources(null, insertPst, null);
		oai2uriPst = null;
		updatePst = null;
		deleteUpdatePst = null;
		insertPst = null;
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (mode == COPY) {
			uri = correctFaultyUris(uri);
			prefixMap.put(prefix, uri);
			try {
				xxmlw.setPrefix(prefix, uri);
			} catch (Exception e) {
				throw new SAXException(e);
			}

		}
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		
		// ny nivå i xml:en, håll reda på level
		level++;

		switch (mode) {
			case NORMAL:
				if ("record".equals(name)) {
					// byt till "record-mode"
					mode = RECORD;
					deleteRecord = false;
					datestamp = null;
					oaiURI = null;
				} else if ("error".equals(name)) {
					errorCode = attributes.getValue("", "code");
				}
				break;
			case RECORD:
				if ("metadata".equals(name)) {
					// byt till "copy-mode"
					mode = COPY;
					try {
						xsw = new StringWriter();
						xxmlw = xxmlf.createXMLStreamWriter(xsw);
						// inget start doc, vi vill ha xml-fragment
						// xxmlw.writeStartDocument("UTF-8", "1.0");
					}  catch (Exception e) {
						String errMsg = "Error when creating XMLStreamWriter on uri: " + uri + ", localName: " + localName + ", name: " + name;
						throw new SAXException(errMsg, e);
					}
				} else if ("header".equals(name)) {
					// kontrollera om status säger deleted, då ska denna post bort
					String status = attributes.getValue("", "status");
					if ("deleted".equals(status)) {
						if (!sm.canSendDeletes()) {
							String errMsg = "Service is not supposed to handle deletes but did in fact send one! on uri: " + uri + ", localName: " + localName + ", name: " + name;
							throw new SAXException(errMsg);
						}
						deleteRecord = true;
					}
				}
				break;
			case COPY:
				// i "copy-mode" kopiera hela taggen som den är


				// correct faulty uri:s from local nodes
				uri = correctFaultyUris(uri);

					
				if (PERIOD_AUTH.equals(localName) ||
						TO_PERIOD_ID.equals(localName) ||
						FROM_PERIOD_ID.equals(localName) ||
						NAME_AUTH.equals(localName) ||
						NAME_ID.equals(localName)) {
					
					// Deprecated URI:er ska skrivas om till nya
					// Spara undan informationen på rätt "nivå" i xml:en
					
					// fetch or create deprecatedValues 
					HashMap<String, DeprecatedTag> deprecatedTags = deprecatedTagsByLevel.get(level);
					if (deprecatedTags == null) {
						deprecatedTags = new HashMap<>();
						deprecatedTagsByLevel.put(level, deprecatedTags);
					}

					DeprecatedTag deprecatedTag = new DeprecatedTag();
					deprecatedTag.setUri(uri);
					deprecatedTag.setName(name);
					deprecatedTag.setAttributes(attributes);

					// spara informationen med localName som nyckel
					deprecatedTags.put(localName, deprecatedTag);

					// de här ska vi inte skriva ut här, kolla i endElement vad vi ska göra med den, om något
					return;
				}

				_writeStartElement(uri, localName, name, attributes);
				break;
		}
	}

	/**
	 * 
	 * @param uri
	 * @param localName
	 * @param name Används bara för felutskrifter - behöver vi ha med det?
	 * @param attributes
	 * @throws SAXException
	 */
	private void _writeStartElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
		try {
			xxmlw.writeStartElement(uri, localName);
		}  catch (Exception e) {
			String errMsg = "Error when writing start element on uri: " + uri + ", localName: " + localName + ", name: " + name;
			logger.error(errMsg);
			throw new SAXException(errMsg, e);
		}
		
		if (prefixMap.size() > 0) {
			for (String prefix : prefixMap.keySet()) {
				try {
					xxmlw.writeNamespace(prefix, prefixMap.get(prefix));
				}  catch (Exception e) {
					String errMsg = "Error when writing namespace on uri: " + uri + ", localName: " + localName + ", name: " + name;
					throw new SAXException(errMsg, e);
				}
			}
		}
		prefixMap.clear();
		try {
			if (attributes != null && attributes.getLength() > 0) {
				for (int i = 0; i < attributes.getLength(); ++i) {
					String aUri = attributes.getURI(i);
					String lName = attributes.getLocalName(i);
					String value = attributes.getValue(i);
					// Om ej rätt metod används blir det ett exception
					if (aUri == null || "".equals(aUri)) {
						xxmlw.writeAttribute(lName, value);
					} else {
						xxmlw.writeAttribute(aUri, lName, value);
					}
				}
			}
		} catch (Exception e) {
			String errMsg = "Error when extracting attributes on uri: " + uri + ", localName: " + localName + ", name: " + name;
			throw new SAXException(errMsg, e);
		}
	}

	private String correctFaultyUris(String uri) {
		// correct the occasional "aut" into "aukt"
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aut",
			"http://kulturarvsdata.se/resurser/aukt");

		// correcty faulty geography uris
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/continent/continent",
			"http://kulturarvsdata.se/resurser/aukt/geo/continent");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/country/country",
			"http://kulturarvsdata.se/resurser/aukt/geo/country");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/county/county",
			"http://kulturarvsdata.se/resurser/aukt/geo/county");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/municipality/municipality",
			"http://kulturarvsdata.se/resurser/aukt/geo/municipality");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/parish/parish",
			"http://kulturarvsdata.se/resurser/aukt/geo/parish");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/aukt/geo/province/province",
			"http://kulturarvsdata.se/resurser/aukt/geo/province");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/contextsupertype/contextsupertype",
			"http://kulturarvsdata.se/resurser/contextsupertype");

		// båda dessa nedan förekommer och måste rättas
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/contexttyp/contexttype",
			"http://kulturarvsdata.se/resurser/contexttype");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/contexttype/contexttype",
			"http://kulturarvsdata.se/resurser/contexttype");

		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/dataquality/dataquality",
			"http://kulturarvsdata.se/resurser/dataquality");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/entitysupertype/entitysupertype",
			"http://kulturarvsdata.se/resurser/entitysupertype");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/entitytype/entitytype",
			"http://kulturarvsdata.se/resurser/entitytype");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/license/license",
			"http://kulturarvsdata.se/resurser/license");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/subject/subject",
			"http://kulturarvsdata.se/resurser/subject");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/theme/theme",
			"http://kulturarvsdata.se/resurser/theme");
		uri = StringUtils.replace(uri, "http://kulturarvsdata.se/resurser/title/title",
			"http://kulturarvsdata.se/resurser/title");



		// correct other uris
		uri = SamsokUriPrefix.lookupPrefix(uri);
		return uri;
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		// TODO: if uri is ever used in this method, it needs to be run through
		// correctFaultyUris,
		// but it's unnecessary as long as this method doesn't do anything with the
		// uri:s
		switch (mode) {
			case COPY:
				if ("metadata".equals(name)) {
					// tillbaks till "record-mode"
					mode = RECORD;
					try {
						// spara i databas
						// xxmlw.writeEndDocument(); // inget end doc, vi vill ha xml-fragment
						xxmlw.close();
						xxmlw = null;
						insertOrUpdateRecord(oaiURI, xsw.toString(), datestamp);
						xsw.close();
						xsw = null;
					} catch (Exception e) {
						throw new SAXException(e);
					}
				} else {
					// Kolla om URI:ns localName innehåller någon av de deprekerade som ska skrivas om till nya

					if (PERIOD_AUTH.equals(localName) ||
							TO_PERIOD_ID.equals(localName) ||
							FROM_PERIOD_ID.equals(localName) ||
							NAME_AUTH.equals(localName) ||
							NAME_ID.equals(localName)) {

						// Deprecated URI:er ska skrivas om till nya

						// fetch or create deprecatedValues
						HashMap<String, DeprecatedTag> deprecatedTags = deprecatedTagsByLevel.get(level);
						if (deprecatedTags == null) {
							deprecatedTags = new HashMap<>();
							deprecatedTagsByLevel.put(level, deprecatedTags);
						}

						DeprecatedTag deprecatedTag = deprecatedTags.get(localName);

						// här borde det finnas en, annars är något knasigt
						if (deprecatedTag == null) {
							logger.warn("Hittar ingen deprecatedTag trots att det borde finnas för localName "
									+ localName + ", uri " + uri + ", level " + level);
							throw new SAXException("Missing expected deprecatedTag for " + localName);
						}

						// läs ut värdet på buf (här finns strängen som står mellan taggarna; content)
						String content = buf.toString().trim();

						// skriv content i deprecatedTag, 
						// ifall vi behöver det nästa varv
						deprecatedTag.setContent(content);

						// Hämta alla deprecated tags som vi sett på den här nivån
						DeprecatedTag periodAuthTag = deprecatedTags.get(PERIOD_AUTH);
						DeprecatedTag fromPeriodIdTag = deprecatedTags.get(FROM_PERIOD_ID);
						DeprecatedTag toPeriodIdTag = deprecatedTags.get(TO_PERIOD_ID);
						DeprecatedTag nameAuthTag = deprecatedTags.get(NAME_AUTH);
						DeprecatedTag nameIdTag = deprecatedTags.get(NAME_ID);

						String contentToUse = null;
						String idContent = null;
						String uriToUse = null;
						String localNameToUse = null;
						String nameToUse = null;
						Attributes attributesToUse = null;
						String authContent = null;

						// Räkna ut vilka värden vi ska använda
						if (periodAuthTag != null) {
							uriToUse = periodAuthTag.getUri();
							attributesToUse = periodAuthTag.getAttributes();
							authContent = periodAuthTag.getContent();
							nameToUse = periodAuthTag.getName();

							if (toPeriodIdTag != null) {

								// Skapa en "http://kulturarvsdata.se/ksamsok#toPeriod"-tagg istället för periodAuth+toPeriodId-taggarna
								localNameToUse = TO_PERIOD;
								idContent = toPeriodIdTag.getContent();

								// vi måste ta bort den här från mappen så vi inte använder den igen nästa iteration
								deprecatedTags.remove(TO_PERIOD_ID);
							} else if (fromPeriodIdTag != null) {
								// Skapa en "http://kulturarvsdata.se/ksamsok#fromPeriod"-tagg istället för periodAuth+fromPeriod-taggarna
								localNameToUse = FROM_PERIOD;
								idContent = fromPeriodIdTag.getContent();
								
								// vi måste ta bort den här från mappen så vi inte använder den igen nästa iteration
								deprecatedTags.remove(FROM_PERIOD_ID);
							}
						} else if (nameAuthTag != null) {
							uriToUse = nameAuthTag.getUri();
							attributesToUse = nameAuthTag.getAttributes();
							authContent = nameAuthTag.getContent();
							nameToUse = nameAuthTag.getName();
							if (nameIdTag != null) {
								// Skapa en "http://kulturarvsdata.se/ksamsok#agent"-tagg istället för nameAuth+nameId-taggarna
								localNameToUse = AGENT;
								idContent = nameIdTag.getContent();

								// vi måste ta bort den här från mappen så vi inte använder den igen nästa iteration
								deprecatedTags.remove(NAME_ID);
							}
						}
						if (localNameToUse != null && authContent != null && idContent != null) {
							// vi har ett par med både auth och id, slå ihop!
							if (authContent
									.startsWith(KULTURARVSDATA_PERIOD_AUTH_URI)) {
								// de här ska behållas som de är, men taggarna ska bytas ut mot
								// "toPeriod/fromPeriod"
								contentToUse = authContent;
							} else if (authContent.startsWith(MIS_AUTH_URI)) {
								// slå ihop med id-content
								contentToUse = fixContent(authContent, HASHTAG, idContent);
							} else if (authContent.startsWith(LIBRIS_AUTH_URI)) {
								// slå ihop med id-content
								contentToUse = fixContent(authContent, SLASH, idContent);
							} else if (authContent.equals(KUNGLIGA_BIBLIOTEKET)) {
								// Byt ut mot libris och slå ihop med id-content
								contentToUse = fixContent(LIBRIS_AUTH_URI, SLASH, idContent);
							} else if (authContent.equals(VIAF)) {
								// Byt ut mot viaf-url och slå ihop med id-content
								contentToUse = fixContent(VIAF_AUTH_URI, SLASH, idContent);
							}
						}
						if (contentToUse != null) {
							// Börja med att starta taggen

							// innehållet ska sättas som ett rdf:resource-attribut på taggen

							// Kopiera attributesToUse till en AttributesImpl så vi kan modifiera dem
							AttributesImpl modifiedAttributes = new AttributesImpl(attributesToUse);
							modifiedAttributes.addAttribute(RDF_NS_URI, RESOURCE, "", CDATA, contentToUse);

							_writeStartElement(uriToUse, localNameToUse,
									nameToUse, modifiedAttributes);

							// stäng taggen
							try {
								xxmlw.writeEndElement();
							} catch (Exception e) {
								throw new SAXException(e);
							}
						}

					} else {

						// det här är en vanlig tag, kopiera som den är
						try {
							xxmlw.writeCharacters(buf.toString().trim());
							xxmlw.writeEndElement();
						} catch (Exception e) {
							throw new SAXException(e);
						}
					}
				}
				// återställ char-buff
				buf.setLength(0);
				break;
			case RECORD:
				if ("identifier".equals(name)) {
					// läs ut värde för identifier
					oaiURI = buf.toString().trim();
				} else if ("datestamp".equals(name)) {
					// läs ut och tolka värde för datum
					String datestampStr = buf.toString().trim();
					datestamp = parseDatestamp(datestampStr);
					if (datestamp == null) {
						datestamp = new Timestamp((ts.getTime()));
						ContentHelper.addProblemMessage("There was a problem parsing datestamp (" + datestampStr +
								") for record, using 'now' instead");
					}
				} else if ("record".equals(name)) {
					// tillbaks till "normal-mode"
					mode = NORMAL;
					if (deleteRecord) {
						// ta bort post nu om vi skulle göra det
						try {
							deleteRecord(oaiURI, datestamp);
						} catch (Exception e) {
							throw new SAXException(e);
						}
					}
				}
				// återställ char-buff
				buf.setLength(0);
				break;
			case NORMAL:
				if ("error".equals(name)) {
					throw new SAXException("Error in request, code=" + errorCode + ", text: " + buf.toString().trim());
				}
				// återställ char-buff
				buf.setLength(0);
				break;
			default:
				logger.warn("Unexpected mode " + mode + " found in endElement for uri + " + uri);
		}
		// vi flyttar upp en nivå, ta bort alla hittade deprecatedValues nivån under
		deprecatedTagsByLevel.remove(level + 1);
		level--;
	}

	public static String fixContent(String authContent, String delimiter, String idContent) {
		String contentToUse;
		contentToUse = authContent;

		// se till att det finns ett "#"
		if (!contentToUse.endsWith(delimiter)) {
			contentToUse += delimiter;
		}

		// och lägg till content från periodId-taggen
		contentToUse += idContent;
		return contentToUse;
	}

	

	@Override
	public void characters(char[] ch, int start, int length) {
		buf.append(ch, start, length);
	}

	/**
	 * Tar bort alla poster i repositoryt för tjänsten vi jobbar med. I praktiken tas inget bort
	 * utan status-kolumnen sätts till 1 för att senare eventuellt ge att deleted-kolumnen sätts
	 * icke null.
	 * 
	 * @throws Exception
	 */
	protected void deleteAllFromThisService() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Attempting to remove all records for service id: " + service.getId());
		}
		PreparedStatement pst = null;
		try {
			// OBS notera att geometrier inte tas bort här utan det görs inkrementellt
			// för varje post som dyker upp, eller i slutsteget för de som ej har behandlats
			pst = c.prepareStatement("update content set status = ? where serviceId = ?");
			pst.setInt(1, DBUtil.STATUS_PENDING);
			pst.setString(2, service.getId());
			long start = System.currentTimeMillis();
			int num = pst.executeUpdate();
			numDeletedXact += num;
			commitIfLimitReached(true);
			if (logger.isDebugEnabled()) {
				logger.debug("** Removed (updated status to pending) " + num + " records for service: " +
					service.getId() + " in " + ContentHelper.formatRunTime(System.currentTimeMillis() - start));
			}
		} finally {
			DBUtil.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Tar bort post i repositoryt med inskickad OAI-identifierare (Obs != rdf-identifierare)
	 * 
	 * @param oaiURI OAI-identifierare
	 * @throws Exception
	 */
	protected void deleteRecord(String oaiURI, Timestamp deletedAt) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Removing oaiURI=" + oaiURI + " from service with ID: " + service.getId());
		}

		// OBS att antalet parametrar etc *måste* stämma med det statement som används
		// och som skapas och förbereds i konstruktorn!


		// update content set status = ?, changed = ?, deleted = ?, datestamp = ? where
		// serviceId = ? and oaiuri = ?
		deleteUpdatePst.setInt(1, DBUtil.STATUS_NORMAL);
		deleteUpdatePst.setTimestamp(2, ts);
		deleteUpdatePst.setTimestamp(3, deletedAt);
		deleteUpdatePst.setTimestamp(4, deletedAt);
		deleteUpdatePst.setString(5, service.getId());
		deleteUpdatePst.setString(6, oaiURI);
		int num = deleteUpdatePst.executeUpdate();
		numDeletedXact += num;
		if (logger.isDebugEnabled()) {
			logger.debug(
					"* Removed " + num + " number of oaiURI=" + oaiURI + " from service with ID: " + service.getId());
		}
		commitIfLimitReached();
	}

	/**
	 * Stoppar in en ny post i repositoryt med angiven OAI-identifierare, identifierare och
	 * xml-innehåll.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-innehåll
	 * @param datestamp postens ändringsdatum (från oai-huvudet)
	 * @param nativeURL url till html-representation, eller null
	 * @throws Exception
	 */
	protected void insertRecord(String oaiURI, String uri, String xmlContent, Timestamp datestamp, String nativeURL) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(
				"* Entering data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " + service.getId());
		}

		// OBS att antalet parametrar etc *måste* stämma med det statement som används
		// och som skapas och förbereds i konstruktorn!

		// insert into content
		// (uri, oaiuri, serviceId, xmldata, changed, added, datestamp, status, nativeURL)
		// values (?, ?, ?, ?, ?, ?, ?, ?, ?)
		insertPst.setString(1, uri);
		insertPst.setString(2, oaiURI);
		insertPst.setString(3, service.getId());
		insertPst.setCharacterStream(4, new StringReader(xmlContent), xmlContent.length());
		insertPst.setTimestamp(5, ts);
		insertPst.setTimestamp(6, ts);
		insertPst.setTimestamp(7, datestamp);
		insertPst.setInt(8, DBUtil.STATUS_NORMAL);
		insertPst.setString(9, nativeURL);
		insertPst.executeUpdate();

		++numInsertedXact;
		if (logger.isDebugEnabled()) {
			logger.debug(
				"* Entered data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " + service.getId());
		}
		commitIfLimitReached();
	}

	/**
	 * Uppdaterar en post i repositoryt.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-innehåll
	 * @param datestamp postens ändringsdatum (från oai-huvudet)
	 * @param nativeURL url till html-representation, eller null
	 * @throws Exception
	 */
	protected boolean updateRecord(String oaiURI, String uri, String xmlContent, Timestamp datestamp, String nativeURL) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(
				"* Updated data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " + service.getId());
		}

		// OBS att antalet parametrar etc *måste* stämma med det statement som används
		// och som skapas och förbereds i konstruktorn!

		// update content set oaiuri = ?, deleted = null,
		// serviceId = ?, changed = ?, datestamp = ?, xmldata = ?, status = ?, nativeURL = ? where
		// uri = ?
		updatePst.setString(1, oaiURI);
		updatePst.setString(2, service.getId());
		updatePst.setTimestamp(3, ts);
		updatePst.setTimestamp(4, datestamp);
		updatePst.setCharacterStream(5, new StringReader(xmlContent), xmlContent.length());
		updatePst.setInt(6, DBUtil.STATUS_NORMAL);
		updatePst.setString(7, nativeURL);
		updatePst.setString(8, uri);
		boolean updated = updatePst.executeUpdate() > 0;
		if (updated) {
			++numUpdatedXact;
			if (logger.isDebugEnabled()) {
				logger.debug("* Updated data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " +
					service.getId());
			}
		}
		commitIfLimitReached();
		return updated;
	}

	/**
	 * Uppdaterar en befintlig eller stoppar in en ny post i repositoryt beroende på om posten
	 * finns och om tjänsten klarar av att skicka persistent deletes.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param xmlContent xml-innehåll
	 * @param datestamp postens ändringsdatum (från oai-huvudet)
	 * @throws Exception
	 */
	protected void insertOrUpdateRecord(String oaiURI, String xmlContent, Timestamp datestamp) throws Exception {
		String uri;
		String nativeURL;

		try {
			ExtractedInfo info = contentHelper.extractInfo(xmlContent);
			uri = info.getIdentifier();
			nativeURL = info.getNativeURL();
		} catch (Exception e) {
			ContentHelper.addProblemMessage(
				"Problem parsing rdf and/or extracting info for record " + oaiURI + " --SKIPPING--");
			ss.signalRDFError(service);
			return;
		}
		// bör/ska inte hända, men...
		if (uri == null) {
			ContentHelper.addProblemMessage("No uri found for " + oaiURI + " --SKIPPING--");
			return;
		}

		// gör update och om ingen post uppdaterades stoppa in en (istf för att kolla om post
		// finns först)
		if (!updateRecord(oaiURI, uri, xmlContent, datestamp, nativeURL)) {
			insertRecord(oaiURI, uri, xmlContent, datestamp, nativeURL);
		}
	}

	// gör commit och räkna up antalet lyckade åtgärder
	private void commitIfLimitReached() throws Exception {
		commitIfLimitReached(false);
	}

	// gör commit och räkna up antalet lyckade åtgärder, gör alltid commit om argumentet är
	// true
	private void commitIfLimitReached(boolean forceCommit) throws Exception {
		int count = numInsertedXact + numUpdatedXact + numDeletedXact;
		if (forceCommit || count >= XACT_LIMIT) {
			commitAndUpdateCounters();
		}
	}

	/**
	 * Uppdaterar obehandlade poster baserat på status och tjänst. Sätter deleted och
	 * nollställer status.
	 * 
	 * @return antal databasförändringar
	 * @throws Exception
	 */
	protected int updateTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Update status for service with ID: " + service.getId());
		}
		final int BATCH_SIZE = 500;
		ss.setStatusText(service, "Attempting to update status and deleted column for pending records");
		int updated;
		PreparedStatement updatePst = null;
		PreparedStatement selPst = null;
		ResultSet rs = null;
		try {
			// hämta kvarvarande poster under behandling och sätt deras deleted
			// och ta bort deras geometrier i batchar
			String sql = DBUtil.fetchFirst(c, "select uri from content where serviceid = ? and status <> ?",
				BATCH_SIZE);
			selPst = c.prepareStatement(sql);
			selPst.setString(1, service.getId());
			selPst.setInt(2, DBUtil.STATUS_NORMAL);

			// behåll deleted om värdet finns, även för datestamp tas värdet från deleted
			updatePst = c.prepareStatement("update content set changed = ?, deleted = coalesce(deleted, ?), " +
				"datestamp = coalesce(deleted, ?), status = ?, xmldata = null where uri = ?");
			updatePst.setTimestamp(1, ts);
			updatePst.setTimestamp(2, ts);
			updatePst.setTimestamp(3, ts);
			updatePst.setInt(4, DBUtil.STATUS_NORMAL);
			int totalRec = 0;
			int totalGeo = 0;
			long start = System.currentTimeMillis();
			String uri;
			int deltaRec = BATCH_SIZE;
			// när vi får mindre än (och har behandlat dem) batch-storleken har vi nått slutet
			while (deltaRec == BATCH_SIZE) {
				deltaRec = 0;
				// kolla om vi ska avbryta
				ss.checkInterrupt(service);
				ss.setStatusText(service, "Have commited " + totalRec + " (plus " + totalGeo +
					" geo deletes) status and deleted column updates");
				rs = selPst.executeQuery();
				while (rs.next()) {
					uri = rs.getString("uri");
					updatePst.setString(5, uri);
					deltaRec += updatePst.executeUpdate();
				}
				// stäng (och nollställ) rs för återanvändning
				DBUtil.closeDBResources(rs, null, null);
				rs = null;
				DBUtil.commit(c);
				totalRec += deltaRec;
			}
			updated = totalRec + totalGeo;
			ss.setStatusTextAndLog(service,
				"Committed status and deleted column updates for " + totalRec + " records (plus " + totalGeo +
					" geo deletes) in " + ContentHelper.formatRunTime(System.currentTimeMillis() - start));
			if (logger.isDebugEnabled()) {
				logger.debug("Updated status och deleted column for " + totalRec + " records in " +
					ContentHelper.formatRunTime(System.currentTimeMillis() - start) + " for service " +
					service.getId());
			}
		} finally {
			DBUtil.closeDBResources(rs, selPst, null);
			DBUtil.closeDBResources(null, updatePst, null);
		}
		return updated;
	}

	/**
	 * Återställer status-kolumnen och därmed status till ursprungligt så långt det går.
	 * 
	 * @throws Exception vid fel
	 */
	protected int resetTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Attempting to reset status for pending records for service " + service.getId());
		}
		ss.setStatusText(service, "Recovery: Attempting to reset status for pending records");
		int numAffected;
		PreparedStatement pst = null;
		try {
			pst = c.prepareStatement("update content set status = ? where serviceId = ? and status <> ?");
			pst.setInt(1, DBUtil.STATUS_NORMAL);
			pst.setString(2, service.getId());
			pst.setInt(3, DBUtil.STATUS_NORMAL);
			long start = System.currentTimeMillis();
			numAffected = pst.executeUpdate();
			if (logger.isDebugEnabled()) {
				logger.debug("** Updated state for " + numAffected + " records for service: " + service.getId());
			}
			commitIfLimitReached(true);
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Recovery: The status for " + numAffected +
				" pending records was reset in " + ContentHelper.formatRunTime(durationMillis));
			if (logger.isDebugEnabled()) {
				logger.debug("Reset status for " + numAffected + " records in " +
					ContentHelper.formatRunTime(durationMillis) + " for service " + service.getId());
			}
		} finally {
			DBUtil.closeDBResources(null, pst, null);
		}
		return numAffected;
	}

	/**
	 * Gör utestående commit och beräknar respektive återställer antal uppdaterade total och
	 * för denna transaktion.
	 * 
	 * @throws Exception
	 */
	public void commitAndUpdateCounters() throws Exception {
		// kolla om vi ska avbryta, kastar exception
		ss.checkInterrupt(service);

		c.commit();
		numInserted += numInsertedXact;
		numUpdated += numUpdatedXact;
		numDeleted += numDeletedXact;
		numInsertedXact = 0;
		numUpdatedXact = 0;
		numDeletedXact = 0;

		String msg = "Committed (i/u/d " + numInserted + SLASH + numUpdated + SLASH + numDeleted + ") " +
			(numInserted + numUpdated + numDeleted) + " database changes";
		ss.setStatusText(service, msg);
		if (logger.isDebugEnabled()) {
			logger.debug(msg);
		}
	}

	/**
	 * Hämtar antal poster som stoppades in.
	 * 
	 * @return antal "nya" poster
	 */
	public int getInserted() {
		return numInserted;
	}

	/**
	 * Hämtar antalet borttagna poster.
	 * 
	 * @return antal borttagna poster
	 */
	public int getDeleted() {
		return numDeleted;
	}

	/**
	 * Hämtar antalet ändrade poster.
	 * 
	 * @return antal ändrade poster
	 */
	public int getUpdated() {
		return numUpdated;
	}

	/**
	 * Tolkar värdet som ett iso8601-datum (med ev tid).
	 * 
	 * @param value sträng att tolka
	 * @return tolkad timestamp eller null om värdet inte gick att tolka
	 */
	private Timestamp parseDatestamp(String value) {
		DateTime dateTime = null;
		try {
			dateTime = isoDateTimeParser.parseDateTime(value);
		} catch (Throwable t) {
			if (logger.isDebugEnabled()) {
				logger.debug("There was a problem parsing string '" + value + "' as an iso8601 date", t);
			}
		}
		return (dateTime != null ? new Timestamp(dateTime.getMillis()) : null);

	}

}
