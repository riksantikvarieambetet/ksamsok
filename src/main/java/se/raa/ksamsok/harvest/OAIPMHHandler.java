package se.raa.ksamsok.harvest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokUriPrefix;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Handler fÃ¶r xml-parsning som lagrar poster i repositoryt och gÃ¶r commit med jÃ¤mna mellanrum
 * (fÃ¶r att inte oracle ska fÃ¥ spunk, derby klarar det). Formatet pÃ¥ xml:en ska vara samma som
 * fÃ¶r RawWrite, dvs i princip OAI-PMH med en omslutande tagg.
 */
public class OAIPMHHandler extends DefaultHandler {

	// antal databasoperationer innan en commit gÃ¶rs
	private static final int XACT_LIMIT = 1000;

	// en generisk iso 8601-parser som klarar "alla" isoformat - egentligen ska vi bara stÃ¶dja tvÃ¥
	// enl spec
	private static DateTimeFormatter isoDateTimeParser = ISODateTimeFormat.dateTimeParser();

	Connection c;
	HarvestService service;
	ContentHelper contentHelper;
	String oaiURI;
	Timestamp datestamp;
	int mode = 0;
	private boolean deleteRecord;
	private StringBuffer buf = new StringBuffer();
	private HashMap<String, String> prefixMap = new HashMap<String, String>();
	private static final int NORMAL = 0;
	private static final int RECORD = 1;
	private static final int COPY = 2;

	private static final XMLOutputFactory xxmlf = XMLOutputFactory.newInstance();
	private XMLStreamWriter xxmlw;
	private StringWriter xsw;
	// antal genomfÃ¶rda databasoperationer, totalt och i nuvarande transaktion
	private int numDeleted = 0, numDeletedXact = 0;
	private int numInserted = 0, numInsertedXact = 0;
	private int numUpdated = 0, numUpdatedXact = 0;
	private ServiceMetadata sm;
	private String errorCode;
	private Timestamp ts;
	private StatusService ss;
	private PreparedStatement oai2uriPst;
	private PreparedStatement updatePst;
	private PreparedStatement deleteUpdatePst;
	private PreparedStatement insertPst;

	private static final Logger logger = LogManager.getLogger("se.raa.ksamsok.harvest.OAIPMHHandler");


	public OAIPMHHandler(StatusService ss, HarvestService service, ContentHelper contentHelper, ServiceMetadata sm,
		Connection c, Timestamp ts) throws Exception {
		this.ss = ss;
		this.service = service;
		this.contentHelper = contentHelper;
		this.c = c;
		this.sm = sm;
		this.ts = ts;
		// fÃ¶rbered nÃ¥gra databas-statements som kommer anvÃ¤ndas frekvent
		this.oai2uriPst = c.prepareStatement("select uri from content where oaiuri = ?");
		this.updatePst = c.prepareStatement("update content set deleted = null, oaiuri = ?, " +
			"serviceId = ?, changed = ?, datestamp = ?, xmldata = ?, status = ?, nativeURL = ? where uri = ?");
		// TODO: stoppa in xmldata = null nedan fÃ¶r att rensa onÃ¶digt gammalt postinnehÃ¥ll?
		this.deleteUpdatePst = c.prepareStatement("update content set status = ?, " +
			"changed = ?, deleted = ?, datestamp = ? where serviceId = ? and oaiuri = ?");
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
					} catch (Exception e) {
						throw new SAXException(e);
					}
				} else if ("header".equals(name)) {
					// kontrollera om status sÃ¤ger deleted, dÃ¥ ska denna post bort
					String status = attributes.getValue("", "status");
					if ("deleted".equals(status)) {
						if (!sm.canSendDeletes()) {
							throw new SAXException(
								"Service is not supposed to handle deletes but did in fact send one!");
						}
						deleteRecord = true;
					}
				}
				break;
			case COPY:
				// i "copy-mode" kopiera hela taggen som den Ã¤r
				// correct faulty uri:s from local nodes
				uri = correctFaultyUris(uri);
				try {
					xxmlw.writeStartElement(uri, localName);
				} catch (Exception e) {
					throw new SAXException(e);
				}
				if (prefixMap.size() > 0) {
					for (String prefix : prefixMap.keySet()) {
						try {
							xxmlw.writeNamespace(prefix, prefixMap.get(prefix));
						} catch (Exception e) {
							throw new SAXException(e);
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
							// Om ej rÃ¤tt metod anvÃ¤nds blir det ett exception
							if (aUri == null || "".equals(aUri)) {
								xxmlw.writeAttribute(lName, value);
							} else {
								xxmlw.writeAttribute(aUri, lName, value);
							}
						}
					}
				} catch (Exception e) {
					throw new SAXException(e);
				}
				break;
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
		// TODO: if uri is ever used in this method, it needs to be run through correctFaultyUris,
		// but it's unnecessary as long as this method doesn't do anything with the uri:s
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
					// kopiera
					try {
						xxmlw.writeCharacters(buf.toString().trim());
						xxmlw.writeEndElement();
					} catch (Exception e) {
						throw new SAXException(e);
					}
				}
				// Ã¥terstÃ¤ll char-buff
				buf.setLength(0);
				break;
			case RECORD:
				if ("identifier".equals(name)) {
					// lÃ¤s ut vÃ¤rde fÃ¶r identifier
					oaiURI = buf.toString().trim();
				} else if ("datestamp".equals(name)) {
					// lÃ¤s ut och tolka vÃ¤rde fÃ¶r datum
					String datestampStr = buf.toString().trim();
					datestamp = parseDatestamp(datestampStr);
					if (datestamp == null) {
						datestamp = ts;
						ContentHelper.addProblemMessage("There was a problem parsing datestamp (" + datestampStr +
							") for record, using 'now' instead");
					}
				} else if ("record".equals(name)) {
					// tillbaks till "normal-mode"
					mode = NORMAL;
					if (deleteRecord) {
						// ta bort post nu om vi skulle gÃ¶ra det
						try {
							deleteRecord(oaiURI, datestamp);
						} catch (Exception e) {
							throw new SAXException(e);
						}
					}
				}
				// Ã¥terstÃ¤ll char-buff
				buf.setLength(0);
				break;
			case NORMAL:
				if ("error".equals(name)) {
					throw new SAXException("Error in request, code=" + errorCode + ", text: " + buf.toString().trim());
				}
				// Ã¥terstÃ¤ll char-buff
				buf.setLength(0);
				break;
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		buf.append(ch, start, length);
	}

	/**
	 * Tar bort alla poster i repositoryt fÃ¶r tjÃ¤nsten vi jobbar med. I praktiken tas inget bort
	 * utan status-kolumnen sÃ¤tts till 1 fÃ¶r att senare eventuellt ge att deleted-kolumnen sÃ¤tts
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
			// OBS notera att geometrier inte tas bort hÃ¤r utan det gÃ¶rs inkrementellt
			// fÃ¶r varje post som dyker upp, eller i slutsteget fÃ¶r de som ej har behandlats
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

		// OBS att antalet parametrar etc *mÃ¥ste* stÃ¤mma med det statement som anvÃ¤nds
		// och som skapas och fÃ¶rbereds i konstruktorn!

		ResultSet rs = null;
		try {

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
		} finally {
			DBUtil.closeDBResources(rs, null, null);
		}
	}

	/**
	 * Stoppar in en ny post i repositoryt med angiven OAI-identifierare, identifierare och
	 * xml-innehÃ¥ll.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-innehÃ¥ll
	 * @param datestamp postens Ã¤ndringsdatum (frÃ¥n oai-huvudet)
	 * @param nativeURL url till html-representation, eller null
	 * @throws Exception
	 */
	protected void insertRecord(String oaiURI, String uri, String xmlContent, Timestamp datestamp, String nativeURL) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(
				"* Entering data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " + service.getId());
		}

		// OBS att antalet parametrar etc *mÃ¥ste* stÃ¤mma med det statement som anvÃ¤nds
		// och som skapas och fÃ¶rbereds i konstruktorn!

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
	 * @param xmlContent xml-innehÃ¥ll
	 * @param datestamp postens Ã¤ndringsdatum (frÃ¥n oai-huvudet)
	 * @param nativeURL url till html-representation, eller null
	 * @throws Exception
	 */
	protected boolean updateRecord(String oaiURI, String uri, String xmlContent, Timestamp datestamp, String nativeURL) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(
				"* Updated data for oaiURI=" + oaiURI + ", uri=" + uri + " for service with ID: " + service.getId());
		}

		// OBS att antalet parametrar etc *mÃ¥ste* stÃ¤mma med det statement som anvÃ¤nds
		// och som skapas och fÃ¶rbereds i konstruktorn!

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
	 * Uppdaterar en befintlig eller stoppar in en ny post i repositoryt beroende pÃ¥ om posten
	 * finns och om tjÃ¤nsten klarar av att skicka persistent deletes.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param xmlContent xml-innehÃ¥ll
	 * @param datestamp postens Ã¤ndringsdatum (frÃ¥n oai-huvudet)
	 * @throws Exception
	 */
	protected void insertOrUpdateRecord(String oaiURI, String xmlContent, Timestamp datestamp) throws Exception {
		String uri = null;
		String nativeURL = null;

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
		// bÃ¶r/ska inte hÃ¤nda, men...
		if (uri == null) {
			ContentHelper.addProblemMessage("No uri found for " + oaiURI + " --SKIPPING--");
			return;
		}

		// gÃ¶r update och om ingen post uppdaterades stoppa in en (istf fÃ¶r att kolla om post
		// finns fÃ¶rst)
		if (!updateRecord(oaiURI, uri, xmlContent, datestamp, nativeURL)) {
			insertRecord(oaiURI, uri, xmlContent, datestamp, nativeURL);
		}
	}

	// gÃ¶r commit och rÃ¤kna up antalet lyckade Ã¥tgÃ¤rder
	private void commitIfLimitReached() throws Exception {
		commitIfLimitReached(false);
	}

	// gÃ¶r commit och rÃ¤kna up antalet lyckade Ã¥tgÃ¤rder, gÃ¶r alltid commit om argumentet Ã¤r
	// true
	private void commitIfLimitReached(boolean forceCommit) throws Exception {
		int count = numInsertedXact + numUpdatedXact + numDeletedXact;
		if (forceCommit || count >= XACT_LIMIT) {
			commitAndUpdateCounters();
		}
	}

	/**
	 * Uppdaterar obehandlade poster baserat pÃ¥ status och tjÃ¤nst. SÃ¤tter deleted och
	 * nollstÃ¤ller status.
	 * 
	 * @return antal databasfÃ¶rÃ¤ndringar
	 * @throws Exception
	 */
	protected int updateTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Update status for service with ID: " + service.getId());
		}
		final int BATCH_SIZE = 500;
		ss.setStatusText(service, "Attempting to update status and deleted column for pending records");
		int updated = 0;
		PreparedStatement updatePst = null;
		PreparedStatement selPst = null;
		ResultSet rs = null;
		try {
			// hÃ¤mta kvarvarande poster under behandling och sÃ¤tt deras deleted
			// och ta bort deras geometrier i batchar
			String sql = DBUtil.fetchFirst(c, "select uri from content where serviceid = ? and status <> ?",
				BATCH_SIZE);
			selPst = c.prepareStatement(sql);
			selPst.setString(1, service.getId());
			selPst.setInt(2, DBUtil.STATUS_NORMAL);

			// behÃ¥ll deleted om vÃ¤rdet finns, Ã¤ven fÃ¶r datestamp tas vÃ¤rdet frÃ¥n deleted
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
			// nÃ¤r vi fÃ¥r mindre Ã¤n (och har behandlat dem) batch-storleken har vi nÃ¥tt slutet
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
				// stÃ¤ng (och nollstÃ¤ll) rs fÃ¶r Ã¥teranvÃ¤ndning
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
	 * ÃterstÃ¤ller status-kolumnen och dÃ¤rmed status till ursprungligt sÃ¥ lÃ¥ngt det gÃ¥r.
	 * 
	 * @throws Exception vid fel
	 */
	protected int resetTmpStatus() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Attempting to reset status for pending records for service " + service.getId());
		}
		ss.setStatusText(service, "Recovery: Attempting to reset status for pending records");
		int numAffected = 0;
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
	 * GÃ¶r utestÃ¥ende commit och berÃ¤knar respektive Ã¥terstÃ¤ller antal uppdaterade total och
	 * fÃ¶r denna transaktion.
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

		String msg = "Committed (i/u/d " + numInserted + "/" + numUpdated + "/" + numDeleted + ") " +
			(numInserted + numUpdated + numDeleted) + " database changes";
		ss.setStatusText(service, msg);
		if (logger.isDebugEnabled()) {
			logger.debug(msg);
		}
	}

	/**
	 * HÃ¤mtar antal poster som stoppades in.
	 * 
	 * @return antal "nya" poster
	 */
	public int getInserted() {
		return numInserted;
	}

	/**
	 * HÃ¤mtar antalet borttagna poster.
	 * 
	 * @return antal borttagna poster
	 */
	public int getDeleted() {
		return numDeleted;
	}

	/**
	 * HÃ¤mtar antalet Ã¤ndrade poster.
	 * 
	 * @return antal Ã¤ndrade poster
	 */
	public int getUpdated() {
		return numUpdated;
	}

	/**
	 * Tolkar vÃ¤rdet som ett iso8601-datum (med ev tid).
	 * 
	 * @param value strÃ¤ng att tolka
	 * @return tolkad timestamp eller null om vÃ¤rdet inte gick att tolka
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

	public static void main(String[] args) {
		/*
		 * funkar inte riktigt fn Connection c = null; Statement st = null; FSDirectory dir = null;
		 * try { //File xmlFile = new File("d:/temp/oaipmh2.xml"); //File xmlFile = new
		 * File("d:/temp/kthdiva_2694.xml"); File xmlFile = new File("d:/temp/kthdiva.xml");
		 * SAXParserFactory spf = SAXParserFactory.newInstance(); spf.setNamespaceAware(true);
		 * SAXParser p = spf.newSAXParser(); Class.forName("org.hsqldb.jdbcDriver"); c =
		 * DriverManager.getConnection("jdbc:hsqldb:file:d:/temp/harvestdb", "sa", ""); dir =
		 * FSDirectory.getDirectory(new File("d:/temp/lucene-index/test"));
		 * 
		 * OAIPMHHandler h = new OAIPMHHandler("test2", c); p.parse(xmlFile, h);
		 * DBBasedManagerImpl.commit(c);
		 * 
		 * st = c.createStatement(); st.execute("SHUTDOWN"); Thread.sleep(3000);
		 * System.out.println("OK - " + h.serviceId + ": handlesDeleted: " + h.getSupportsDeleted()
		 * + ", inserted: " + h.getInserted() + ", deleted: " + h.getDeleted() + ", updated: " +
		 * h.getUpdated()); } catch (Exception e) { e.printStackTrace();
		 * DBBasedManagerImpl.rollback(c); } finally { DBBasedManagerImpl.closeDBResources(null,
		 * null, c); }
		 * 
		 * IndexSearcher s = null; if (dir != null) { try { s = new IndexSearcher(dir); QueryParser
		 * p = new QueryParser("dc_desc", new StandardAnalyzer()); String qs = (args.length == 0 ?
		 * "chine" : args[0]); Query q = p.parse(qs); System.err.println("SÃ¶ker med: " + qs); Hits
		 * hits = s.search(q); int antal = hits.length(); System.out.println("Fick " + antal +
		 * " trÃ¤ffar"); Iterator iter = hits.iterator(); int i = 0; while (iter.hasNext()) { Hit h
		 * = (Hit) iter.next(); ++i; org.apache.lucene.document.Document d = h.getDocument();
		 * System.out.println("## trÃ¤ff " + i + "/" + antal + ", score: " + h.getScore() + " ##");
		 * System.out.println("creator: " + d.get("dc_creator")); System.out.println("desc: " +
		 * d.get("dc_desc")); System.out.println(""); } } catch (Exception e) { e.printStackTrace();
		 * } finally { if (s != null) { try { s.close(); } catch (IOException e) {
		 * e.printStackTrace(); } } } }
		 */
	}
}
