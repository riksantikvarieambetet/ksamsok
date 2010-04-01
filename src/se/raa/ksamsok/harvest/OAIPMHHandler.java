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

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.spatial.GMLDBWriter;
import se.raa.ksamsok.spatial.GMLInfoHolder;
import se.raa.ksamsok.spatial.GMLUtil;

/**
 * Handler för xml-parsning som lagrar poster i repositoryt och gör commit med jämna
 * mellanrum (för att inte oracle ska få spunk, derby klarar det).
 * Formatet på xml:en ska vara samma som för RawWrite, dvs i princip OAI-PMH med en
 * omslutande tagg.
 */
public class OAIPMHHandler extends DefaultHandler {

	// antal databasoperationer innan en commit görs
	private static final int XACT_LIMIT = 1000;

	Connection c;
	GMLDBWriter gmlDBWriter;
	HarvestService service;
	ContentHelper contentHelper;
	String oaiURI;
	String datestamp;
	int mode = 0;
	private boolean deleteRecord;
	private StringBuffer buf = new StringBuffer();
	private HashMap<String,String> prefixMap = new HashMap<String, String>();
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
	private ServiceMetadata sm;
	private String errorCode;
	private Timestamp ts;
	private StatusService ss;

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.OAIPMHHandler");


	public OAIPMHHandler(StatusService ss, HarvestService service, ContentHelper contentHelper, ServiceMetadata sm, Connection c, Timestamp ts) {
		this.ss = ss;
		this.service = service;
		this.contentHelper = contentHelper;
		this.c = c;
		this.sm = sm;
		this.ts = ts;
		gmlDBWriter = GMLUtil.getGMLDBWriter(service.getId(), c);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		if (mode == COPY) {
			prefixMap.put(prefix, uri);
			try {
				xxmlw.setPrefix(prefix, uri);
			} catch (Exception e) {
				throw new SAXException(e);
			}

		}
	}

	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		switch (mode) {
		case NORMAL:
			if ("record".equals(name)) {
				// byt till "record-mode"
				mode = RECORD;
				deleteRecord = false;
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
					//xxmlw.writeStartDocument("UTF-8", "1.0");
				} catch (Exception e) {
					throw new SAXException(e);
				}
			} else if ("header".equals(name)) {
				// kontrollera om status säger deleted, då ska denna post bort
				String status = attributes.getValue("", "status");
				if ("deleted".equals(status)) {
					if (!sm.canSendDeletes()) {
						throw new SAXException("Service is not supposed to handle deletes but did in fact send one!");
					}
					deleteRecord = true;
				}
			}
			break;
		case COPY:
			// i "copy-mode" kopiera hela taggen som den är
			try {
				xxmlw.writeStartElement(uri, localName);
			} catch (Exception e) {
				throw new SAXException(e);
			}
			if (prefixMap.size() > 0) {
				for (String prefix: prefixMap.keySet()) {
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
						// Om ej rätt metod används blir det ett exception
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
	
	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		switch (mode) {
		case COPY:
			if ("metadata".equals(name)) {
				// tillbaks till "record-mode"
				mode = RECORD;
				try {
					// spara i databas
					//xxmlw.writeEndDocument(); // inget end doc, vi vill ha xml-fragment
					xxmlw.close();
					xxmlw = null;
					insertOrUpdateRecord(oaiURI, xsw.toString());
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
			// återställ char-buff
			buf.setLength(0);
			break;
		case RECORD:
			if ("identifier".equals(name)) {
				// läs ut värde för identifier
				oaiURI = buf.toString().trim();
			} else if ("datestamp".equals(name)) {
				// läs ut värde för datum
				datestamp = buf.toString().trim();
			} else if ("record".equals(name)) {
				// tillbaks till "normal-mode" 
				mode = NORMAL;
				if (deleteRecord) {
					// ta bort post nu om vi skulle göra det
					try {
						deleteRecord(oaiURI);
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
		}
		
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		buf.append(ch, start, length);
	}

	/**
	 * Tar bort alla poster i repositoryt för tjänsten vi jobbar med.
	 * 
	 * @throws Exception
	 */
	protected void deleteAllFromThisService() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Tar bort allt från tjänst med id: " + service.getId());
		}
		PreparedStatement pst = null;
		try {
			if (gmlDBWriter != null) {
				gmlDBWriter.deleteAllForService();
			}
			pst = c.prepareStatement("delete from content where serviceId = ?");
			pst.setString(1, service.getId());
			int num = pst.executeUpdate();
			numDeletedXact += num;
			if (logger.isDebugEnabled()) {
				logger.debug("** Removed " + num + " records for service: " + service.getId());
			}
			commitIfLimitReached();
		} finally {
			DBBasedManagerImpl.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Tar bort post i repositoryt med inskickad OAI-identifierare (Obs != rdf-identifierare)
	 * @param oaiURI OAI-identifierare
	 * @throws Exception
	 */
	protected void deleteRecord(String oaiURI) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Removing oaiURI=" + oaiURI + " from service with ID: " + service.getId());
		}
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			// bort med ev spatialt data
			if (gmlDBWriter != null) {
				// hämta ut uri:n då oai-uri bara är intern identifierare
				pst = c.prepareStatement("select uri from content where oaiuri = ?");
				pst.setString(1, oaiURI);
				rs = pst.executeQuery();
				if (rs.next()) {
					String uri = rs.getString("uri");
					gmlDBWriter.delete(uri);
				}
				// stäng för återanvändning (rs stängs i finally)
				pst.close();
			}
			pst = c.prepareStatement("delete from content where serviceId = ? and oaiuri = ?");
			pst.setString(1, service.getId());
			pst.setString(2, oaiURI);
			int num = pst.executeUpdate();
			numDeletedXact += num;
			if (logger.isDebugEnabled()) {
				logger.debug("* Removed " + num + " number of oaiURI=" + oaiURI +
						" from service with ID: " + service.getId());
			}
			commitIfLimitReached();
		} finally {
			DBBasedManagerImpl.closeDBResources(rs, pst, null);
		}
	}
	
	/**
	 * Stoppar in en ny post i repositoryt med angiven OAI-identifierare, identifierare och
	 * xml-innehåll.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-innehåll
	 * @param gmlInfoHolder hållare för geometrier mm
	 * @throws Exception
	 */
	protected void insertRecord(String oaiURI, String uri, String xmlContent,
			GMLInfoHolder gmlInfoHolder) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Entering data for oaiURI=" + oaiURI + ", uri=" +
					uri + " for service with ID: " + service.getId());
		}
		PreparedStatement pst = null;
		try {
			pst = c.prepareStatement("insert into content " +
					"(uri, oaiuri, serviceId, xmldata, changed) " +
					"values (?, ?, ?, ?, ?)");
			pst.setString(1, uri);
			pst.setString(2, oaiURI);
			pst.setString(3, service.getId());
			pst.setCharacterStream(4, new StringReader(xmlContent), xmlContent.length());
			pst.setTimestamp(5, ts);
			pst.executeUpdate();
			// stoppa in ev spatialdata om vi har nåt
			if (gmlDBWriter != null && gmlInfoHolder != null && gmlInfoHolder.hasGeometries()) {
				gmlDBWriter.insert(gmlInfoHolder);
			}
			++numInsertedXact;
			if (logger.isDebugEnabled()) {
				logger.debug("* Entered data for oaiURI=" + oaiURI + ", uri=" +
						uri + " for service with ID: " + service.getId());
			}
			commitIfLimitReached();
		} finally {
			DBBasedManagerImpl.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Uppdaterar en post i repositoryt.
	 * 
	 * @param oaiURI OAI-identifierare
	 * @param uri (rdf-)identifierare
	 * @param xmlContent xml-innehåll
	 * @param gmlInfoHolder hållare för geometrier mm
	 * @throws Exception
	 */
	protected void updateRecord(String oaiURI, String uri, String xmlContent,
			GMLInfoHolder gmlInfoHolder) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("* Updated data for oaiURI=" + oaiURI + ", uri=" +
					uri + " for service with ID: " + service.getId());
		}
		PreparedStatement pst = null;
		try {
			pst = c.prepareStatement("update content set oaiuri = ?, " +
			"serviceId = ?, changed = ?, xmldata = ? where uri = ?");
			pst.setString(1, oaiURI);
			pst.setString(2, service.getId());
			pst.setTimestamp(3, ts);
			pst.setCharacterStream(4, new StringReader(xmlContent), xmlContent.length());
			pst.setString(5, uri);
			pst.executeUpdate();
			// spara gml (obs, inget villkor på att det finns geometrier då det kanske
			// fanns gamla som nu ska tas bort)
			if (gmlDBWriter != null && gmlInfoHolder != null) {
				gmlDBWriter.update(gmlInfoHolder);
			}
			++numUpdatedXact;
			if (logger.isDebugEnabled()) {
				logger.debug("* Updated data for oaiURI=" + oaiURI + ", uri=" +
						uri + " for service with ID: " + service.getId());
			}
			commitIfLimitReached();
		} finally {
			DBBasedManagerImpl.closeDBResources(null, pst, null);
		}
	}

	/**
	 * Uppdaterar en befintlig eller stoppar in en ny post i repositoryt beroende på om
	 * posten finns och om tjänsten klarar av att skicka persistent deletes.
	 *  
	 * @param oaiURI OAI-identifierare
	 * @param xmlContent xml-innehåll
	 * @throws Exception
	 */
	protected void insertOrUpdateRecord(String oaiURI, String xmlContent) throws Exception {
		String uri = null;
		GMLInfoHolder gmlih = null;
		if (gmlDBWriter != null) {
			// om vi ska hantera spatiala data, skapa en datahållare att fylla på
			gmlih = new GMLInfoHolder();
		}
		try {
			uri = contentHelper.extractIdentifierAndGML(xmlContent, gmlih);
			PreparedStatement pst = null;
			ResultSet rs = null;
			try {
				if (sm.canSendDeletes()) {
					// insert eller update
					pst = c.prepareStatement("select oaiuri from content where uri = ?");
					pst.setString(1, uri);
					rs = pst.executeQuery();
					if (rs.next()) {
						updateRecord(oaiURI, uri, xmlContent, gmlih);
					} else {
						insertRecord(oaiURI, uri, xmlContent, gmlih);
					}
				} else {
					// insert bara
					insertRecord(oaiURI, uri, xmlContent, gmlih);
				}
			} finally {
				DBBasedManagerImpl.closeDBResources(rs, pst, null);
			}
		} catch (Exception e) {
			logger.error("Error when storing " + (uri != null ? uri : oaiURI), e);
			throw new Exception("Error when storing " + (uri != null ? uri : oaiURI) +
					": " + e.getMessage());
		}
	}

	// gör commit och räkna up antalet lyckade åtgärder
	private void commitIfLimitReached() throws Exception {
		int count = numInsertedXact + numUpdatedXact + numDeletedXact;
		if (count >= XACT_LIMIT) {
			commitAndUpdateCounters();
		}
	}

	/**
	 * Gör utestående commit och beräknar respektive återställer antal uppdaterade
	 * total och för denna transaktion.
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

		String msg = "Committed (i/u/d " + numInserted +
			"/" + numUpdated + "/" + numDeleted + ") " +
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

	public static void main(String[] args) {
		/* funkar inte riktigt fn
		Connection c = null;
		Statement st = null;
		FSDirectory dir = null;
		try {
			//File xmlFile = new File("d:/temp/oaipmh2.xml");
			//File xmlFile = new File("d:/temp/kthdiva_2694.xml");
			File xmlFile = new File("d:/temp/kthdiva.xml");
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			SAXParser p = spf.newSAXParser();
			Class.forName("org.hsqldb.jdbcDriver");
			c = DriverManager.getConnection("jdbc:hsqldb:file:d:/temp/harvestdb", "sa", "");
			dir = FSDirectory.getDirectory(new File("d:/temp/lucene-index/test"));

			OAIPMHHandler h = new OAIPMHHandler("test2", c);
			p.parse(xmlFile, h);
			DBBasedManagerImpl.commit(c);
			
			st = c.createStatement();
			st.execute("SHUTDOWN");
			Thread.sleep(3000);
			System.out.println("OK - " + h.serviceId + ": handlesDeleted: " + h.getSupportsDeleted() +
					", inserted: " + h.getInserted() +
					", deleted: " + h.getDeleted() +
					", updated: " + h.getUpdated());
		} catch (Exception e) {
			e.printStackTrace();
			DBBasedManagerImpl.rollback(c);
		} finally {
			DBBasedManagerImpl.closeDBResources(null, null, c);
		}
		
		IndexSearcher s = null;
		if (dir != null) {
			try {
				s = new IndexSearcher(dir);
				QueryParser p = new QueryParser("dc_desc", new StandardAnalyzer());
				String qs = (args.length == 0 ? "chine" : args[0]);
				Query q = p.parse(qs);
				System.err.println("Söker med: " + qs);
				Hits hits = s.search(q);
				int antal = hits.length();
				System.out.println("Fick " + antal + " träffar");
				Iterator iter = hits.iterator();
				int i = 0;
				while (iter.hasNext()) {
					Hit h = (Hit) iter.next();
					++i;
					org.apache.lucene.document.Document d = h.getDocument();
					System.out.println("## träff " + i + "/" + antal + ", score: " + h.getScore() + " ##");
					System.out.println("creator: " + d.get("dc_creator"));
					System.out.println("desc: " + d.get("dc_desc"));
					System.out.println("");
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (s != null) {
					try {
						s.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		*/
	}
}
