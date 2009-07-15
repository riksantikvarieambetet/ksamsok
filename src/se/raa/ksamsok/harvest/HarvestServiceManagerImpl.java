package se.raa.ksamsok.harvest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

public class HarvestServiceManagerImpl extends DBBasedManagerImpl implements HarvestServiceManager {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.HarvestServiceManager");

	private static final String JOBGROUP_HARVESTERS = "harvesters";
	private static final String TRIGGER_SUFFIX = "-trigger";

	protected Scheduler scheduler;
	protected HarvestRepositoryManager hrm;
	protected StatusService ss;

	protected HarvestServiceManagerImpl(DataSource ds, HarvestRepositoryManager hrm, StatusService ss) {
		super(ds);
		this.hrm = hrm;
		this.ss = ss;
	}

	protected void init() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Init, start");
		}
		Connection c = null;
		try {
			c = ds.getConnection();
		} catch (Throwable t) {
			logger.error("Fel på datasource, kan inte få en connection", t);
		} finally {
			if (c != null) {
				c.close();
			}
		}
		SchedulerFactory schedFact = new StdSchedulerFactory();
		scheduler = schedFact.getScheduler();
		scheduler.getContext().put(SS_KEY, ss);
		scheduler.getContext().put(HSM_KEY, this);
		scheduler.getContext().put(HRM_KEY, hrm);
		scheduler.start();

		List<HarvestService> services = getServices();
		for (HarvestService service: services) {
			scheduleJob(service);
		}
		// skapa "tjänst" om den inte finns
		HarvestService luceneOptimizeService = getService(SERVICE_LUCENE_OPTIMIZE);
		if (luceneOptimizeService != null) {
			scheduleJob(luceneOptimizeService);
		} else {
			HarvestService service = newServiceInstance();
			service.setId(SERVICE_LUCENE_OPTIMIZE);
			service.setServiceType("_LUCENE_OPTIMIZE");
			service.setName("Lucene index optimizer");
			service.setCronString("0 30 6 * * ? 2049");
			createService(service);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Init, klart");
		}
	}

	@SuppressWarnings("unchecked")
	protected void destroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Destroy, start");
		}
		try {
			for (HarvestService service: getServices()) {
				Step s = ss.getStep(service);
				if (s != Step.IDLE) {
					if (logger.isDebugEnabled()) {
						logger.debug("Destroy, begär att körande jobb för " + service.getId() +
								" ska avbrytas i steg " + s);
					}
					ss.requestInterrupt(service);
				}
			}
		} catch (Exception e) {
			logger.error("Fel vid hämtning av tjänster att avbryta", e);
		}
		if (scheduler != null) {
			try {
				if (scheduler.isStarted()) {
					// stoppa nya jobb från att triggas
					scheduler.standby();
					// hämta nu körande jobb och avbryt dem
					List<JobExecutionContext> running = scheduler.getCurrentlyExecutingJobs();
					for (JobExecutionContext jce: running) {
						Job j = jce.getJobInstance();
						if (j instanceof InterruptableJob) {
							if (logger.isInfoEnabled()) {
								logger.info("Destroy, avbryter körande jobb (quartz): " + jce.getJobDetail().getName());
							}
							((InterruptableJob) j).interrupt();
						} else {
							if (logger.isInfoEnabled()) {
								logger.info("Destroy, körande jobb ej avbrytbart: " + jce.getJobDetail().getName());
							}
						}
					}
					// TODO: vänta lite också?
					scheduler.shutdown(true);
				}
			} catch (SchedulerException se) {
				logger.error("Fel vid shutdown av scheduler", se);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Destroy, klart");
		}
	}

	public void createService(HarvestService service) throws Exception {
	    Connection c = null;
	    PreparedStatement  pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("insert into harvestservices " +
					"(serviceId, name, cronstring, harvestURL, harvestSetSpec, serviceType, alwaysEverything) values " +
					"(?, ?, ?, ?, ?, ?, ?)");
			int i = 0;
			pst.setString(++i, service.getId());
			pst.setString(++i, service.getName());
			pst.setString(++i, service.getCronString());
			pst.setString(++i, service.getHarvestURL());
			pst.setString(++i, service.getHarvestSetSpec());
			pst.setString(++i, service.getServiceType());
			pst.setBoolean(++i, service.getAlwaysHarvestEverything());

			pst.executeUpdate();
			DBBasedManagerImpl.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Skapade ny tjänst med id " + service.getId());
			}
			try {
				scheduleJob(service);
			} catch (Exception e) {
				logger.warn("Problem att schedulera jobb för tjänst " + service.getId(), e);
			}
	    } catch (Exception e) {
	    	DBBasedManagerImpl.rollback(c);
	    	logger.error("Fel vid skapande av ny tjänst med id " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBBasedManagerImpl.closeDBResources(null, pst, c);
	    }
	}

	public HarvestService getService(String serviceId) throws Exception {
		HarvestService service = null;
	    Connection c = null;
	    PreparedStatement pst = null;
	    ResultSet rs = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("select * from harvestservices where serviceId = ?");
			pst.setString(1, serviceId);
			rs = pst.executeQuery();
			if (rs.next()) {
				service = newServiceInstance(rs);
			}
	    } finally {
	    	DBBasedManagerImpl.closeDBResources(rs, pst, c);
	    }
	    return service;
	}

	public List<HarvestService> getServices() throws Exception {
	    Connection c = null;
	    PreparedStatement  pst = null;
	    ResultSet rs = null;
	    List<HarvestService> services = new ArrayList<HarvestService>();
	    try {
	    	c = ds.getConnection();
	    	pst = c.prepareStatement("select * from harvestservices where serviceId <> '" +
	    			SERVICE_LUCENE_OPTIMIZE + "' order by name");
	    	rs = pst.executeQuery();
	    	while (rs.next()) {
	    		HarvestService service = newServiceInstance(rs);
	    		services.add(service);
	    	}
		} finally {
			DBBasedManagerImpl.closeDBResources(rs, pst, c);
		}
		return services;
	}

	public HarvestService newServiceInstance() {
		return new HarvestServiceImpl();
	}
	
	protected HarvestService newServiceInstance(ResultSet rs) throws SQLException {
		HarvestService service = newServiceInstance();
		service.setId(rs.getString("serviceId"));
		service.setServiceType(rs.getString("serviceType"));
		service.setName(rs.getString("name"));
		service.setCronString(rs.getString("cronstring"));
		service.setHarvestURL(rs.getString("harvestURL"));
		service.setHarvestSetSpec(rs.getString("harvestSetSpec"));
		Timestamp ts = rs.getTimestamp("lastHarvestDate");
		if (ts != null) {
			service.setLastHarvestDate(new Date(ts.getTime()));
		}
		service.setAlwaysHarvestEverything(rs.getBoolean("alwaysEverything"));
		return service;
	}

	public void triggerHarvest(HarvestService service) throws Exception {
		triggerJob(service.getId());
	}

	public void triggerReindex(HarvestService service) throws Exception {
		triggerJob(service, Step.INDEX);
	}

	public void triggerReindexAll() throws Exception {
		JobDetail jd = new JobDetail(SERVICE_LUCENE_REINDEX, JOBGROUP_HARVESTERS, LuceneReindexAllJob.class);
		scheduler.scheduleJob(jd, new SimpleTrigger(SERVICE_LUCENE_REINDEX + TRIGGER_SUFFIX, null));
	}

	public boolean interruptHarvest(HarvestService service) throws Exception {
		// begär att jobbet ska avbrytas, både på "logisk nivå" och på jobb-nivå
		ss.requestInterrupt(service);
		return interruptJob(service.getId());
	}

	public boolean interruptReindexAll() throws Exception {
		HarvestService service = newServiceInstance();
		service.setId(SERVICE_LUCENE_REINDEX);
		return interruptHarvest(service);
	}

	public boolean isRunning(HarvestService service) {
		return isJobRunning(service.getId());
	}

	public void updateService(HarvestService service) throws Exception {
		HarvestService dbService = getService(service.getId());
		if (dbService == null) {
			throw new RuntimeException("Hittade inte service med id: " + service.getId());
		}
	    Connection c = null;
	    PreparedStatement  pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("update harvestservices set " +
					"name = ?, " +
					"cronstring = ?, " +
					"harvestURL = ?, " +
					"harvestSetSpec = ?, " +
					"serviceType = ?, " +
					"lastHarvestDate = ?, " +
					"alwaysEverything = ?" +
					"where serviceId = ?");
			int i = 0;
			pst.setString(++i, service.getName());
			pst.setString(++i, service.getCronString());
			pst.setString(++i, service.getHarvestURL());
			pst.setString(++i, service.getHarvestSetSpec());
			pst.setString(++i, service.getServiceType());
			Timestamp ts = service.getLastHarvestDate() == null ? null : new Timestamp(service.getLastHarvestDate().getTime());
			pst.setTimestamp(++i, ts);
			pst.setBoolean(++i, service.getAlwaysHarvestEverything());
	
			pst.setString(++i, service.getId());
			pst.executeUpdate();
			DBBasedManagerImpl.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Uppdaterade tjänst med id " + service.getId());
			}
			String dbCron = dbService.getCronString() + "";
			String newCron = service.getCronString() + "";
			String dbType = dbService.getServiceType();
			String newType = service.getServiceType();
			// bara om de diffar
			if (!dbCron.equals(newCron) || !dbType.equals(newType)) {
				if (logger.isInfoEnabled()) {
					logger.info("Cronsträng eller typ ändrad för tjänst med id " + service.getId());
				}
				try {
					unScheduleJob(service.getId());
				} catch (Exception e) {
					logger.warn("Problem att avschedulera jobb för tjänst med id = " +
							service.getId(), e);
				}
				try {
					scheduleJob(service);
				} catch (Exception e) {
					logger.warn("Problem att schedulera jobb för tjänst med id = " +
							service.getId(), e);
				}
			}			
	    } catch (Exception e) {
	    	DBBasedManagerImpl.rollback(c);
	    	logger.error("Fel vid uppdatering av tjänst med id " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBBasedManagerImpl.closeDBResources(null, pst, c);
	    }
	}

	public void updateServiceDate(HarvestService service, Date date) throws Exception {
		HarvestService dbService = getService(service.getId());
		if (dbService == null) {
			throw new RuntimeException("Hittade inte service med id: " + service.getId());
		}
	    Connection c = null;
	    PreparedStatement  pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("update harvestservices set " +
					"lastHarvestDate = ? " +
					"where serviceId = ?");
			Timestamp ts = new Timestamp(date.getTime());
			pst.setTimestamp(1, ts);

			pst.setString(2, service.getId());
			pst.executeUpdate();
			DBBasedManagerImpl.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Uppdaterade datum för tjänst med id " + service.getId());
			}
	    } catch (Exception e) {
	    	DBBasedManagerImpl.rollback(c);
	    	logger.error("Fel vid uppdatering av datum för tjänst med id " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBBasedManagerImpl.closeDBResources(null, pst, c);
	    }
	}

	public void deleteService(HarvestService service) throws Exception {
	    Connection c = null;
	    PreparedStatement pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("delete from harvestservices where serviceId = ?");
			pst.setString(1, service.getId());
			pst.executeUpdate();
			DBBasedManagerImpl.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Tog bort tjänst med id: " + service.getId());
			}
			try {
				unScheduleJob(service.getId());
			} catch (Exception e) {
				logger.warn("Problem att avschedulera jobb för tjänst med id = " +
						service.getId(), e);
			}
			// rensa data i repo (rdf + ev spatialt data)
			hrm.deleteData(service);
	    } catch (Exception e) {
	    	DBBasedManagerImpl.rollback(c);
	    	logger.error("Fel vid borttagning av tjänst med id " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBBasedManagerImpl.closeDBResources(null, pst, c);
	    }
	}

	private void scheduleJob(HarvestService service) throws Exception {
		scheduleJob(service, JOBGROUP_HARVESTERS);
	}

	private void scheduleJob(HarvestService service, String jobGroup) throws Exception {
		if (service.getCronString() != null) {
			JobDetail jd = createJobDetail(service);
			Trigger t = new CronTrigger(service.getId() + TRIGGER_SUFFIX, null,
					service.getCronString());
			scheduler.scheduleJob(jd, t);
		}
	}
	private void unScheduleJob(String serviceId) throws SchedulerException  {
		scheduler.deleteJob(serviceId, JOBGROUP_HARVESTERS);
	}

	private void triggerJob(String serviceId) throws SchedulerException  {
		scheduler.triggerJob(serviceId, JOBGROUP_HARVESTERS);
	}

	private void triggerJob(HarvestService service, Step step) throws SchedulerException  {
		ss.setStartStep(service, step);
		scheduler.triggerJob(service.getId(), JOBGROUP_HARVESTERS);
	}

	private boolean interruptJob(String serviceId) throws SchedulerException  {
		return scheduler.interrupt(serviceId, JOBGROUP_HARVESTERS);
	}

	@SuppressWarnings("unchecked")
	public String getJobStatus(HarvestService service) {
		CronTrigger t = null;
		if (!SERVICE_LUCENE_REINDEX.equals(service.getId())) {
			JobDetail jd = null;
			try {
				jd = scheduler.getJobDetail(service.getId(), JOBGROUP_HARVESTERS);
			} catch (Exception ignore) {}
			if (jd == null) {
				return "Jobb saknas! cronsträng ok?";
			}
			try {
				t = (CronTrigger) scheduler.getTrigger(service.getId() + TRIGGER_SUFFIX, null);
			} catch (Exception ignore) {}
			if (t == null) {
				return "Ej schedulerat";
			}
		}
		// hämta info om ev fel vid senaste körning
		String err = ss.getErrorText(service);
		if (err != null) {
			return err;
		}
		String status = null;
		String isRunning = "";
		try {
			List<JobExecutionContext> running = scheduler.getCurrentlyExecutingJobs();
			for (JobExecutionContext jce: running) {
				if (service.getId().equals(jce.getJobDetail().getName())) {
					isRunning = "Kör sen " + ContentHelper.formatDate(jce.getFireTime(), true) + " - ";
					break;
				}
			}
		} catch (SchedulerException e) {
			logger.warn("Fel vid hämtning av körande jobb", e);
		}
		if (t != null && !t.getCronExpression().equals(service.getCronString())) {
			return isRunning + "Ej samma körschema!";
		}
		status = ss.getStatusText(service);
		if (status == null) {
			// okänd status, ej kört ännu tex
			status = "Ok";
		}
		return isRunning + status;
	}

	public List<String> getJobLog(HarvestService service) {
		return ss.getStatusLog(service);
	}

	public List<String> getJobLogHistory(HarvestService service) {
		return ss.getStatusLogHistory(service);
	}

	public Step getJobStep(HarvestService service) {
		return ss.getStep(service);
	}

	// hjälpmetod som kollar om ett jobb körs
	@SuppressWarnings("unchecked")
	private boolean isJobRunning(String serviceId) {
		boolean isRunning = false;
		try {
			List<JobExecutionContext> running = scheduler.getCurrentlyExecutingJobs();
			for (JobExecutionContext jce: running) {
				if (serviceId.equals(jce.getJobDetail().getName())) {
					isRunning = true;
					break;
				}
			}
		} catch (SchedulerException e) {
			logger.warn("Fel vid hämtning av körande jobb", e);
		}
		return isRunning;
	}

	// skapar en instans av JobDetail med rätt jobbklass beroende på tjänstetyp
	private JobDetail createJobDetail(HarvestService service) {
		String type = service.getServiceType();
		Class<? extends HarvestJob> clazz = null;
		String jobGroup = JOBGROUP_HARVESTERS;
		if (type == null || "OAI-PMH".equalsIgnoreCase(type)) {
			clazz = OAIPMHHarvestJob.class;
		} else if ("SIMPLE".equalsIgnoreCase(type)) {
			clazz = SimpleHarvestJob.class;
		} else if ("SIMPLE-SAMSOK".equalsIgnoreCase(type)) {
			clazz = SamsokSimpleHarvestJob.class;
		} else if ("OAI-PMH-SAMSOK".equalsIgnoreCase(type)) {
			clazz = SamsokOAIPMHHarvestJob.class;
		} else if ("_LUCENE_OPTIMIZE".equalsIgnoreCase(type)) {
			clazz = LuceneOptimizeJob.class;
			//jobGroup = JOBGROUP_LUCENE;
		} else {
			logger.error("Kunde inte skapa jobdetail för " + service);
			return null;
		}
		return new JobDetail(service.getId(), jobGroup, clazz);
	}
}
