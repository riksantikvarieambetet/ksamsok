package se.raa.ksamsok.organization;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.sql.DataSource;

import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;

/**
 * Klass för att hantera databas graj för att modda organisationers
 * information
 * @author Henrik Hjalmarsson
 */
public class OrganizationDatabaseHandler extends DBBasedManagerImpl
{

	/**
	 * Skapar en ny databashanterare
	 * @param ds datakälls som skall användas
	 */
	public OrganizationDatabaseHandler(DataSource ds)
	{
		super(ds);
	}
	
	/**
	 * Returnerar en mapp med de organisationer som finns i databasen
	 * @return Map med String,String. Key är kortnamn för organisation
	 * och value är det svenska namnet för organisationen
	 */
	public Map<String,String> getServiceOrganizationMap()
	{
		Map<String,String> map = new HashMap<String,String>();
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT kortnamn, namnSwe FROM organisation";
			c = ds.getConnection();
			pst = c.prepareStatement(sql);
			rs = pst.executeQuery();
			while(rs.next()) {
				map.put(rs.getString("kortnamn"), rs.getString("namnSwe"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
		return map;
	}
	
	/**
	 * Returnerar en böna innehållande de värden som finns i databasen
	 * för given organisation.
	 * @param kortnamn organisationens kortnamn
	 * @return Böna med organisations-data
	 */
	public Organization getOrganization(String kortnamn)
	{	
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Organization org = new Organization();
		try {
			String sql = "SELECT * FROM organisation WHERE kortnamn=?";
			c = ds.getConnection();
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			rs = ps.executeQuery();
			if(rs.next()) {
				setOrgValues(org, rs);
				sql = "SELECT name, beskrivning, kortnamn FROM harvestservices WHERE kortnamn= ?";
				ps = c.prepareStatement(sql);
				ps.setString(1, org.getKortnamn());
				rs = ps.executeQuery();
				List<Service> serviceList = new Vector<Service>();
				while(rs.next()) {
					Service s = new Service();
					s.setNamn(rs.getString("name"));
					s.setBeskrivning(rs.getString("beskrivning"));
					s.setKortnamn(rs.getString("kortnamn"));
					serviceList.add(s);
				}
				org.setServiceList(serviceList);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return org;
	}
	
	/**
	 * Sätter värden för organisations bönan
	 * @param org organisations bönan
	 * @param rs ResultSet från SQL query
	 * @return Organisationsbönan med satta värden
	 */
	private Organization setOrgValues(Organization org, ResultSet rs)
	{
		try {
			org.setKortNamn(rs.getString("kortnamn"));
			org.setNamnSwe(rs.getString("namnswe"));
			org.setNamnEng(rs.getString("namneng"));
			org.setBeskrivSwe(rs.getString("beskrivswe"));
			org.setBeskrivEng(rs.getString("beskriveng"));
			org.setAdress1(rs.getString("adress1"));
			org.setAdress2(rs.getString("adress2"));
			org.setPostadress(rs.getString("postadress"));
			org.setKontaktPerson(rs.getString("kontaktperson"));
			org.setEpostKontaktPerson(rs.getString("epostkontaktperson"));
			org.setWebsida(rs.getString("websida"));
			org.setWebsidaKS(rs.getString("websidaks"));
			org.setLowressUrl(rs.getString("lowressurl"));
			org.setThumbnailUrl(rs.getString("thumbnailurl"));
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return org;
	}
	
	/**
	 * Uppdaterar given organisation i databasen
	 * @param org organisationen som skall uppdateras
	 */
	public void updateOrg(Organization org)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET namnswe=?, namneng=?, beskrivswe=?, beskriveng=?, adress1=?, adress2=?, postadress=?, kontaktperson=?, epostkontaktperson=?, websida=?, websidaks=?, lowressurl=?, thumbnailurl=? WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			setPsStrings(ps, org);
			ps.executeUpdate();
			List<Service> serviceList = org.getServiceList();
			for(int i = 0; serviceList != null && i < serviceList.size(); i++) {
				Service s = serviceList.get(i);
				sql = "UPDATE harvestServices SET beskrivning='" + s.getBeskrivning() + "' WHERE name='" + s.getNamn() + "'";
				ps = c.prepareStatement(sql);
				ps.executeUpdate();
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}
	
	/**
	 * Sätter Strängar i SQL satsen med rätta värden
	 * @param ps PreparedStatment som innehåller query
	 * @param org organisationens uppdaterade data
	 * @return PreparedStatement med strängar satta
	 */
	private PreparedStatement setPsStrings(PreparedStatement ps, Organization org)
	{
		try {
			ps.setString(1, org.getNamnSwe());
			ps.setString(2, org.getNamnEng());
			ps.setString(3, org.getBeskrivSwe());
			ps.setString(4, org.getBeskrivEng());
			ps.setString(5, org.getAdress1());
			ps.setString(6, org.getAdress2());
			ps.setString(7, org.getPostadress());
			ps.setString(8, org.getKontaktperson());
			ps.setString(9, org.getEpostKontaktperson());
			ps.setString(10, org.getWebsida());
			ps.setString(11, org.getWebsidaKS());
			ps.setString(12, org.getLowressUrl());
			ps.setString(13, org.getThumbnailUrl());
			ps.setString(14, org.getKortnamn());
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return ps;
	}
	
	/**
	 * Returnerar alla organisationer i databasen i form av en lista med organisations-bönor
	 * @return Lista med organisationer i databasen
	 */
	public List<Organization> getAllOrganizations()
	{
		List<Organization> orgList = new Vector<Organization>();
		Map<String,String> orgNameMap = getServiceOrganizationMap();
		for(String orgShortName : orgNameMap.keySet()) {
			orgList.add(getOrganization(orgShortName));
		}
		return orgList;
	}
	
	/**
	 * Kollar om given användare med lösenord stämmer.
	 * @param kortnamn organisationen som skall autensieras
	 * @param password lösenordet för organisationen
	 * @return true om lösenord och kortnamn är korrekt. Annars false
	 */
	public boolean Authenticate(String kortnamn, String password)
	{
		boolean authentic = false;
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT pass FROM organisation WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			rs = ps.executeQuery();
			if(rs.next()) {
				String storedPassword = rs.getString("pass");
				if(password.equals(storedPassword)) {
					authentic = true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return authentic;
	}
	
	/**
	 * Returnerar en Map med alla organisationskortnamn och deras lösen
	 * @return Map med lösenord
	 */
	public Map<String,String> getPasswords()
	{
		Map<String,String> passwordMap = new HashMap<String,String>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "SELECT kortnamn, pass FROM organisation";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				String kortnamn = rs.getString("kortnamn");
				String password = rs.getString("pass");
				passwordMap.put(kortnamn, password);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return passwordMap;
	}
	
	/**
	 * Ändrar lösenord för organisationer
	 * @param passwordMap Map med användare och lösenord.
	 */
	public void setPassword(Map<String,String> passwordMap)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET pass=? WHERE kortnamn=?";
			for(Map.Entry<String, String> entry : passwordMap.entrySet()) {
				ps = c.prepareStatement(sql);
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.executeUpdate();
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}
}
