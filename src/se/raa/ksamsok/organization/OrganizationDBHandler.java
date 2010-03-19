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

/**
 * Klass för att hantera databas graj för att modda organisationers
 * information
 * @author Henrik Hjalmarsson
 */
public class OrganizationDBHandler extends DBBasedManagerImpl
{

	/**
	 * Skapar en ny databashanterare
	 * @param ds datakälls som skall användas
	 */
	protected OrganizationDBHandler(DataSource ds)
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
			closeDBResources(rs, pst, c);
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
			rs.next();
			setOrgValues(org, rs);
			sql = "SELECT name, beskrivning, kortnamn FROM harvestservices WHERE kortnamn= ?";
			ps = c.prepareStatement(sql);
			ps.setString(1, org.getKortNamn());
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
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeDBResources(rs, ps, c);
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
			commit(c);
		} catch (SQLException e) {
			rollback(c);
			e.printStackTrace();
		}finally {
			closeDBResources(rs, ps, c);
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
			ps.setString(8, org.getKontaktPerson());
			ps.setString(9, org.getEpostKontaktPerson());
			ps.setString(10, org.getWebsida());
			ps.setString(11, org.getWebsidaKS());
			ps.setString(12, org.getLowressUrl());
			ps.setString(13, org.getThumbnailUrl());
			ps.setString(14, org.getKortNamn());
		}catch(SQLException e) {
			e.printStackTrace();
		}
		return ps;
	}
}
