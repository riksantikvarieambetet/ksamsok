package se.raa.ksamsok.apikey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;
import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.statistic.APIKey;

public class APIKeyManager extends DBBasedManagerImpl {

	private static final Logger logger = Logger.getLogger(APIKeyManager.class);

	// ladda om cache med detta mellanrum (lazily)
	private static final long UPDATE_INTERVAL_MILLIS = 10 * 60 * 1000; // 10 min

	// behåll en cache i minnet (obs volatile för att vara trådsäker)
	private volatile Set<String> currentApiKeys = Collections.emptySet();
	private volatile long lastUpdateTime;

	private volatile long lastFailedAt = 0;

	public APIKeyManager(DataSource ds) {
		super(ds);
	}

	public void init() {
		reloadAPIKeys();
	}

	public boolean contains(String apiKey) {
		if ((System.currentTimeMillis() - lastUpdateTime) > UPDATE_INTERVAL_MILLIS) {
			reloadAPIKeys();
		}
		// kolla mappen, men tillåt allt om vi startat utan databas (inte 100% sant, men tillräckligt bra)
		return currentApiKeys.contains(apiKey) || (lastFailedAt > 0 && currentApiKeys.size() == 0);
	}

	public List<APIKey> getAPIKeys() {
		List<APIKey> apiKeys = new Vector<APIKey>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT apikey, owner FROM apikeys";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				APIKey key = new APIKey();
				key.setAPIKey(rs.getString("apikey"));
				key.setOwner(rs.getString("owner"));
				apiKeys.add(key);
			}
		} catch (SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem getting api keyes", e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return apiKeys;
	}
	
	public void removeAPIKeys(String apiKey) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "DELETE FROM apikeys WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, apiKey);
			ps.executeUpdate();
			// stäng då variabeln återanvänds
			DBUtil.closeDBResources(null, ps, null);
			ps = null;
			sql = "DELETE FROM searches WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, apiKey);
			ps.executeUpdate();
			DBUtil.commit(c);
			// ladda om och sätt om den interna nyckelmängden
			reloadAPIKeys();
		} catch(SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem removing api key " + apiKey, e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	public void addNewAPIKey(String apiKey, String owner) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "INSERT INTO apikeys(apikey, owner, total) VALUES(?,?,0)";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, apiKey);
			ps.setString(++i, owner);
			ps.executeUpdate();
			DBUtil.commit(c);
			// ladda om och sätt om den interna nyckelmängden
			reloadAPIKeys();
		} catch(SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem adding api key " + apiKey + " (owner: " + owner + ")", e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}

	/**
	 * Uppdaterar databasen med +1 sökning från given API nyckel
	 * @param apiKey där sökningen kommer ifrån
	 */
	public void updateUsage(String apiKey) {
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE apikeys SET total=total+1 WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, apiKey);
			ps.executeUpdate();
			DBUtil.commit(c);
		} catch(Exception e) {
			//logger.error("error. Doing rollback");
			DBUtil.rollback(c);
			logger.error("Error updating usage: " + e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("Error updating usage", e);
			}
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}

	
	private void reloadAPIKeys() {
		// sätt om direkt för att hindra (många) andra att köra samtidigt
		lastUpdateTime = System.currentTimeMillis();
		Connection c  = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		HashSet<String> apiKeys = new HashSet<String>();
		try {
			c = ds.getConnection();
			String sql = "SELECT apikey FROM apikeys";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				apiKeys.add(rs.getString("apikey"));
			}
			// sätt om den interna nyckelmängden
			currentApiKeys = apiKeys;
			// nollställ misslyckande-tid
			lastFailedAt = 0;
		} catch (Exception e) {
			// sätt misslyckande-tid
			lastFailedAt = lastUpdateTime;
			logger.error("Error reloading api keys: " + e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("Error reloading api keys", e);
			}
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}

}
