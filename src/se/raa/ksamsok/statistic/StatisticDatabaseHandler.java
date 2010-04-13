package se.raa.ksamsok.statistic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;

/**
 * Databashanterare som hanterar ändringar och tillägg i statistikdatabasen
 * @author Henrik Hjalmarsson
 */
public class StatisticDatabaseHandler extends DBBasedManagerImpl
{
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.statistic.StatisticDatabaseHandler");

	/**
	 * Skapar en StatisticDatabaseHandler
	 * @param ds
	 */
	public StatisticDatabaseHandler(DataSource ds)
	{
		super(ds);
	}
	
	/**
	 * Returnerar en lista med statistik matchande given API nyckel som skall sorteras efter
	 * givna sorteringsparametrar
	 * @param APIKey Specifik API nyckel eller ALL om all statistik skall hämtas
	 * @param sortBy anger efter vilken column data skall sorteras
	 * @param sortConf anger om data skall sorteras fallande (ASC) eller stigande (DESC)
	 * @return
	 */
	public List<Statistic> getStatistic(String APIKey, String sortBy, String sortConf)
	{
		List<Statistic> statistics = new Vector<Statistic>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = null;
			if(APIKey.equals("ALL")) {
				sql = "SELECT * FROM searches ORDER BY " + sortBy + " " + sortConf;
				ps = c.prepareStatement(sql);
				//ps.setString(1, sortBy);
			}else {
				sql = "SELECT * FROM searches WHERE apikey=? ORDER BY " + sortBy + " " + sortConf;
				ps = c.prepareStatement(sql);
				ps.setString(1, APIKey);
				//ps.setString(2, "searchstring");
			}
			if(logger.isDebugEnabled()) {
				logger.debug("Querying database. Query: " + sql);
			}
			rs = ps.executeQuery();
			while(rs.next()) {
				Statistic statistic = new Statistic();
				statistic.setAPIKey(rs.getString("apikey"));
				statistic.setQueryString(rs.getString("searchstring"));
				statistic.setParam(rs.getString("param"));
				statistic.setCount(rs.getInt("count"));
				statistics.add(statistic);
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return statistics;
	}
	
	/**
	 * Returnerar en lista med API nycklar och översiktlig statistik för dessa
	 * ie. totala antalet sökningar som gjorts med specifik API nyckel
	 * @return
	 */
	public List<APIKey> getOverviewStatistics()
	{
		List<APIKey> APIKeys = new Vector<APIKey>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT * FROM apikeys";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				APIKey apikey = new APIKey();
				apikey.setAPIKey(rs.getString("apikey"));
				apikey.setOwner(rs.getString("owner"));
				apikey.setTotal(rs.getString("total"));
				APIKeys.add(apikey);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return APIKeys;
	}
}









