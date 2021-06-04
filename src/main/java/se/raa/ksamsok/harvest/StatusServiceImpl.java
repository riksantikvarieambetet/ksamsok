package se.raa.ksamsok.harvest;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.raa.ksamsok.lucene.ContentHelper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StatusServiceImpl implements StatusService {

	private static final Logger logger = LogManager.getLogger(StatusService.class);

	// gräns i antal dagar för hur länge loggmeddelanden sparas i databasen
	static final int LOG_THRESHHOLD_DAYS = 21;

	Map<String, String> statusTexts = Collections.synchronizedMap(new HashMap<>());
	Map<String, String> errorTexts = Collections.synchronizedMap(new HashMap<>());
	Map<String, List<String>> statusLogs = Collections.synchronizedMap(new HashMap<>());
	Map<String, String> interrupts = Collections.synchronizedMap(new HashMap<>());
	Map<String, String> lastStarts = Collections.synchronizedMap(new HashMap<>());
	Map<String, Step> steps = Collections.synchronizedMap(new HashMap<>());
	Map<String, Step> startSteps = Collections.synchronizedMap(new HashMap<>());
	Map<String, Boolean> rdfErrors = Collections.synchronizedMap(new HashMap<>());

	DataSource ds;

	Set<HarvestService> servicesInitializedFromDb = Collections.synchronizedSet(new HashSet<>());

	StatusServiceImpl(DataSource ds) {
		this.ds = ds;
	}

	@Override
	public void checkInterrupt(HarvestService service) {
		String iDate = interrupts.remove(service.getId());
		if (iDate != null) {
			throw new RuntimeException("Job aborted " + iDate + " in step " +
					steps.get(service.getId()) + " by user request");
		}
	}

	@Override
	public List<String> getStatusLog(HarvestService service) {
		List<String> statusLog = statusLogs.get(service.getId());
		if (statusLog == null) {
			statusLog = Collections.emptyList();
		}
		return statusLog;
	}

	@Override
	public String getStatusText(HarvestService service) {
		return statusTexts.get(service.getId());
	}

	@Override
	public String getErrorText(HarvestService service) {
		if (!servicesInitializedFromDb.contains(service)) {
			// The server has just been started, check for error messages in the database

			Connection c = null;
			PreparedStatement pst = null;
			ResultSet rs = null;
			try {
				c = ds.getConnection();
				pst = c.prepareStatement("select * from servicelog where serviceId = ? " +
						"order by eventTs desc");
				pst.setString(1, service.getId());
				rs = pst.executeQuery();
				if (rs.next()) {
					// we're only interested in the latest log entry, and only if it's type=error
					if (rs.getInt("eventType") == LogEvent.EVENT_ERROR) {
						errorTexts.put(service.getId(), rs.getString("message"));
					}
				}
			} catch (Exception e) {
				logger.error("Error when fetching old error messages for service " + service.getId(), e);
				DBUtil.rollback(c);
			} finally {
				DBUtil.closeDBResources(rs, pst, c);
			}

			servicesInitializedFromDb.add(service);
		}
		return errorTexts.get(service.getId());
	}

	@Override
	public String getLastStart(HarvestService service) {
		return lastStarts.get(service.getId());
	}

	@Override
	public void initStatus(HarvestService service, String message) {
		interrupts.remove(service.getId());
		statusTexts.remove(service.getId());
		statusLogs.remove(service.getId());
		errorTexts.remove(service.getId());
		steps.remove(service.getId());
		startSteps.remove(service.getId());
		Date nowDate = new Date();
		lastStarts.put(service.getId(), ContentHelper.formatDate(nowDate, true));
		rdfErrors.remove(service.getId());
		cleanDb(service, nowDate);
		setStatusTextAndLog(service, "------ " + message + " ------");
	}

	@Override
	public void requestInterrupt(HarvestService service) {
		setStatusTextAndLog(service, "* Request for abortion received");
		interrupts.put(service.getId(), ContentHelper.formatDate(new Date(), true));
	}

	@Override
	public void setStatusText(HarvestService service, String message) {
		String now = ContentHelper.formatDate(new Date(), true);
		statusTexts.put(service.getId(), message + " (" + now + ")");
	}

	@Override
	public void setStatusTextAndLog(HarvestService service, String message) {
		setStatusTextAndLog(service, message, LogEvent.EVENT_INFO, null);
	}

	protected void setStatusTextAndLog(HarvestService service, String message, int eventType, Date date) {
		Date nowDate = date != null ? date : new Date();
		String now = ContentHelper.formatDate(nowDate, true);
		statusTexts.put(service.getId(), message + " (" + now + ")");
		List<String> statusLog = statusLogs.computeIfAbsent(service.getId(), k -> new ArrayList<>());
		statusLog.add(now + ": " + message);
		log2Db(service, eventType, nowDate, message);
	}

	@Override
	public void setWarningTextAndLog(HarvestService service, String message) {
		setStatusTextAndLog(service, message, LogEvent.EVENT_WARNING, null);
	}

	@Override
	public void setWarningTextAndLog(HarvestService service, String message, Date date) {
		setStatusTextAndLog(service, message, LogEvent.EVENT_WARNING, date);
	}

	@Override
	public void setErrorTextAndLog(HarvestService service, String message) {
		Date nowDate = new Date();
		String now = ContentHelper.formatDate(nowDate, true);
		errorTexts.put(service.getId(), now + ": " + message);
		List<String> statusLog = statusLogs.computeIfAbsent(service.getId(), k -> new ArrayList<>());
		statusLog.add(now + ": *** " + message);
		log2Db(service, LogEvent.EVENT_ERROR, nowDate, message);
	}

	@Override
	public Step getStep(HarvestService service) {
		Step step = steps.get(service.getId());
		if (step == null) {
			step = Step.IDLE;
		}
		return step;
	}

	@Override
	public void setStep(HarvestService service, Step step) {
		steps.put(service.getId(), step);
	}

	@Override
	public Step getStartStep(HarvestService service) {
		Step step = startSteps.get(service.getId());
		if (step == null) {
			step = Step.FETCH;
		}
		return step;
	}

	@Override
	public void setStartStep(HarvestService service, Step step) {
		startSteps.put(service.getId(), step);
	}

	@Override
	public void signalRDFError(HarvestService service) {
		rdfErrors.put(service.getId(), true);
	}

	@Override
	public boolean containsRDFErrors(HarvestService service) {
		Boolean containError = rdfErrors.get(service.getId());
		if (containError != null) {
			return containError;
		}
		return false;
	}

	@Override
	public List<String> getStatusLogHistory(HarvestService service) {
		List<String> statusLog = null;
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("select * from servicelog where serviceId = ? " +
					"order by eventTs, eventId");
			pst.setString(1, service.getId());
			rs = pst.executeQuery();
			if (rs.next()) {
				statusLog = new ArrayList<>();
				do {
					statusLog.add(ContentHelper.formatDate(rs.getTimestamp("eventTs"), true) +
							": " + (rs.getInt("eventType") != LogEvent.EVENT_INFO ? "*** " : "") +
							rs.getString("message"));
				} while (rs.next());
			}
		} catch (Exception e) {
			logger.error("Error when fetching old log messages for service " + service.getId(), e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		
		if (statusLog == null) {
			statusLog = Collections.emptyList();
		}
		return statusLog;
	}

	@Override
	public List<LogEvent> getProblemLogHistory(int maxRows, String sort, String sortDir) {
		List<LogEvent> statusLog = Collections.emptyList();
		final List<String> columns = Arrays.asList("serviceId", "eventType", "eventTs", "message");
		if (!columns.contains(sort)) {
			return Collections.singletonList(new LogEvent("err", LogEvent.EVENT_ERROR, "now",
					"Bad sort column: " + sort + ", must be one of " + columns));
		}
		if (!"asc".equals(sortDir) && !"desc".equals(sortDir)) {
			return Collections.singletonList(new LogEvent("err", LogEvent.EVENT_ERROR, "now",
					"Bad sort direction: " + sortDir + ", must be one of asc, desc"));
		}
		if (maxRows <= 0) {
			return Collections.singletonList(new LogEvent("err", LogEvent.EVENT_ERROR, "now",
					"Bad maxRows: " + maxRows + ", must be > 0"));
		}
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("select * from servicelog where eventType > ? " +
					"order by " + sort + " " + sortDir + ", eventId asc");
			pst.setInt(1, LogEvent.EVENT_INFO);
			pst.setMaxRows(maxRows);
			rs = pst.executeQuery();
			if (rs.next()) {
				statusLog = new ArrayList<>();
				do {
					statusLog.add(new LogEvent(rs.getString("serviceId"),
							rs.getInt("eventType"),
							ContentHelper.formatDate(rs.getTimestamp("eventTs"), true),
							rs.getString("message")));
				} while (rs.next());
			}
		} catch (Exception e) {
			logger.error("Error when fetching old problem log messages", e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		
		return statusLog;
	}

	private void log2Db(HarvestService service, int eventType, Date now, String message) {
		Connection c = null;
		PreparedStatement pst = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("insert into servicelog " +
					"(serviceId, eventType, eventStep, eventTs, message) " +
					"values (?, ?, ?, ?, ?)");
			pst.setString(1, service.getId());
			pst.setInt(2, eventType);
			pst.setString(3, getStep(service).name());
			pst.setTimestamp(4, new Timestamp(now.getTime()));
			pst.setString(5, message);
			pst.executeUpdate();
			c.commit();
		} catch (Exception e) {
			logger.error("Error when storing log messages for service " + service.getId() +
					": " + message, e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(null, pst, c);
		}
	}

	
	/**
	 * Rensar loggmeddelanden äldre än ca {@linkplain #LOG_THRESHHOLD_DAYS}.
	 * @param service tjänst
	 * @param now datum/tid att utgå från, vanligen "nu"
	 */
	protected void cleanDb(HarvestService service, Date now) {
		Connection c = null;
		PreparedStatement pst = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("delete from servicelog where serviceId = ? and eventTs < ?");
			pst.setString(1, service.getId());
			pst.setTimestamp(2, new Timestamp(DateUtils.addDays(now, -LOG_THRESHHOLD_DAYS).getTime()));
			pst.executeUpdate();
			c.commit();
		} catch (Exception e) {
			logger.error("Error when purging log messages for service " + service.getId(), e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(null, pst, c);
		}
	}
}
