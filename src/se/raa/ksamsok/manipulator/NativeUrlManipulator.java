package se.raa.ksamsok.manipulator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;
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
		stopWatch.start();
		isRunning = true;
		if(logger.isDebugEnabled()) {
			logger.debug("Running manipulate Native url");
		}
		manipulate();
		isRunning = false;
	}
	
	private void manipulate()
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		ContentHelper contentHelper = new SamsokContentHelper();
		try {
			c = ds.getConnection();
			if(logger.isDebugEnabled()) {
				logger.debug("Getting record count");
			}
			totalNumberOfRecords = getTotalNumberOfRecords(c);
			counting = false;
			String sql = "select uri, nativeUrl, xmlData from content where deleted is null";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			if(logger.isDebugEnabled()) {
				logger.debug("fetching data");
			}
			currentRecord = 1;
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
					updateDB(c, uri, nativeUrl);
					currentRecord++;
				}
			}
		} catch(NullPointerException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
			stopWatch.stop();
		}
	}
	
	private int getTotalNumberOfRecords(Connection c)
	{
		PreparedStatement ps = null;
		ResultSet rs = null;
		int count = 0;
		try {
			String sql = "select count(*) from content where deleted is null";
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
			return "counting number of records. Elapsed time: " + stopWatch.getTimeAsString(stopWatch.getElapsedTimeSecs());
		}
		if(totalNumberOfRecords <= currentRecord) {
			return "done. Total time elapsed: " + stopWatch.getTimeAsString(stopWatch.getElapsedTimeSecs());
		}
		return "updated " + currentRecord + " of " + totalNumberOfRecords + " records. Elapsed time: " + stopWatch.getTimeAsString(stopWatch.getElapsedTimeSecs()) + " Estimated Remaining time: " + estimateRemainingTime();
	}
	
	private String estimateRemainingTime()
	{
		long elapsedTime = stopWatch.getElapsedTime();
		long MillisecPerRecord = elapsedTime / currentRecord;
		long calculatedTotalTime = MillisecPerRecord * totalNumberOfRecords;
		long estimateTimeRemaining = calculatedTotalTime - elapsedTime;
		String timeText = stopWatch.getTimeAsString(estimateTimeRemaining / 1000);
		return timeText;
	}
	
}
