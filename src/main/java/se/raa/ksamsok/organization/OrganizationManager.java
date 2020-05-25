package se.raa.ksamsok.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.raa.ksamsok.harvest.DBBasedManagerImpl;
import se.raa.ksamsok.harvest.DBUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Klass för att hantera databas graj för att modda organisationers
 * information
 * @author Henrik Hjalmarsson
 */
public class OrganizationManager extends DBBasedManagerImpl {

	private static final Logger logger = LogManager.getLogger(OrganizationManager.class);

	/**
	 * Skapar en ny databashanterare
	 * @param ds datakälla som skall användas
	 */
	public OrganizationManager(DataSource ds) {
		super(ds);
	}
	
	/**
	 * Returnerar en mapp med de organisationer som finns i databasen
	 * @return Map med String,String. Key är kortnamn för organisation
	 * och value är det svenska namnet för organisationen
	 */
	public List<Organization> getServiceOrganizations() {
		List<Organization> list = new Vector<>();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT kortnamn, serv_org, namnSwe FROM organisation";
			c = ds.getConnection();
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				Organization o = new Organization();
				o.setKortnamn(rs.getString("kortnamn"));
				o.setServ_org(rs.getString("serv_org"));
				o.setNamnSwe(rs.getString("namnSwe"));
				list.add(o);
			}
		} catch (SQLException e) {
			logger.error("Problem getting organizations", e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return list;
	}
	
	/**
	 * Returnerar en böna innehållande de värden som finns i databasen
	 * för given organisation.
	 * @param kortnamn organisationens kortnamn
	 * @return Böna med organisations-data
	 */
	public Organization getOrganization(String kortnamn, boolean isServOrg) {
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Organization org = null;
		try {
			String sql = "SELECT * FROM organisation WHERE kortnamn=?";
			if (isServOrg) {
				sql = "SELECT * FROM organisation WHERE serv_org=?";
			}
			c = ds.getConnection();
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			rs = ps.executeQuery();
			if (rs.next()) {
				org = new Organization();
				setOrgValues(org, rs);
				DBUtil.closeDBResources(rs, ps, null);
				rs = null;
				ps = null;
				sql = "SELECT name, beskrivning, kortnamn FROM harvestservices WHERE kortnamn= ?";
				ps = c.prepareStatement(sql);
				ps.setString(1, org.getKortnamn());
				rs = ps.executeQuery();
				List<Service> serviceList = new Vector<>();
				while (rs.next()) {
					Service s = new Service();
					s.setNamn(rs.getString("name"));
					s.setBeskrivning(rs.getString("beskrivning"));
					s.setKortnamn(rs.getString("kortnamn"));
					serviceList.add(s);
				}
				org.setServiceList(serviceList);
			}
		} catch (SQLException e) {
			logger.error("Problem getting organization " + kortnamn + " (isServOrg: " + isServOrg + ")", e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return org;
	}
	
	/**
	 * Sätter värden för organisationsbönan
	 * @param org organisationsbönan
	 * @param rs ResultSet från SQL query
	 * @return Organisationsbönan med satta värden
	 */
	private Organization setOrgValues(Organization org, ResultSet rs) {
		try {
			org.setKortnamn(rs.getString("kortnamn"));
			org.setServ_org(rs.getString("serv_org"));
			org.setNamnSwe(rs.getString("namnswe"));
			org.setNamnEng(rs.getString("namneng"));
			org.setBeskrivSwe(rs.getString("beskrivswe"));
			org.setBeskrivEng(rs.getString("beskriveng"));
			org.setAdress1(rs.getString("adress1"));
			org.setAdress2(rs.getString("adress2"));
			org.setPostadress(rs.getString("postadress"));
			org.setEpostKontaktPerson(rs.getString("epostkontaktperson"));
			org.setWebsida(rs.getString("websida"));
			org.setWebsidaKS(rs.getString("websidaks"));
			org.setLowressUrl(rs.getString("lowressurl"));
			org.setThumbnailUrl(rs.getString("thumbnailurl"));
		} catch(SQLException e) {
			logger.error("Problem setting organization values", e);
		}
		return org;
	}
	
	/**
	 * Uppdaterar given organisation i databasen
	 * @param org organisationen som skall uppdateras
	 */
	public void updateOrg(Organization org) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET namnswe=?, namneng=?, beskrivswe=?, beskriveng=?, adress1=?, adress2=?, postadress=?, epostkontaktperson=?, websida=?, websidaks=?, lowressurl=?, thumbnailurl=?, serv_org=? WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			setPsStrings(ps, org);
			ps.executeUpdate();
			// stäng då ps återanvänds
			DBUtil.closeDBResources(null, ps, null);
			ps = null; 
			ps = c.prepareStatement("UPDATE harvestServices SET beskrivning=? WHERE name=?");
			List<Service> serviceList = org.getServiceList();
			for(int i = 0; serviceList != null && i < serviceList.size(); i++) {
				Service s = serviceList.get(i);
				ps.setString(1, s.getBeskrivning());
				ps.setString(2, s.getNamn());
				ps.executeUpdate();
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem updating organization " + (org != null ? org.getKortnamn() : "null"), e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	/**
	 * Sätter Strängar i SQL satsen med rätta värden
	 * @param ps PreparedStatment som innehåller query
	 * @param org organisationens uppdaterade data
	 * @return PreparedStatement med strängar satta
	 */
	private PreparedStatement setPsStrings(PreparedStatement ps, Organization org) {
		try {
			ps.setString(1, org.getNamnSwe());
			ps.setString(2, org.getNamnEng());
			ps.setString(3, org.getBeskrivSwe());
			ps.setString(4, org.getBeskrivEng());
			ps.setString(5, org.getAdress1());
			ps.setString(6, org.getAdress2());
			ps.setString(7, org.getPostadress());
			ps.setString(8, org.getEpostKontaktperson());
			ps.setString(9, org.getWebsida());
			ps.setString(10, org.getWebsidaKS());
			ps.setString(11, org.getLowressUrl());
			ps.setString(12, org.getThumbnailUrl());
			ps.setString(13, org.getServ_org());
			ps.setString(14, org.getKortnamn());
		} catch(SQLException e) {
			logger.error("Problem setting ps strings", e);
		}
		return ps;
	}
	
	/**
	 * Returnerar alla organisationer i databasen i form av en lista med organisationsbönor
	 * @return Lista med organisationer i databasen
	 */
	public List<Organization> getAllOrganizations() {
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		List<Organization> orgList = new Vector<>();
		try {
			c = ds.getConnection();
			String sql = "SELECT kortnamn FROM organisation";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				orgList.add(getOrganization(rs.getString("kortnamn"), false));
			}
		} catch (SQLException e) {
			logger.error("Problem getting all organizations: " + e.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("Problem getting all organizations", e);
			}
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return orgList;
	}
	
	/**
	 * Kollar om given användare med lösenord stämmer.
	 * @param kortnamn organisationen som skall autensieras
	 * @param password lösenordet för organisationen
	 * @return true om lösenord och kortnamn är korrekt. Annars false
	 */
	public boolean authenticate(String kortnamn, String password) {
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
			logger.error("Problem authenticating " + kortnamn, e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return authentic;
	}
	
	/**
	 * Returnerar en Map med alla organisationskortnamn och deras lösen
	 * @return Map med lösenord
	 */
	public Map<String,String> getPasswords() {
		Map<String,String> passwordMap = new HashMap<>();
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
			logger.error("Problem getting passwords", e);
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return passwordMap;
	}
	
	/**
	 * ändrar lösenord för organisationer
	 * @param passwordMap Map med användare och lösenord.
	 */
	public void setPassword(Map<String,String> passwordMap) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE organisation SET pass=? WHERE kortnamn=?";
			ps = c.prepareStatement(sql);
			for(Map.Entry<String, String> entry : passwordMap.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.executeUpdate();
			}
			DBUtil.commit(c);
		} catch (SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem setting passwords", e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
	
	/**
	 * Lägger till en ny organisation i databasen
	 * (övrig info får fixas i efterhand)
	 * @param kortnamn Kortnamnet för organisationen
	 * @param namnSwe svenska namnet för organisationen
	 */
	public void addOrganization(String kortnamn, String namnSwe) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "INSERT INTO organisation(kortnamn, serv_org, namnswe) VALUES(?, ?, ?)";
			ps = c.prepareStatement(sql);
			int i = 0;
			ps.setString(++i, kortnamn);
			ps.setString(++i, kortnamn);
			ps.setString(++i, namnSwe);
			ps.executeUpdate();
			DBUtil.commit(c);
		} catch(SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem adding organization " + kortnamn + " - " + namnSwe, e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}

	public void removeOrganization(String kortnamn) {
		Connection c = null;
		PreparedStatement ps = null;
		try {
			c = ds.getConnection();
			String sql = "delete from organisation where kortnamn=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, kortnamn);
			ps.executeUpdate();
			DBUtil.commit(c);
		} catch(SQLException e) {
			DBUtil.rollback(c);
			logger.error("Problem removing organization " + kortnamn, e);
		} finally {
			DBUtil.closeDBResources(null, ps, c);
		}
	}
}
