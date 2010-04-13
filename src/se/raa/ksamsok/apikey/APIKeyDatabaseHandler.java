package se.raa.ksamsok.apikey;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.sql.DataSource;

import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;
import se.raa.ksamsok.statistic.APIKey;

public class APIKeyDatabaseHandler extends DBBasedManagerImpl
{
	public APIKeyDatabaseHandler(DataSource ds)
	{
		super(ds);
	}
	
	public List<APIKey> getAPIKeys()
	{
		List<APIKey> APIKeys = new Vector<APIKey>();
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
				APIKeys.add(key);
			}
		} catch (SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return APIKeys;
	}
	
	public void removeAPIKeys(String APIKey)
	{
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "DELETE FROM apikeys WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, APIKey);
			ps.executeUpdate();
			sql = "DELETE FROM searches WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, APIKey);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	public void addNewAPIKey(String APIKey, String owner)
	{
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "INSERT INTO apikeys(apikey, owner, total) VALUES(?,?,0)";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, APIKey);
			ps.setString(++i, owner);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
}
