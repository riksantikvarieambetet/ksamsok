package se.raa.ksamsok.harvest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import se.raa.ksamsok.lucene.ContentHelper;

public class StatusServiceImpl implements StatusService {

	Map<String, String> statusTexts = Collections.synchronizedMap(new HashMap<String, String>());
	Map<String, String> errorTexts = Collections.synchronizedMap(new HashMap<String, String>());
	Map<String, List<String>> statusLogs = Collections.synchronizedMap(new HashMap<String, List<String>>());
	Map<String, String> interrupts = Collections.synchronizedMap(new HashMap<String, String>());
	Map<String, String> lastStarts = Collections.synchronizedMap(new HashMap<String, String>());
	Map<String, Step> steps = Collections.synchronizedMap(new HashMap<String, Step>());
	Map<String, Step> startSteps = Collections.synchronizedMap(new HashMap<String, Step>());
	Map<String, Boolean> rdfErrors = Collections.synchronizedMap(new HashMap<String,Boolean>());

	DataSource ds;

	StatusServiceImpl(DataSource ds) {
		this.ds = ds;
	}

	public void checkInterrupt(HarvestService service) {
		String iDate = interrupts.remove(service.getId());
		if (iDate != null) {
			throw new RuntimeException("Job aborted " + iDate + " in step " +
					steps.get(service.getId()) + " by user request");
		}
	}

	public List<String> getStatusLog(HarvestService service) {
		List<String> statusLog = statusLogs.get(service.getId());
		if (statusLog == null) {
			statusLog = Collections.emptyList();
		}
		return statusLog;
	}

	public String getStatusText(HarvestService service) {
		return statusTexts.get(service.getId());
	}

	public String getErrorText(HarvestService service) {
		return errorTexts.get(service.getId());
	}

	public String getLastStart(HarvestService service) {
		return lastStarts.get(service.getId());
	}

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

	public void requestInterrupt(HarvestService service) {
		setStatusTextAndLog(service, "* Request for abortion received");
		interrupts.put(service.getId(), ContentHelper.formatDate(new Date(), true));
	}

	public void setStatusText(HarvestService service, String message) {
		String now = ContentHelper.formatDate(new Date(), true);
		statusTexts.put(service.getId(), message + " (" + now + ")");
	}

	public void setStatusTextAndLog(HarvestService service, String message) {
		Date nowDate = new Date();
		String now = ContentHelper.formatDate(nowDate, true);
		statusTexts.put(service.getId(), message + " (" + now + ")");
		List<String> statusLog = statusLogs.get(service.getId());
		if (statusLog == null) {
			statusLog = new ArrayList<String>();
			statusLogs.put(service.getId(), statusLog);
		}
		statusLog.add(now + ": " + message);
		log2Db(service, 0, nowDate, message);
	}

	public void setErrorTextAndLog(HarvestService service, String message) {
		Date nowDate = new Date();
		String now = ContentHelper.formatDate(nowDate, true);
		errorTexts.put(service.getId(), now + ": " + message);
		List<String> statusLog = statusLogs.get(service.getId());
		if (statusLog == null) {
			statusLog = new ArrayList<String>();
			statusLogs.put(service.getId(), statusLog);
		}
		statusLog.add(now + ": *** " + message);
		log2Db(service, 1, nowDate, message);
	}

	public Step getStep(HarvestService service) {
		Step step = steps.get(service.getId());
		if (step == null) {
			step = Step.IDLE;
		}
		return step;
	}

	public void setStep(HarvestService service, Step step) {
		steps.put(service.getId(), step);
	}

	public Step getStartStep(HarvestService service) {
		Step step = startSteps.get(service.getId());
		if (step == null) {
			step = Step.FETCH;
		}
		return step;
	}

	public void setStartStep(HarvestService service, Step step) {
		startSteps.put(service.getId(), step);
	}

	public void signalRDFError(HarvestService service) {
		rdfErrors.put(service.getId(), true);
	}

	public boolean containsRDFErrors(HarvestService service) {
		Boolean containError = rdfErrors.get(service.getId());
		if (containError != null) {
			return containError;
		}
		return false;
	}

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
				statusLog = new ArrayList<String>();
				do {
					statusLog.add(ContentHelper.formatDate(rs.getTimestamp("eventTs"), true) +
							": " + (rs.getInt("eventType") != 0 ? "*** " : "") +
							rs.getString("message"));
				} while (rs.next());
			}
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).error(
					"Error when fetching old log messages for service " + service.getId(), e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		
		if (statusLog == null) {
			statusLog = Collections.emptyList();
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
			Logger.getLogger(this.getClass()).error(
					"Error when storing log messages for service " + service.getId() +
					": " + message, e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(null, pst, c);
		}
	}

	protected void cleanDb(HarvestService service, Date now) {
		Connection c = null;
		PreparedStatement pst = null;
		try {
			c = ds.getConnection();
			pst = c.prepareStatement("delete from servicelog where serviceId = ? and eventTs < ?");
			pst.setString(1, service.getId());
			pst.setTimestamp(2, new Timestamp(DateUtils.addDays(now, -14).getTime()));
			pst.executeUpdate();
			c.commit();
		} catch (Exception e) {
			Logger.getLogger(this.getClass()).error(
					"Error when purging log messages for service " + service.getId(), e);
			DBUtil.rollback(c);
		} finally {
			DBUtil.closeDBResources(null, pst, c);
		}
	}
}
