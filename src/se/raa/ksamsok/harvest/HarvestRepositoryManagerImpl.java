package se.raa.ksamsok.harvest;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
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
			// ta bort alla gamla poster om inte denna tjänst klarar persistenta deletes 
			if (!sm.handlesPersistentDeletes()) {
				h.deleteAllFromThisService();
			}
			p.parse(xmlFile, h);
			h.commitAndUpdateCounters();

			// flagga för att något har uppdaterats
			updated = (h.getDeleted() > 0 || h.getInserted() > 0 || h.getUpdated() > 0);
			ss.setStatusTextAndLog(service, "Lagrade skörd (i/u/d " + h.getInserted() +
					"/" + h.getUpdated() + "/" + h.getDeleted() + ")");
		} catch (Throwable e) {
			rollback(c);
			if (h != null) {
				ss.setStatusTextAndLog(service, "Lagrade del av skörd innan fel (i/u/d " +
						h.getInserted() + "/" + h.getUpdated() + "/" + h.getDeleted() + ")");
			}
			logger.error(serviceId + ", fel vid lagring av skörd: " + e.getMessage());
			throw new Exception(e);
		} finally {
			closeDBResources(null, null, c);
			if (logger.isInfoEnabled() && h != null) {
				logger.info(serviceId +
						" (committade), bort: " + h.getDeleted() +
						", nya: " + h.getInserted() +
						", ändrade: " + h.getUpdated());
			}
		}
		// rapportera eventuella problemmeddelanden
		Map<String,Integer> problemMessages = ContentHelper.getAndClearProblemMessages();
		if (problemMessages != null && problemMessages.size() > 0) {
			ss.setStatusTextAndLog(service, "OBS! Fick följande problem vid lagring av skörd");
			logger.warn(serviceId + ", fick följande problem vid lagring av skörd: ");
			for (String uri: problemMessages.keySet()) {
				ss.setStatusTextAndLog(service, uri + " - " + problemMessages.get(uri) + " ggr");
				logger.warn("  " + uri + " - " + problemMessages.get(uri) + " ggr");
			}
		}
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
					logger.info(service.getId() + ", uppdaterar index (" + count + " poster) - start");
				}
				serviceId = service.getId();
				c = ds.getConnection();
				String sql = "select xmldata from content where serviceId = ?";
				if (ts != null) {
					sql = "select uri, xmldata from content where serviceId = ? and changed > ?";
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
				ContentHelper helper = getContentHelper(service);
				ContentHelper.initProblemMessages();
				while (rs.next()) {
					//oaiURI = rs.getString("oaiuri");
					if (ts != null) {
						uri = rs.getString("uri");
						iw.deleteDocuments(new Term(ContentHelper.IX_ITEMID, uri));
					}
					xmlContent = rs.getString("xmldata");
					iw.addDocument(helper.createLuceneDocument(service, xmlContent));
					++i;
					if (i % 1000 == 0) { // TODO: konstant?
						ss.checkInterrupt(service);
						if (enclosingService != null) {
							ss.checkInterrupt(enclosingService);
						}
						long deltaMillis = System.currentTimeMillis() - start;
			            long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(
			            		deltaMillis, i, count);
						ss.setStatusText(service, "Har uppdaterat " + i + "/" + count +
								" poster i indexet" +
		            			(aproxMillisLeft >= 0 ? " (beräknad återstående tid: " +
		            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						if (logger.isDebugEnabled()) {
							logger.debug(service.getId() + ", har uppdaterat " +
									i + "/" + count + " poster i lucene" +
			            			(aproxMillisLeft >= 0 ? " (beräknad återstående tid: " +
			            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": ""));
						}
					}
				}
				iw.commit();
				refreshIndex = true;
				long durationMillis = (System.currentTimeMillis() - start);
				String runTime = ContentHelper.formatRunTime(durationMillis);
				String speed = ContentHelper.formatSpeedPerSec(count, durationMillis);
				ss.setStatusTextAndLog(service, "Uppdaterade index, " + i + " poster (" + 
						(ts == null ? "bort + insert" : "updatering") + "), tid: " +
						runTime + " (" + speed + ")");
				if (logger.isInfoEnabled()) {
					logger.info(service.getId() +
							", uppdaterade index - klart, " + (ts == null ?
									"tog först bort alla och la sen till " :
									"uppdaterade ") + i +
							" poster i lucene-indexet, tid: " +
							runTime + " (" + speed + ")");
				}
			} catch (Exception e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid abort för lucene-index", e2);
					}
				}
				logger.error(serviceId + ", fel vid uppdatering av lucene-index", e);
				throw e;
			} finally {
				closeDBResources(rs, pst, c);
				LuceneServlet.getInstance().returnIndexWriter(iw, refreshIndex);
			}
			// rapportera eventuella problemmeddelanden
			Map<String,Integer> problemMessages = ContentHelper.getAndClearProblemMessages();
			if (problemMessages != null && problemMessages.size() > 0) {
				ss.setStatusTextAndLog(service, "OBS! Fick följande problem vid indexering");
				logger.warn(serviceId + ", fick följande problem vid indexering: ");
				for (String uri: problemMessages.keySet()) {
					ss.setStatusTextAndLog(service, uri + " - " + problemMessages.get(uri) + " ggr");
					logger.warn("  " + uri + " - " + problemMessages.get(uri) + " ggr");
				}
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
				commit(c);
				iw.commit();
				refreshIndex = true;
				if (logger.isInfoEnabled()) {
					logger.info("Tog bort alla poster för " + serviceId);
				}
			} catch (Exception e) {
				rollback(c);
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid abort för lucene-index", e2);
					}
				}
				logger.error(serviceId + ", fel vid delete", e);
				throw e;
			} finally {
				closeDBResources(null, pst, c);
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
			pst = c.prepareStatement("select xmldata from content where uri = ?");
			pst.setString(1, uri);
			rs = pst.executeQuery();
			if (rs.next()) {
				xmlContent = rs.getString("xmldata");
			}
		} catch (Exception e) {
			logger.error("Fel vid hämtning xmldata för uri " + uri, e);
			logger.error(e.getMessage());
			throw e;
		} finally {
			closeDBResources(rs, pst, c);
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
			String sql = "select count(*) from content where serviceId = ?";
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
			logger.error(serviceId + ", fel vid hämtning av antal poster", e);
			throw e;
		} finally {
			closeDBResources(rs, pst, c);
		}
		return count;
	}

	@Override
	public File getSpoolFile(HarvestService service) {
		return new File(spoolDir, service.getId() + "_.xml");
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
