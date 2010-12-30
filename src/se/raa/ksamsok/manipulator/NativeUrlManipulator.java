package se.raa.ksamsok.manipulator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;
import se.raa.ksamsok.util.RedirectChecker;
import se.raa.ksamsok.util.StopWatch;

public class NativeUrlManipulator implements Manipulator
{
	private long totalNumberOfRecords;
	private long currentRecord;
	private DataSource ds;
	private boolean counting;
	private StopWatch stopWatch;
	private boolean closeByRequest;
	private boolean isRunning;
	private boolean manipulateAllPosts;
	
	private static final Logger logger = Logger.getLogger(NativeUrlManipulator.class);
	private static final String NAME = "Native URL extract and insert";
	
	public NativeUrlManipulator(DataSource ds)
	{
		this.ds = ds;
		totalNumberOfRecords = 0;
		currentRecord = 0;
		counting = true;
		stopWatch = new StopWatch();
		closeByRequest = false;
		isRunning = false;
		manipulateAllPosts = true;
	}
	
	@Override
	public boolean isRunning()
	{
		return isRunning;
	}

	@Override
	public void stopThread()
	{
		closeByRequest = true;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void run()
	{
		isRunning = true;
		logger.info("Running manipulate Native url");
		manipulate();
		isRunning = false;
	}
	
	public void setManipulateAllPosts(boolean manipulateAllPosts)
	{
		this.manipulateAllPosts = manipulateAllPosts;
	}
	
	private void manipulate()
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ContentHelper contentHelper = new SamsokContentHelper();
		try {
			c = ds.getConnection();
			logger.info("Getting record count");
			totalNumberOfRecords = getTotalNumberOfRecords(c);
			counting = false;
			stopWatch.start();
			logger.info("fetching data");
			String sql = getFetchDataSQL();
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			currentRecord = 1;
			logger.info("manipulating database date");
			while(rs.next()) {
				if(closeByRequest) {
					logger.info("closing nativeUrlManipulator by request");
					DBUtil.closeDBResources(rs, ps, c);
					return;
				}
				String uri = rs.getString("uri");
				String xmlContent = rs.getString("xmlData");
				if(xmlContent != null) {
					String nativeUrl = contentHelper.extractNativeURL(xmlContent);
					try {
						if(!StringUtils.containsIgnoreCase(nativeUrl, "raa.se")) {
							RedirectChecker redirectChecker = new RedirectChecker(nativeUrl);
							while(redirectChecker.isRedirected()) {
								logger.info("URL: " + nativeUrl + " is redirected to: " + redirectChecker.getRedirectString());
								redirectChecker.setURL(redirectChecker.getRedirect());
								nativeUrl = redirectChecker.getRedirectString();
								//setNativeUrlTooNull(c, uri);
								//currentRecord++;
								//continue;
							}
						}
					}catch(MalformedURLException e) {
						logger.error("The URL: " + nativeUrl + " is malformed | Object-URI: " + uri);
						setNativeUrlTooNull(c, uri);
						currentRecord++;
						continue;
					} catch (IOException e) {
						logger.error(e.getMessage());
						setNativeUrlTooNull(c, uri);
						currentRecord++;
						continue;
					}
					updateDB(c, uri, nativeUrl);
					currentRecord++;
				}
			}
			logger.info("Database updated");
		} catch(NullPointerException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
			stopWatch.stop();
		}
	}
	
	private String getFetchDataSQL()
	{
		String sql = null;
		if(manipulateAllPosts) {
			sql = "select uri, nativeUrl, xmlData from content where deleted is null";
		}else {
			sql = "SELECT uri, nativeUrl, xmlData FROM content WHERE deleted IS NULL AND nativeurl IS NULL";
		}
		return sql;
	}
	
	private void setNativeUrlTooNull(Connection c, String uri)
	{
		PreparedStatement ps = null;
		String sql = "UPDATE content SET nativeurl = NULL WHERE uri = ?";
		try {
			ps = c.prepareStatement(sql);
			ps.setString(1, uri);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			e.printStackTrace();
			DBUtil.rollback(c);
		}finally {
			DBUtil.closeDBResources(null, ps, null);
		}
	}
	
	private int getTotalNumberOfRecords(Connection c)
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;
		try {
			String sql = getRecordCountSQL();
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			if(rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, null);
		}
		return count;
	}
	
	private String getRecordCountSQL()
	{
		String sql = null;
		if(manipulateAllPosts) {
			sql = "select count(*) from content where deleted is null";
		}else {
			sql = "SELECT count(*) FROM content WHERE deleted IS NULL AND nativeurl IS NULL";
		}
		return sql;
	}

	private void updateDB(Connection c, String uri, String nativeUrl) {
		PreparedStatement ps = null;
		try {
			String sql = "update content set nativeUrl=? where uri=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, nativeUrl);
			ps.setString(2, uri);
			ps.executeUpdate();
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, null);
		}
	}
	
	@Override
	public String getStatus()
	{
		if(counting) {
			return "counting number of records.";
		}
		if(totalNumberOfRecords <= currentRecord) {
			return "done. Total time elapsed: " + stopWatch.getTimeAsString(stopWatch.getElapsedTimeSecs());
		}
		return "updated " + currentRecord + " of " + totalNumberOfRecords + " records. Elapsed time: " + stopWatch.getTimeAsString(stopWatch.getElapsedTimeSecs()) + " Estimated Remaining time: " + estimateRemainingTime();
	}
	
	private String estimateRemainingTime()
	{
		/*long elapsedTime = stopWatch.getElapsedTime();
		long MillisecPerRecord = elapsedTime / currentRecord;
		long calculatedTotalTime = MillisecPerRecord * totalNumberOfRecords;
		long estimateTimeRemaining = calculatedTotalTime - elapsedTime;
		String timeText = stopWatch.getTimeAsString(estimateTimeRemaining / 1000);
		return timeText;*/
		if(currentRecord != 0) {
			long elapsedTime = stopWatch.getElapsedTime();
			long millisecPerRecord = elapsedTime / currentRecord;
			long recordsRemaining = totalNumberOfRecords - currentRecord;
			long estimatedTimeRemaining = recordsRemaining * millisecPerRecord;
			String timeText = stopWatch.getTimeAsString(estimatedTimeRemaining / 1000);
			return timeText;
		}else{
			return "waiting";
		}
	}
	
}