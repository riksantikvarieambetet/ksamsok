package se.raa.ksamsok.harvest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.quartz.CronTrigger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HarvestServiceManagerImpl extends DBBasedManagerImpl implements HarvestServiceManager {

	private static final Logger logger = LogManager.getLogger(HarvestServiceManager.class);

	private final Object LOCK_OBJECT = new Object();

	private static final String JOBGROUP_HARVESTERS = "harvesters";
	private static final String TRIGGER_SUFFIX = "-trigger";
	private static final String OPTIMIZE_TYPE = "_LUCENE_OPTIMIZE"; // TODO: ligger i db så kan inte bara ändras till solr

	// hur ofta init-försök ska göras vid försenad init
	private static final int INIT_RETRY_TIME_MS = 60000; // 1 min

	//flagga för att sätta skördningar till pausat/inte pausat tillstånd, beroende på prod/utv installation, 
	//i utv. sätts skördning till pausad per default.
	private String appstate;
	//sätts till "true" om applikationen är i utveckling/test.
	private boolean devState = true;

	protected Scheduler scheduler;
	protected HarvestRepositoryManager hrm;
	protected StatusService ss;

	// hjälpvariabler för försenad init (db ej åtkomlig vid uppstart)
	protected volatile boolean initOk = false;
	protected Thread delayedInit;

	protected HarvestServiceManagerImpl(DataSource ds, HarvestRepositoryManager hrm,
			StatusService ss, String appstate) {
		super(ds);
		this.hrm = hrm;
		this.ss = ss;
		this.appstate = appstate;
	}

	protected boolean checkInit() {
		return initOk;
	}

	protected void init()  {
		if (logger.isInfoEnabled()) {
			logger.info("Starting HarvestServiceManager");
		}
		try {
			innerInit();
		} catch (Throwable e) {
			logger.error("Error during initial init", e);
			delayedInit = new Thread() {
				public void run() {
					logger.info("Starting delayed init thread since there were errors during init");
					while (!initOk) {
						try {
							Thread.sleep(INIT_RETRY_TIME_MS);
							logger.info("Attempting delayed init");
							innerInit();
							logger.info("Init has now been run successfully");
						} catch (InterruptedException e) {
							break;
						} catch (Throwable t) {
							logger.error("Problem doing delayed init, trying again in " +
									INIT_RETRY_TIME_MS + " millis", t);
						}
					}
					logger.info("Exiting delayed init thread");
				}
			};
			delayedInit.setDaemon(true);
			delayedInit.start();
		}
		if (logger.isInfoEnabled()) {
			logger.info("HarvestServiceManager started");
		}
	}

	protected void innerInit() throws Throwable {
		synchronized (LOCK_OBJECT) {
			if (initOk) {
				return;
			}
			Connection c = null;
			try {
				c = ds.getConnection();
				if (c != null) {
					devState = this.appstate.equals("development");
					if (devState) {
						if (logger.isInfoEnabled()) {
							logger.info("This has been determined to be a " +
									(devState ? "development" : "production") + " instance based on " +
									"the fact that the is built as a such instance.");
							logger.info("All services has been sat to paused state");
						}
					} else {
						logger.warn("We are now in production state, all services are 'live', not paused.");
					}
				}
				// hämta ut tidigt (från inner-metoder) för att få ev fel här innan scheduleraren skapats och startats
				List<HarvestService> services = innerGetServices();
				if (services == null) {
					throw new Exception("No services from innerGetServices during init, db problem?");
				}
				HarvestService indexOptimizeService = innerGetService(SERVICE_INDEX_OPTIMIZE);

				SchedulerFactory schedFact = new StdSchedulerFactory();
				scheduler = schedFact.getScheduler();
				scheduler.getContext().put(SS_KEY, ss);
				scheduler.getContext().put(HSM_KEY, this);
				scheduler.getContext().put(HRM_KEY, hrm);
				scheduler.start();

				//Om applikationen är i test/utv. sätts alla servicear till pausade.
				if (devState) {
					togglePausedForServices(devState);
				} else {
					// Om applikationen är i prod, schemaläggs alla servicar som inte är satta på paus
					for (HarvestService service: services) {
						if (!service.getPaused()) {
							scheduleJob(service);
						}
					}
					// skapa "tjänst" om den inte finns
					if (indexOptimizeService != null) {
						scheduleJob(indexOptimizeService);
					} else {
						HarvestService service = newServiceInstance();
						service.setId(SERVICE_INDEX_OPTIMIZE);
						service.setServiceType(OPTIMIZE_TYPE);
						service.setName("Index optimizer");
						service.setCronString("0 30 6 * * ? 2049");
						createService(service);
					}
				}
				
				initOk = true;
			} finally {
				DBUtil.closeDBResources(null, null, c);
			}
		}
	}

	protected void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar HarvestServiceManager");
		}
		if (delayedInit != null) {
			if (delayedInit.isAlive()) {
				delayedInit.interrupt();
			}
			delayedInit = null;
		}
		if (scheduler != null) {
			try {
				for (HarvestService service: getServices()) {
					Step s = ss.getStep(service);
					if (s != Step.IDLE) {
						if (logger.isDebugEnabled()) {
							logger.debug("Destroy, request abort of running job for " + service.getId() +
									" in step " + s);
						}
						ss.requestInterrupt(service);
					}
				}
			} catch (Exception e) {
				logger.error("Error when fetching services to abort", e);
			}
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
								logger.info("Destroy, aborting job (quartz): " + jce.getJobDetail().getKey().getName());
							}
							((InterruptableJob) j).interrupt();
						} else {
							if (logger.isInfoEnabled()) {
								logger.info("Destroy, job cannot be aborted: " + jce.getJobDetail().getKey().getName());
							}
						}
					}
					// TODO: vänta lite också?
					scheduler.shutdown(true);
				}
			} catch (SchedulerException se) {
				logger.error("Error at scheduler shutdown", se);
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("HarvestServiceManager stoppad");
		}
	}

	@Override
	public boolean isSchedulerStarted() {
		boolean result = false;
		try {
			result = scheduler != null && scheduler.isStarted();
		} catch (SchedulerException e) {
			logger.error("Problem checking scheduler running status", e);
		}
		return result;
	}

	@Override
	public void createService(HarvestService service) throws Exception {
	    Connection c = null;
	    PreparedStatement  pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("insert into harvestservices " +
					"(serviceId, name, cronstring, harvestURL, harvestSetSpec, serviceType, alwaysEverything, kortnamn, paused) values " +
					"(?, ?, ?, ?, ?, ?, ?, ?, ?)");
			int i = 0;
			pst.setString(++i, service.getId());
			pst.setString(++i, service.getName());
			pst.setString(++i, service.getCronString());
			pst.setString(++i, service.getHarvestURL());
			pst.setString(++i, service.getHarvestSetSpec());
			pst.setString(++i, service.getServiceType());
			pst.setBoolean(++i, service.getAlwaysHarvestEverything());
			pst.setString(++i, service.getShortName());
			pst.setBoolean(++i, false);

			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Created new service with ID: " + service.getId());
			}
			try {
				scheduleJob(service);
			} catch (Exception e) {
				logger.warn("Problem to schedule job for service: " + service.getId(), e);
			}
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when creating new service with ID: " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
	}

	@Override
	public HarvestService getService(String serviceId) {
		if (!checkInit()) {
			return null;
		}
		return innerGetService(serviceId);
	}

	@Override
	public JSONObject getServiceAsJSON(String serviceId) {
		if (!checkInit()) {
			return null;
		}
		
		return new JSONObject(innerGetService(serviceId));
	}

	/**
	 * Inre version av {@linkplain #getService(String)} som inte kontrollerar init-status.
	 * @param serviceId id
	 * @return tjänst eller null
	 */
	protected HarvestService innerGetService(String serviceId)  {
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
	    } catch (SQLException e) {
	    	logger.error("Problem getting service " + serviceId + ": " + e.getMessage());
	    } finally {
	    	DBUtil.closeDBResources(rs, pst, c);
	    }
	    return service;
	}

	@Override
	public List<HarvestService> getServices() {
	    if (!checkInit()) {
	    	return null;
	    }
		return innerGetServices();
	}

	/**
	 * Inre version av {@linkplain #getServices()} som inte kontrollerar init-status.
	 * @return lista med tjänster, eller null vid databasproblem
	 * @throws Exception
	 */
	protected List<HarvestService> innerGetServices()  {
	    Connection c = null;
	    PreparedStatement  pst = null;
	    ResultSet rs = null;
	    List<HarvestService> services = null;
	    try {
	    	c = ds.getConnection();
	    	pst = c.prepareStatement("select * from harvestservices where serviceId <> '" +
	    			SERVICE_INDEX_OPTIMIZE + "' order by name");
	    	rs = pst.executeQuery();
	    	services = new ArrayList<>();
	    	while (rs.next()) {
	    		HarvestService service = newServiceInstance(rs);
	    		services.add(service);
	    	}
	    } catch (SQLException e) {
			logger.error("Problem getting services " + e.getMessage()); 
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return services;
	}

	@Override
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
		service.setShortName(rs.getString("kortnamn"));
		service.setPaused(rs.getBoolean("paused"));
		Timestamp ts = rs.getTimestamp("lastHarvestDate");
		if (ts != null) {
			service.setLastHarvestDate(new Date(ts.getTime()));
		}
		ts = rs.getTimestamp("firstIndexDate");
		if (ts != null) {
			service.setFirstIndexDate(new Date(ts.getTime()));
		}
		service.setAlwaysHarvestEverything(rs.getBoolean("alwaysEverything"));
		return service;
	}

	@Override
	public void triggerHarvest(HarvestService service) throws Exception {
		triggerJob(service.getId());
	}

	@Override
	public void triggerReindex(HarvestService service) throws Exception {
		triggerJob(service, Step.INDEX);
	}

	@Override
	public void triggerRemoveindex(HarvestService service) throws Exception {
		triggerJob(service, Step.EMPTYINDEX);
	}

	@Override
	public void triggerReindexAll() throws Exception {
		JobDetail jd = JobBuilder.newJob(ReindexAllJob.class)
				.withIdentity(SERVICE_INDEX_REINDEX, JOBGROUP_HARVESTERS)
				.build();
		
		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(SERVICE_INDEX_REINDEX + TRIGGER_SUFFIX, null)
				.build();
		
		scheduler.scheduleJob(jd, trigger);
	}

	@Override
	public boolean interruptHarvest(HarvestService service) throws Exception {
		// begär att jobbet ska avbrytas, både på "logisk nivå" och på jobb-nivå
		ss.requestInterrupt(service);
		return interruptJob(service.getId());
	}

	@Override
	public boolean interruptReindexAll() throws Exception {
		HarvestService service = newServiceInstance();
		service.setId(SERVICE_INDEX_REINDEX);
		return interruptHarvest(service);
	}

	@Override
	public boolean isRunning(HarvestService service) {
		return isJobRunning(service.getId());
	}

	@Override
	public void updateService(HarvestService service) throws Exception {
		HarvestService dbService = getService(service.getId());
		if (dbService == null) {
			throw new RuntimeException("Could not find service with ID: " + service.getId());
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
					"alwaysEverything = ?, " +
					"kortnamn = ?, " +
					"paused = ?" +
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
			pst.setString(++i, service.getShortName());
			pst.setBoolean(++i, service.getPaused());
			pst.setString(++i, service.getId());
			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Updated service with ID: " + service.getId());
			}
			String dbCron = dbService.getCronString() + "";
			String newCron = service.getCronString() + "";
			String dbType = dbService.getServiceType();
			String newType = service.getServiceType();
			// bara om de diffar
			if (!dbCron.equals(newCron) || !dbType.equals(newType)) {
				if (logger.isInfoEnabled()) {
					logger.info("Cron string or type changed for service with ID: " + service.getId());
				}
				try {
					unScheduleJob(service.getId());
				} catch (Exception e) {
					logger.warn("Problem when unscheduling job for service with ID: " +
							service.getId(), e);
				}
				try {
					scheduleJob(service);
				} catch (Exception e) {
					logger.warn("Problem when scheduling job for service with ID: " +
							service.getId(), e);
				}
			}
			//Om servicen är pausad (service.getPaused() == true), ta inte med den, i den schemalagda skördningen.
			if (service.getPaused()) {
				if (logger.isInfoEnabled()) {
					logger.info("Remove from schedualed harvest, paused service with ID: " + service.getId());
				}
				try {
					unScheduleJob(service.getId());
				} catch (Exception e) {
					logger.warn("Problem when unscheduling paused job for service with ID: " +
							service.getId(), e);
				}
			} else {
				try {
					unScheduleJob(service.getId());
				} catch (Exception e) {
					logger.warn("Problem when unscheduling paused job for service with ID: " +
							service.getId(), e);
				}
				try {
					scheduleJob(service);
				} catch (Exception e) {
					logger.warn("Problem when scheduling paused job for service with ID: " +
							service.getId(), e);
				}
			}
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when updating service with ID: " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
	}
	
	@Override
	public void togglePausedForServices(boolean paused) throws Exception {
		Connection c = null;
	    PreparedStatement  pst = null;
	    
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("update harvestservices set " +
					"paused = ?");
			int i = 0;
			pst.setBoolean(++i, paused);
			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Updated paused state for services.");
			}
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when updating paused state for services.", e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
		
	}

	@Override
	public void updateServiceDate(HarvestService service, Date date) throws Exception {
		HarvestService dbService = getService(service.getId());
		if (dbService == null) {
			throw new RuntimeException("Could not find service with ID: " + service.getId());
		}
	    Connection c = null;
	    PreparedStatement pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("update harvestservices set " +
					"lastHarvestDate = ? " +
					"where serviceId = ?");
			Timestamp ts = new Timestamp(date.getTime());
			pst.setTimestamp(1, ts);
			pst.setString(2, service.getId());
			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Updated date for service with ID: " + service.getId());
			}
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when updating date for service with ID: " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
	}

	@Override
	public void storeFirstIndexDateIfNotSet(HarvestService service) throws Exception {
		HarvestService dbService = getService(service.getId());
		if (dbService == null) {
			throw new RuntimeException("Could not find service with ID: " + service.getId());
		}
		// har vi ett datum behöver vi inte göra nåt
		if (dbService.getFirstIndexDate() != null) {
			return;
		}
	    Connection c = null;
	    PreparedStatement pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("update harvestservices set " +
					"firstIndexDate = ? " +
					"where serviceId = ?");
			Timestamp ts = new Timestamp(new Date().getTime());
			pst.setTimestamp(1, ts);
			pst.setString(2, service.getId());
			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Updated first indexing date for service with ID: " +
						service.getId());
			}
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when updating först indexing date for service with ID: " +
	    			service.getId(), e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
	}

	@Override
	public void deleteService(HarvestService service) throws Exception {
	    Connection c = null;
	    PreparedStatement pst = null;
	    try {
	    	c = ds.getConnection();
			pst = c.prepareStatement("delete from harvestservices where serviceId = ?");
			pst.setString(1, service.getId());
			pst.executeUpdate();
			DBUtil.commit(c);
			if (logger.isInfoEnabled()) {
				logger.info("Removed service with ID: " + service.getId());
			}
			try {
				unScheduleJob(service.getId());
			} catch (Exception e) {
				logger.warn("Problem when unscheduling job for service with ID: " +
						service.getId(), e);
			}
			// rensa data i repo (rdf + ev spatialt data)
			hrm.deleteData(service);
	    } catch (Exception e) {
	    	DBUtil.rollback(c);
	    	logger.error("Error when removing service with ID: " + service.getId(), e);
	    	throw e;
	    } finally {
	    	DBUtil.closeDBResources(null, pst, c);
	    }
	}

	private void scheduleJob(HarvestService service) throws Exception {
		scheduleJob(service, JOBGROUP_HARVESTERS);
	}

	private void scheduleJob(HarvestService service, String jobGroup) throws Exception {
		if (service.getCronString() != null) {
			JobDetail jd = createJobDetail(service);
			String cronString = service.getCronString();
			CronTriggerImpl t = new CronTriggerImpl();
			t.setName(service.getId() + TRIGGER_SUFFIX);
			t.setGroup(jobGroup);
			t.setCronExpression(cronString);
			
			scheduler.scheduleJob(jd, t);
		}
	}
	private void unScheduleJob(String serviceId) throws SchedulerException  {
		scheduler.deleteJob(getJobDetail(serviceId).getKey());
	}

	private void triggerJob(String serviceId) throws SchedulerException  {
		scheduler.triggerJob(getJobDetail(serviceId).getKey());
	}

	private void triggerJob(HarvestService service, Step step) throws SchedulerException  {
		ss.setStartStep(service, step);
		scheduler.triggerJob(createJobDetail(service).getKey());
	}

	private boolean interruptJob(String serviceId) throws SchedulerException  {
		return scheduler.interrupt(getJobDetail(serviceId).getKey());
	}

	@Override
	public String getJobStatus(HarvestService service) {
		if (scheduler == null) {
			return null;
		}
		CronTrigger t = null;
		if (!SERVICE_INDEX_REINDEX.equals(service.getId())) {
			JobDetail jd = null;
			if (service.getPaused()) {
				return "Job is paused";
			}
			try {
				jd = scheduler.getJobDetail(createJobDetail(service).getKey());
			} catch (Exception ignore) {}
			if (jd == null) {
				return "Missing job! Is cron string correct?";
			}
			try {
				t = (CronTrigger) scheduler.getTrigger(new TriggerKey(service.getId() + TRIGGER_SUFFIX, JOBGROUP_HARVESTERS));
			} catch (Exception ignore) {}
			if (t == null) {
				return "Not scheduled";
			}
		}
		// hämta info om ev fel vid senaste körning
		String err = ss.getErrorText(service);
		if (err != null) {
			return err;
		}
		String status;
		String isRunning = "";
		try {
			List<JobExecutionContext> running = scheduler.getCurrentlyExecutingJobs();
			for (JobExecutionContext jce: running) {
				if (service.getId().equals(jce.getJobDetail().getKey().getName())) {
					isRunning = "Running since " + ContentHelper.formatDate(jce.getFireTime(), true) + " - ";
					break;
				}
			}
		} catch (SchedulerException e) {
			logger.warn("Error when fetching running job", e);
		}
		String extraInfo = "";
		if (t != null && !t.getCronExpression().equals(service.getCronString())) {
			if (service.getPaused() && !SERVICE_INDEX_OPTIMIZE.equals(service.getId())) {
				extraInfo = " (NOTE: scheduled with " + t.getCronExpression() + ") ";
			} else {
				return isRunning + "Not the same execution schema!";
			}
		}
		status = ss.getStatusText(service);
		if (status == null) {
			// okänd status, ej kört ännu tex
			status = "Ok";
		}
		return isRunning + status + extraInfo;
	}

	@Override
	public List<String> getJobLog(HarvestService service) {
		return ss.getStatusLog(service);
	}

	@Override
	public List<String> getJobLogHistory(HarvestService service) {
		return ss.getStatusLogHistory(service);
	}

	@Override
	public Step getJobStep(HarvestService service) {
		return ss.getStep(service);
	}

	// hjälpmetod som kollar om ett jobb körs
	private boolean isJobRunning(String serviceId) {
		boolean isRunning = false;
		if (scheduler != null) {
			try {
				List<JobExecutionContext> running = scheduler.getCurrentlyExecutingJobs();
				for (JobExecutionContext jce: running) {
					if (serviceId.equals(jce.getJobDetail().getKey().getName())) {
						isRunning = true;
						break;
					}
				}
			} catch (SchedulerException e) {
				logger.warn("Error when fetching running job", e);
			}
		}
		return isRunning;
	}

	// skapar en instans av JobDetail med rätt jobbklass beroende på tjänstetyp
	private JobDetail createJobDetail(HarvestService service) {
		String type = service.getServiceType();
		Class<? extends HarvestJob> clazz;
		if (type == null || "OAI-PMH".equalsIgnoreCase(type)) {
			clazz = OAIPMHHarvestJob.class;
		} else if ("SIMPLE".equalsIgnoreCase(type)) {
			clazz = SimpleHarvestJob.class;
		} else if ("SIMPLE-SAMSOK".equalsIgnoreCase(type)) {
			clazz = SamsokSimpleHarvestJob.class;
		} else if ("OAI-PMH-SAMSOK".equalsIgnoreCase(type)) {
			clazz = SamsokOAIPMHHarvestJob.class;
		} else if (OPTIMIZE_TYPE.equalsIgnoreCase(type)) {
			clazz = IndexOptimizeJob.class;
			//jobGroup = JOBGROUP_LUCENE;
		} else {
			logger.error("Could not create job detail for " + service);
			return null;
		}
		return JobBuilder.newJob(clazz)
				.withIdentity(service.getId(), JOBGROUP_HARVESTERS)
				.build();
	}
	
	// skapar en instans av JobDetail med rätt jobbklass beroende servicens id.
	private JobDetail getJobDetail(String serviceId) {
		JobDetail jd = null;
		try {
			HarvestService service = getService(serviceId);
			jd = createJobDetail(service);
		} catch (Exception e) {
			logger.error("Could not create job detail for " + e + " from serviceId: " + serviceId);
		}
		
		return jd;
	}
	
}
