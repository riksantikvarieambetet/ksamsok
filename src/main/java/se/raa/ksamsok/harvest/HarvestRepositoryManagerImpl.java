package se.raa.ksamsok.harvest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;

import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class HarvestRepositoryManagerImpl extends DBBasedManagerImpl implements HarvestRepositoryManager {

	private static final Logger logger = LogManager.getLogger(HarvestRepositoryManagerImpl.class);

	/** parameter som pekar ut var hämtad xml ska mellanlagras, om ej satt används tempdir */
	protected static final String D_HARVEST_SPOOL_DIR = "samsok-harvest-spool-dir";

	private static final Object SYNC = new Object(); // används för att synka skrivningar till solr

	private static final ContentHelper samsokContentHelper = new SamsokContentHelper(true);

	// antal solr-dokument som skickas per batch, för få -> mycket io, för många -> mycket minne
	private static final int solrBatchSize = 10;
	// statusrapportering sker efter uppdatering av detta antal objekt
	private static final int statusReportBatchSize = 500;

	private SAXParserFactory spf;
	private StatusService ss;
	private File spoolDir;
	private SolrClient solr;

	public HarvestRepositoryManagerImpl(DataSource ds, StatusService ss, SolrClient solr) {
		super(ds);
		spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		this.ss = ss;
		spoolDir = new File(System.getProperty(D_HARVEST_SPOOL_DIR,
				System.getProperty("java.io.tmpdir")));
		if (!spoolDir.exists() || !spoolDir.isDirectory() || !spoolDir.canWrite()) {
			throw new RuntimeException("Kan inte läsa spoolkatalog: " + spoolDir);
		}
		this.solr = solr;
	}

	@Override
	public boolean storeHarvest(HarvestService service, ServiceMetadata sm,
			File xmlFile, Timestamp ts) throws Exception {
		Connection c = null;
		String serviceId = null;
		OAIPMHHandler h = null;
		boolean updated;
		ContentHelper.initProblemMessages();
		try {
			serviceId = service.getId();
			c = ds.getConnection();
			SAXParser p = spf.newSAXParser();
			h = new OAIPMHHandler(ss, service, getContentHelper(service), sm, c, ts);
			if (!sm.handlesPersistentDeletes()) {
				// ta bort alla gamla poster om inte denna tjänst klarar persistenta deletes
				// inget tas egentligen bort utan posternas status sätts till pending
				h.deleteAllFromThisService();
			} else {
				// nollställ status för att bättre klara en inkrementell
				// skörd efter en misslyckad full skörd
				h.resetTmpStatus();
			}
			// gå igenom xml-filen och uppdatera databasen med filens poster
			p.parse(xmlFile, h);
			// gör utestående commit och uppdatera räknare inför statusuppdatering
			h.commitAndUpdateCounters();
			// uppdatera status och data för berörda poster samt nollställ temp-kolumner
			// OBS att commit görs i anropet nedan nu när den gör batch-commits
			h.updateTmpStatus();
			// flagga för att något har uppdaterats
			updated = (h.getDeleted() > 0 || h.getInserted() > 0 || h.getUpdated() > 0);
			ss.setStatusTextAndLog(service, "Stored harvest (i/u/d " + h.getInserted() +
					"/" + h.getUpdated() + "/" + h.getDeleted() + ")");
		} catch (Throwable e) {
			DBUtil.rollback(c);
			if (h != null) {
				ss.setStatusTextAndLog(service, "Stored part of harvest before error (i/u/d " +
						h.getInserted() + "/" + h.getUpdated() + "/" + h.getDeleted() + ")");
				// försök återställ status för poster till normaltillstånd
				// hängslen och livrem då status också sätts om i start av denna metod
				try {
					ss.setStatusText(service, "Attempting to reset status for records");
					h.resetTmpStatus();
				} catch (Throwable t) {
					ss.setStatusTextAndLog(service, "An error occured while attempting to " +
							"reset status after harvest storing failed: " +
							t.getMessage());
				}
			}
			logger.error(serviceId + ", error when storing harvest: " + e.getMessage());
			throw new Exception(e);
		} finally {
			DBUtil.closeDBResources(null, null, c);
			if (logger.isInfoEnabled() && h != null) {
				logger.info(serviceId +
						" (committed), deleted: " + h.getDeleted() +
						", new: " + h.getInserted() +
						", changed: " + h.getUpdated());
			}
			// frigör resurser såsom prepared statements etc
			if (h != null) {
				h.destroy();
			}
		}
		// rapportera eventuella problemmeddelanden
		reportAndClearProblemMessages(service, "storing harvest");

		return updated;
	}

	@Override
	public void updateIndex(HarvestService service, Timestamp ts) throws Exception {
		updateIndex(service, ts, null);
	}

	@Override
	public void updateIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		synchronized (SYNC) { // en i taget som får köra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service, ts);
				
				serviceId = service.getId();
				if (logger.isInfoEnabled()) {
					logger.info(serviceId + ", updating index (" + count + " records) - start");
				}
				c = ds.getConnection();
				String sql;
				if (ts != null) {
					sql = "select uri, deleted, added, xmldata from content where serviceId = ? and changed > ?";
				} else {
					sql = "select added, xmldata from content where serviceId = ? and deleted is null";
				}
				pst = c.prepareStatement(sql);
				pst.setString(1, serviceId);
				if (ts != null) {
					pst.setTimestamp(2, ts);
				}
				pst.setFetchSize(DBUtil.FETCH_SIZE);
				rs = pst.executeQuery();
				if (ts == null) {
					solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				}
				String uri;
				String xmlContent;
				Timestamp added;
				int i = 0;
				int deleted = 0;
				ContentHelper helper = getContentHelper(service);
				ContentHelper.initProblemMessages();
				// TODO: man skulle kunna strömma allt i en enda request, men jag tror inte man
				//       skulle tjäna så mycket på det
				//       se http://wiki.apache.org/solr/Solrj#Streaming_documents_for_an_update
				List<SolrInputDocument> docs = new ArrayList<>(solrBatchSize);
				while (rs.next()) {
					if (ts != null) {
						uri = rs.getString("uri");
						solr.deleteById(uri);
						if (rs.getTimestamp("deleted") != null) {
							++deleted;
							// om borttagen, gå till nästa
							continue;
						}
					}
					xmlContent = rs.getString("xmldata");
					added = rs.getTimestamp("added");
					SolrInputDocument doc;
					doc = helper.createSolrDocument(service, xmlContent, added);
					if (doc == null) {
						// Some error occured, it has been logged in createSolrDocument.
						// Nothing to see here - carry on
						continue;
					}
					docs.add(doc);
					++i;
					if (i % solrBatchSize == 0) {
						// skicka batchen
						if (logger.isDebugEnabled()) {
							logger.debug("Skickar " + docs.size() + " dokument");
						}
						solr.add(docs);
						docs.clear();
					}
					if (i % statusReportBatchSize == 0) {
						ss.checkInterrupt(service);
						if (enclosingService != null) {
							ss.checkInterrupt(enclosingService);
						}
						long deltaMillis = System.currentTimeMillis() - start;
			            long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(
			            		deltaMillis, i, count);
						ss.setStatusText(service, "Updated " + i +
								(ts != null ? " (and deleted " + deleted + ")" : "") +
								" of " + count + " fetched records in the index" +
		            			(aproxMillisLeft >= 0 ? " (estimated time remaining: " +
		            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						if (logger.isDebugEnabled()) {
							logger.debug(service.getId() + ", has updated " + i +
									(ts != null ? " (and deleted " + deleted + ")" : "") +
									" of " + count + " fetched records in lucene" +
			            			(aproxMillisLeft >= 0 ? " (estimated time remaining: " +
			            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						}
					}
				}
				if (docs.size() > 0) {
					// skicka sista del-batchen
					if (logger.isDebugEnabled()) {
						logger.debug("Skickar de sista " + docs.size() + " dokumenten");
					}
					solr.add(docs);
					docs.clear();
				}
				solr.commit();
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Updated index, " + i + " records (" + 
						(ts == null ? "delete + insert" : "updated incl " + deleted + " deleted") +
						"), time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", updated index - done, " + (ts == null ?
									"first removed all and then inserted " :
									"updated incl " + deleted + " deleted ") + i +
							" records in the index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error when updating index", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(rs, pst, c);
			}
			// rapportera eventuella problemmeddelanden
			reportAndClearProblemMessages(service, "indexing");
		}
	}

	@Override
	public void deleteIndexData(HarvestService service) throws Exception {
		String serviceId = null;
		synchronized (SYNC) { // en i taget som får köra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service);
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() + ", removing index (" + count + " records) - start");
				}
				serviceId = service.getId();
				solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				solr.commit();
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Removed index, " + count + " records" + 
						", time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", removed index - done, " + "removed " + count +
							" records in the index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error when updating index", e);
				throw e;
			}
		}
	}

	@Override
	public void deleteData(HarvestService service) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		String serviceId = null;
		synchronized (SYNC) { // en i taget som får köra index-write
			try {
				Timestamp ts = new Timestamp(new Date().getTime());
				serviceId = service.getId();
				c = ds.getConnection();
				// rensa först allt vanligt innehåll, ta bara bort data, inte själva raderna

				// behåll deleted om värdet finns, även för datestamp tas värdet från deleted
				pst = c.prepareStatement("update content set changed = ?, deleted = coalesce(deleted, ?), " +
						"datestamp = coalesce(deleted, ?), status = ?, xmldata = null where serviceid = ?");
				pst.setTimestamp(1, ts);
				pst.setTimestamp(2, ts);
				pst.setTimestamp(3, ts);
				pst.setInt(4, DBUtil.STATUS_NORMAL);
				pst.setString(5, serviceId);
				pst.execute();

				solr.deleteByQuery(ContentHelper.I_IX_SERVICE + ":" + serviceId);
				// commit först för db då den är troligast att den smäller och sen solr
				DBUtil.commit(c);
				solr.commit();
				if (logger.isInfoEnabled()) {
					logger.info("Removed all records for " + serviceId);
				}
			} catch (Exception e) {
				DBUtil.rollback(c);
				try {
					solr.rollback();
				} catch (Exception e2) {
					logger.warn("Error when aborting for index", e2);
				}
				logger.error(serviceId + ", error at delete", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(null, pst, c);
			}
		}
	}

	@Override
	public boolean existsInDatabase(String uri) throws Exception {
		boolean existsInDatabase = false;
		String xmlContent = null;
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("select * from content where uri = ?");
			pst.setString(1, uri);
			rs = pst.executeQuery();
			if (rs.next()) {
				existsInDatabase = true;
			}
		} catch (Exception e) {
			logger.error("Error when checking whether uri " + uri + " exists in database", e);
			logger.error(e.getMessage());
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return existsInDatabase;
	}

	@Override
	public String getXMLData(String uri) throws Exception {
		String xmlContent = null;
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("select xmldata from content where uri = ? and deleted is null");
			pst.setString(1, uri);
			rs = pst.executeQuery();
			if (rs.next()) {
				xmlContent = rs.getString("xmldata");
			}
		} catch (Exception e) {
			logger.error("Error when fetching xml data for uri " + uri, e);
			logger.error(e.getMessage());
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return xmlContent;
	}

	@Override
	public int getCount(HarvestService service) throws Exception {
		return getCount(service, null);
	}

	/**
	 * Ger antal poster i repositoryt för en tjänst. Om ts skickas in ges antal poster
	 * som ändrats efter ts.
	 * 
	 * @param service tjänst
	 * @param ts timestamp
	 * @return antal poster
	 * @throws Exception
	 */
	protected int getCount(HarvestService service, Timestamp ts) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		int count = 0;
		try {
			serviceId = service.getId();
			c = ds.getConnection();
			String sql = "select count(*) from content where serviceId = ? and deleted is null";
			if (ts != null) {
				sql += " and changed > ?";
			}
			pst = c.prepareStatement(sql);
			pst.setString(1, serviceId);
			if (ts != null) {
				pst.setTimestamp(2, ts);
			}
			rs = pst.executeQuery();
			if (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (Exception e) {
			logger.error(serviceId + ", error when fetching number of records", e);
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return count;
	}

	@Override
	public Map<String, Integer> getCounts() throws Exception {
		Map<String, Integer> countMap = new HashMap<>();
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String serviceId = null;
		int count;
		try {
			c = ds.getConnection();
			// ora: lite special istället för group by för att få db-index att vara med och slippa full table scan...
			//String sql = "select s.serviceId, (select count(*) from content where serviceId = s.serviceId and deleted is null) c from harvestservices s";
			// pg: verkar klara group by bättre (troligen index-relaterat också), ca 13 sek för ovan mot ca 4 för nedan
			String sql = "select serviceId, count(uri) from content where deleted is null " +
				// villkor som filtrerar bort de poster i content som inte har nån "korrekt" tjänst - bortkommenterat tills vidare
				//"and serviceId in (select serviceId from harvestservices) " +
				"group by serviceId";
			pst = c.prepareStatement(sql);
			rs = pst.executeQuery();
			while (rs.next()) {
				serviceId = rs.getString(1);
				count = rs.getInt(2);
				countMap.put(serviceId, count);
			}
		} catch (Exception e) {
			logger.error(serviceId + ", error when fetching number of records", e);
			throw e;
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return countMap;
	}

	@Override
	public void optimizeIndex() throws Exception {
		synchronized (SYNC) { // en i taget som får köra index-write
			solr.optimize();
		}
	}

	@Override
	public void clearIndex() throws Exception {
		synchronized (SYNC) { // en i taget som får köra index-write
			solr.deleteByQuery("*:*");
			solr.commit();
		}
	}

	@Override
	public File getSpoolFile(HarvestService service) {
		return new File(spoolDir, service.getId() + "_.xml");
	}

	@Override
 	public File getZipFile(HarvestService service){
		return new File(getSpoolFile(service).getAbsolutePath() + ".gz");
	}

	@Override
	public void extractGZipToSpool(HarvestService service){
		OutputStream os = null;
		InputStream is = null;
		File outputFile = getSpoolFile(service);
		File inputFile = getZipFile(service);
		
		byte[] buf = new byte[8192];
		int c;
		try {
			is = new GZIPInputStream(new FileInputStream(inputFile));
			os = new BufferedOutputStream( new FileOutputStream(outputFile));
			while ((c = is.read(buf)) > 0) {
				os.write(buf, 0, c);
			}
			os.flush();
		}
		catch(IOException e){
			logger.error("error when unzipping harvest zip file", e);
		}
	 finally {
			closeStream(is);
			closeStream(os);
		}
	}
	
	/**
	 * Hjälpmetod som stänger en ström.
	 * 
	 * @param stream ström
	 */
	protected void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Rensa och rapportera ev problemmeddelanden till statusservicen och logga. 
	 * @param service tjänst
	 * @param operation operation för loggmeddelande, kort format (eng)
	 */
	private void reportAndClearProblemMessages(HarvestService service, String operation) {
		// rapportera eventuella problemmeddelanden
		Map<String,Integer> problemMessages = ContentHelper.getAndClearProblemMessages();
		if (problemMessages != null && problemMessages.size() > 0) {
			Date nowDate = new Date();
			ss.setWarningTextAndLog(service, "Note! Problem(s) when " + operation, nowDate);
			logger.warn(service.getId() + ", got following problem(s) when " + operation + ": ");
			problemMessages.forEach((uri, message) -> {
				ss.setWarningTextAndLog(service, uri + " - " + message + " times", nowDate);
				logger.warn("  " + uri + " - " + message + " times");
			});
		}
	}

	/**
	 * Hämtar rätt typ av ContentHelper för tjänst.
	 * 
	 * @param service tjänst
	 * @return ContentHelper
	 */
	protected static ContentHelper getContentHelper(HarvestService service) {
		// TODO: bättre kontroll
		if (service.getServiceType().endsWith("-SAMSOK")) {
			return samsokContentHelper;
		}
		logger.warn("ContentHelper för tjänst kunde ej bestämmas, npe inkommande?");
		return null;
	}

	@Override
	public File getSpoolDir() {
		return spoolDir;
	}

}
