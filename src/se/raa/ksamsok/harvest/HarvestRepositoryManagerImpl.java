package se.raa.ksamsok.harvest;

import java.io.BufferedInputStream;
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
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.DCContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;
import se.raa.ksamsok.lucene.SamsokContentHelper;
import se.raa.ksamsok.spatial.GMLDBWriter;
import se.raa.ksamsok.spatial.GMLUtil;

public class HarvestRepositoryManagerImpl extends DBBasedManagerImpl implements HarvestRepositoryManager {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.HarvestRepositoryManager");

	private static final ContentHelper samsokContentHelper = new SamsokContentHelper();
	private static final ContentHelper dcContentHelper = new DCContentHelper();

	private SAXParserFactory spf;
	private StatusService ss;
	private File spoolDir;

	public HarvestRepositoryManagerImpl(DataSource ds, StatusService ss, File spoolDir) {
		super(ds);
		spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		this.ss = ss;
		this.spoolDir = spoolDir;
	}

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

	public void updateLuceneIndex(HarvestService service, Timestamp ts) throws Exception {
		updateLuceneIndex(service, ts, null);
	}

	public void updateLuceneIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		IndexWriter iw = null;
		String serviceId = null;
		boolean refreshIndex = false;
		synchronized (LuceneServlet.IW_SYNC) { // en i taget som får köra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service, ts);
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() + ", updating index (" + count + " records) - start");
				}
				serviceId = service.getId();
				c = ds.getConnection();
				String sql;
				if (ts != null) {
					sql = "select uri, deleted, xmldata from content where serviceId = ? and changed > ?";
				} else {
					sql = "select xmldata from content where serviceId = ? and deleted is null";
				}
				pst = c.prepareStatement(sql);
				pst.setString(1, serviceId);
				if (ts != null) {
					pst.setTimestamp(2, ts);
				}
				rs = pst.executeQuery();
				iw = LuceneServlet.getInstance().borrowIndexWriter();
				if (ts == null) {
					iw.deleteDocuments(new Term(ContentHelper.I_IX_SERVICE, serviceId));
				}
				//String oaiURI;
				String uri;
				String xmlContent;
				int i = 0;
				int nonI = 0;
				int deleted = 0;
				ContentHelper helper = getContentHelper(service);
				ContentHelper.initProblemMessages();
				while (rs.next()) {
					//oaiURI = rs.getString("oaiuri");
					if (ts != null) {
						uri = rs.getString("uri");
						iw.deleteDocuments(new Term(ContentHelper.IX_ITEMID, uri));
						if (rs.getTimestamp("deleted") != null) {
							++deleted;
							// om borttagen, gå till nästa
							continue;
						}
					}
					xmlContent = rs.getString("xmldata");
					Document doc = helper.createLuceneDocument(service, xmlContent);
					if (doc == null) {
						// inget dokument betyder att tjänsten har skickat itemForIndexing=n
						++nonI;
						continue;
					}
					iw.addDocument(doc);
					++i;
					if (i % 1000 == 0) { // TODO: konstant?
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
				iw.commit();
				refreshIndex = true;
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Updated index, " + i + " records (" + 
						(ts == null ? "delete + insert" : "updated incl " + deleted + " deleted") +
						(nonI > 0 ? ", itemForIndexing=n: " + nonI : "") +
						"), time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", updated index - done, " + (ts == null ?
									"first removed all and then inserted " :
									"updated incl " + deleted + " deleted ") + i +
							" records in the lucene index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Error when aborting for lucene index", e2);
					}
				}
				logger.error(serviceId + ", error when updating lucene index", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(rs, pst, c);
				LuceneServlet.getInstance().returnIndexWriter(iw, refreshIndex);
			}
			// rapportera eventuella problemmeddelanden
			reportAndClearProblemMessages(service, "indexing");
		}
	}

	public void removeLuceneIndex(HarvestService service) throws Exception {
		IndexWriter iw = null;
		String serviceId = null;
		boolean refreshIndex = false;
		synchronized (LuceneServlet.IW_SYNC) { // en i taget som får köra index-write
			try {
				long start = System.currentTimeMillis();
				int count = getCount(service);
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() + ", removing index (" + count + " records) - start");
				}
				serviceId = service.getId();
				iw = LuceneServlet.getInstance().borrowIndexWriter();
				iw.deleteDocuments(new Term(ContentHelper.I_IX_SERVICE, serviceId));

				iw.commit();
				refreshIndex = true;
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Removed index, " + count + " records" + 
						", time: " + runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", removed index - done, " + "removed " + count +
							" records in the lucene index, time: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Error when aborting for lucene index", e2);
					}
				}
				logger.error(serviceId + ", error when updating lucene index", e);
				throw e;
			} finally {
				LuceneServlet.getInstance().returnIndexWriter(iw, refreshIndex);
			}
		}
	}


	public void deleteData(HarvestService service) throws Exception {
		Connection c = null;
		PreparedStatement pst = null;
		IndexWriter iw = null;
		String serviceId = null;
		boolean refreshIndex = false;
		synchronized (LuceneServlet.IW_SYNC) { // en i taget som får köra index-write
			try {
				serviceId = service.getId();
				c = ds.getConnection();
				// rensa först allt vanligt innehåll
				pst = c.prepareStatement("delete from content where serviceId = ?");
				pst.setString(1, serviceId);
				pst.executeUpdate();
				// och rensa ev spatial-data för tjänsten
				GMLDBWriter gmlDBWriter = GMLUtil.getGMLDBWriter(service.getId(), c);
				if (gmlDBWriter != null) {
					gmlDBWriter.deleteAllForService();
				}
				iw = LuceneServlet.getInstance().borrowIndexWriter();
				iw.deleteDocuments(new Term(ContentHelper.I_IX_SERVICE, serviceId));
				iw.prepareCommit();
				DBUtil.commit(c);
				iw.commit();
				refreshIndex = true;
				if (logger.isInfoEnabled()) {
					logger.info("Removed all records for " + serviceId);
				}
			} catch (Exception e) {
				DBUtil.rollback(c);
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Error when aborting for lucene index", e2);
					}
				}
				logger.error(serviceId + ", error at delete", e);
				throw e;
			} finally {
				DBUtil.closeDBResources(null, pst, c);
				LuceneServlet.getInstance().returnIndexWriter(iw, refreshIndex);
			}
		}
	}

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
	public File getSpoolFile(HarvestService service) {
		return new File(spoolDir, service.getId() + "_.xml");
	}
	
 	public File getZipFile(HarvestService service){
		return new File(getSpoolFile(service).getAbsolutePath() + ".gz");
	}

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
			ss.setStatusTextAndLog(service, "Note! Problem(s) when " + operation + " ");
			logger.warn(service.getId() + ", got following problem(s) when " + operation + ": ");
			for (String uri: problemMessages.keySet()) {
				ss.setStatusTextAndLog(service, uri + " - " + problemMessages.get(uri) + " times");
				logger.warn("  " + uri + " - " + problemMessages.get(uri) + " times");
			}
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
		return dcContentHelper;
	}

}
