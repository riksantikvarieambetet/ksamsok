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

import se.raa.ksamsok.harvest.DBBasedManagerImpl;

/**
 * Tråd som ligger o körs i bakgrunden. Detta för att sökningar skall kunna komma in och 
 * loggas utan att det stannar upp själva sökningen medans.
 * @author Henrik Hjalmarsson
 */
public class StatisticLogger implements Runnable
{
	private static Queue<StatisticLoggData> loggInfo;
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
		return loggInfo.poll();
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
			Connection c = null;
			PreparedStatement ps = null;
			try {
				c = ds.getConnection();
				
				if(checkIfLoggEntryExists(ps, data, c)) {
					updateStatistic(ps, c, data);
				}else {
					insertStatistic(ps, c, data);
				}
				DBBasedManagerImpl.commit(c);
			} catch (SQLException e) {
				DBBasedManagerImpl.rollback(c);
				e.printStackTrace();
			}finally {
				DBBasedManagerImpl.closeDBResources(null, ps, c);
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
	private void insertStatistic(PreparedStatement ps, Connection c, StatisticLoggData data)
		throws SQLException
	{
		if(logger.isDebugEnabled()) {
			logger.debug("Inserting new Database row");
		}
		String sql = "INSERT INTO searches(apikey, searchstring, param, count) VALUES(?, ?, ?, ?)";
		ps = c.prepareStatement(sql);
		int i = 0;
		ps.setString(++i, data.getAPIKey());
		ps.setString(++i, data.getQueryString());
		ps.setString(++i, data.getParam());
		ps.setInt(++i, 1);
		ps.executeUpdate();
	}
	
	/**
	 * Uppdaterar data i databasen då en sökning som gjorts förut skall loggas
	 * @param ps
	 * @param c
	 * @param data
	 * @throws SQLException
	 */
	private void updateStatistic(PreparedStatement ps, Connection c, StatisticLoggData data)
		throws SQLException
	{
		if(logger.isDebugEnabled()) {
			logger.debug("Updating database row");
		}
		String sql = "UPDATE searches SET count=count+1 WHERE apikey=? AND param=? AND searchstring=?";
		ps = c.prepareStatement(sql);
		int i = 0;
		ps.setString(++i, data.getAPIKey());
		ps.setString(++i, data.getParam());
		ps.setString(++i, data.getQueryString());
		ps.executeUpdate();
	}
	
	/**
	 * Kollar om sökning har loggats tidigare
	 * @param ps
	 * @param data
	 * @param c
	 * @return True om identisk sökning finns loggad
	 * @throws SQLException
	 */
	private boolean checkIfLoggEntryExists(PreparedStatement ps, StatisticLoggData data, Connection c)
		throws SQLException
	{
		String sql = "SELECT param, searchstring, apikey FROM searches WHERE apikey=? AND param=? AND searchstring=?";
		ps = c.prepareStatement(sql);
		int i = 0;
		ps.setString(++i, data.getAPIKey());
		ps.setString(++i, data.getParam());
		ps.setString(++i, data.getQueryString());
		ResultSet rs = ps.executeQuery();
		boolean exists = rs.next();
		DBBasedManagerImpl.closeDBResources(rs, null, null);
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
			loggInfo.add(data);
		}
	}
}