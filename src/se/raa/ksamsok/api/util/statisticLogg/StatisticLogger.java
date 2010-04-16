package se.raa.ksamsok.api.util.statisticLogg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;

/**
 * Tråd som ligger o körs i bakgrunden. Detta för att sökningar skall kunna komma in och 
 * loggas utan att det stannar upp själva sökningen medans.
 * @author Henrik Hjalmarsson
 */
public class StatisticLogger implements Runnable
{
	private static Queue<StatisticLoggData> loggInfo;
	private static Object queueLock = new Object();
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.util.statisticLogg.StatisticLogger");
	private static final String DATASOURCE_NAME = "harvestdb";
	private static DataSource ds = null;
	
	/**
	 * Skapar en StatisticLogger
	 */
	public StatisticLogger()
	{
		logger.info("Starting Logger Thread");
		loggInfo = new LinkedList<StatisticLoggData>();
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run()
	{
		if(logger.isDebugEnabled()) {
			logger.debug("Running Logger Thread");
		}
		while(true) {
			while(hasLoggItems() && !Thread.interrupted()) {
				storeData(nextItem());
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.info("Logger Thread interrupted. Closing");
				return;
			}
		}
	}
	
	/**
	 * Kollar om det finns data som skall loggas
	 * @return
	 */
	private boolean hasLoggItems()
	{
		return !loggInfo.isEmpty();
	}
	
	/**
	 * Hämtar nästa föremål från kön med data som skall loggas
	 * @return
	 */
	private StatisticLoggData nextItem()
	{
		synchronized(queueLock) {
			return loggInfo.poll();
		}
	}
	
	/**
	 * Lagrar logg data
	 * @param data som skall loggas
	 */
	private void storeData(StatisticLoggData data)
	{
		if(data != null) {
			if(logger.isDebugEnabled()) {
				logger.debug("storing Logg Data: apikey=" + data.getAPIKey() + "; param=" + data.getParam() + "; query string=" + data.getQueryString());
			}
			if(checkIfLoggEntryExists(data)) {
				updateStatistic(data);
			}else {
				insertStatistic(data);
			}
		}
	}
	
	/**
	 * Lägger till data i databasen då en sökning som ej gjorts förut skall loggas
	 * @param ps
	 * @param c
	 * @param data
	 * @throws SQLException
	 */
	private void insertStatistic(StatisticLoggData data)
	{
		if(logger.isDebugEnabled()) {
			logger.debug("Inserting new Database row");
		}
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "INSERT INTO searches(apikey, searchstring, param, count) VALUES(?, ?, ?, ?)";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, data.getAPIKey());
			ps.setString(++i, data.getQueryString());
			ps.setString(++i, data.getParam());
			ps.setInt(++i, 1);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	/**
	 * Uppdaterar data i databasen då en sökning som gjorts förut skall loggas
	 * @param ps
	 * @param c
	 * @param data
	 * @throws SQLException
	 */
	private void updateStatistic(StatisticLoggData data)
	{
		if(logger.isDebugEnabled()) {
			logger.debug("Updating database row");
		}
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE searches SET count=count+1 WHERE apikey=? AND param=? AND searchstring=?";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, data.getAPIKey());
			ps.setString(++i, data.getParam());
			ps.setString(++i, data.getQueryString());
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	/**
	 * Kollar om sökning har loggats tidigare
	 * @param data
	 * @return True om identisk sökning finns loggad
	 * @throws SQLException
	 */
	private boolean checkIfLoggEntryExists(StatisticLoggData data)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Boolean exists = false;
		try {
			c = ds.getConnection();
			String sql = "SELECT param, searchstring, apikey FROM searches WHERE apikey=? AND param=? AND searchstring=?";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, data.getAPIKey());
			ps.setString(++i, data.getParam());
			ps.setString(++i, data.getQueryString());
			rs = ps.executeQuery();
			exists = rs.next();
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return exists;
	}
	
	/**
	 * lägger till data som skall loggas till kön som då loggas med nästa batch
	 * @param data
	 */
	public static void addToQueue(StatisticLoggData data)
	{
		if(loggInfo != null) {
			if(logger.isDebugEnabled()) {
				logger.debug("Data added to logg queue");
			}
			synchronized(queueLock) {
				loggInfo.add(data);
			}
		}
	}
}