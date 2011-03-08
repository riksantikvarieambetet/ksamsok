package se.raa.ksamsok.statistic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;

/**
 * Databashanterare som hanterar ändringar och tillägg i statistikdatabasen
 * TODO: statistikloggningen kan göras bättre med batchhämtning och verkligen återanvända prepared statements
 */
public class StatisticsManager extends DBBasedManagerImpl {
	private static final Logger logger = Logger.getLogger(StatisticsManager.class);

	// speciell instans som används för att stoppa konsumenttråd
	private static final StatisticLoggData STOP = new StatisticLoggData();

	private final BlockingQueue<StatisticLoggData> queue = new LinkedBlockingQueue<StatisticLoggData>();
	private Thread consumer;

	/**
	 * Skapar en StatisticsManager
	 * @param ds datasource
	 */
	public StatisticsManager(DataSource ds) {
		super(ds);
	}

	/**
	 * Initierar denna manager och startar en konsumenttråd för att logga till databas.
	 */
	public void init() {
		if (logger.isInfoEnabled()) {
			logger.info("Starting StatisticsManager");
		}
		consumer = new Thread(new Runnable() {
			@Override
			public void run() {
				if (logger.isInfoEnabled()) {
					logger.info("Statistics consumer thread started");
				}
				StatisticLoggData data;
				try {
					while ((data = queue.take()) != STOP) {
						storeData(data);
					}
				} catch (InterruptedException e) {
					logger.warn("Statistics consumer thread interrupted, exiting");
				}
				if (logger.isInfoEnabled()) {
					logger.info("Stopping statistics consumer thread");
				}
			}
		});
		consumer.setName("Statistics-data-consumer");
		consumer.setDaemon(true);
		consumer.start();
		if (logger.isInfoEnabled()) {
			logger.info("StatisticsManager started");
		}
	}

	/**
	 * Stänger ner och stoppar statistikkonsumenttråden.
	 */
	public void destroy() {
		if (logger.isInfoEnabled()) {
			logger.info("Stopping StatisticsManager");
		}
		queue.add(STOP);
		if (logger.isInfoEnabled()) {
			logger.info("StatisticsManager stopped");
		}
	}

	/**
	 * lägger till data som skall loggas till kön som då loggas med nästa batch
	 * @param data
	 */
	public void addToQueue(StatisticLoggData data) {
		if (consumer == null || !consumer.isAlive()) {
			logger.error("The statistics consumer thread is not running, no logging will take place");
			return;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Data added to logg queue");
		}
		queue.add(data);
	}

	/**
	 * Returnerar en lista med statistik matchande given API nyckel som skall sorteras efter
	 * givna sorteringsparametrar
	 * @param apiKey Specifik API nyckel eller ALL om all statistik skall hämtas
	 * @param sortBy anger efter vilken column data skall sorteras
	 * @param sortConf anger om data skall sorteras fallande (ASC) eller stigande (DESC)
	 * @return lista med statistik
	 */
	public List<Statistic> getStatistic(String apiKey, String sortBy, String sortConf) {
		List<Statistic> statistics = new Vector<Statistic>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = null;
			if (apiKey.equals("ALL")) {
				sql = "SELECT * FROM searches ORDER BY " + sortBy + " " + sortConf;
				ps = c.prepareStatement(sql);
				//ps.setString(1, sortBy);
			} else {
				sql = "SELECT * FROM searches WHERE apikey=? ORDER BY " + sortBy + " " + sortConf;
				ps = c.prepareStatement(sql);
				ps.setString(1, apiKey);
				//ps.setString(2, "searchstring");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Querying database. Query: " + sql);
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				Statistic statistic = new Statistic();
				statistic.setAPIKey(rs.getString("apikey"));
				statistic.setQueryString(rs.getString("searchstring"));
				statistic.setParam(rs.getString("param"));
				statistic.setCount(rs.getInt("count"));
				statistics.add(statistic);
			}
		} catch (SQLException e) {
			logger.error("When fetching statistics for apiKey " + apiKey, e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return statistics;
	}
	
	/**
	 * Returnerar en lista med API nycklar och översiktlig statistik för dessa
	 * ie. totala antalet sökningar som gjorts med specifik API nyckel
	 * @return lista med statistikdata
	 */
	public List<APIKey> getOverviewStatistics() {
		List<APIKey> apiKeys = new Vector<APIKey>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT * FROM apikeys";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				APIKey apikey = new APIKey();
				apikey.setAPIKey(rs.getString("apikey"));
				apikey.setOwner(rs.getString("owner"));
				apikey.setTotal(rs.getString("total"));
				apiKeys.add(apikey);
			}
		} catch (SQLException e) {
			logger.error("When fetching overview statistics", e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return apiKeys;
	}

	/**
	 * Lagrar logg data
	 * @param data som skall loggas
	 */
	private void storeData(StatisticLoggData data) {
		data.setAPIKey(StaticMethods.removeChar(data.getAPIKey(), '"'));
		if (data != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("storing Logg Data: apikey=" + data.getAPIKey() + "; param=" + data.getParam() + "; query string=" + data.getQueryString());
			}
			try {
				if (!updateStatistic(data)) {
					insertStatistic(data);
				}
			} catch (Exception e) {
				logger.error("Error storing statistic data: " + e.getMessage());
				if (logger.isDebugEnabled()) {
					logger.debug("When storing statistic data", e);
				}
			}
		}
	}
	
	/**
	 * Lägger till data i databasen då en sökning som ej gjorts förut skall loggas
	 * @param data data
	 * @throws SQLException
	 */
	private void insertStatistic(StatisticLoggData data) throws Exception {
		if (logger.isDebugEnabled()) {
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
		} catch (SQLException e) {
			DBUtil.rollback(c);
			throw new Exception("When inserting statistic: " + e.getMessage(), e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	/**
	 * Uppdaterar data i databasen då en sökning som gjorts förut skall loggas
	 * @param data sökdata
	 * @return sant om någon databasrad uppdaterades
	 */
	private boolean updateStatistic(StatisticLoggData data)  throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Updating database row");
		}
		boolean result = false;
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
			int updated = ps.executeUpdate();
			DBUtil.commit(c);
			result = updated > 0;
		} catch (SQLException e) {
			DBUtil.rollback(c);
			throw new Exception("When updating statistic: " + e.getMessage(), e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
		return result;
	}

}
