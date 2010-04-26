package se.raa.ksamsok.organization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;

import se.raa.ksamsok.api.util.StaticMethods;

/**
 * Servlet som hanterar uppdateringar och visningar av information om
 * organisationer anslutna till k-sams�k
 * @author Henrik Hjalmarsson
 */
public class OrganizationServlet extends HttpServlet
{
	private static final long serialVersionUID = 4513891675396512336L;
	
	private static DataSource ds = null;
	private OrganizationDatabaseHandler organizationDatabaseHandler;
	
	private static final String DATASOURCE_NAME = "harvestdb";
	
	@Override
	public void init(ServletConfig conf) 
		throws ServletException
	{
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			organizationDatabaseHandler = new OrganizationDatabaseHandler(ds);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returnerar en DataSource
	 * @return
	 */
	public static DataSource getDataSource()
	{
		try {
			if(ds == null) {
				Context ctx = new InitialContext();
				Context envctx =  (Context) ctx.lookup("java:comp/env");
				ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			}
		}catch(NamingException e) {
			e.printStackTrace();
		}
		return ds;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		RequestDispatcher view = req.getRequestDispatcher("serviceOrganizationAdmin.jsp");
		String operation = req.getParameter("operation");
		if(operation != null) {
			if(operation.equals("passwordAdmin")) {
				req.setAttribute("passwords", organizationDatabaseHandler.getPasswords());
				view = req.getRequestDispatcher("passwordAdmin.jsp");
			}else if(operation.equals("addOrg")) {
				String kortnamn = req.getParameter("kortnamn");
				String namnSwe = req.getParameter("namnSwe");
				organizationDatabaseHandler.addOrganization(kortnamn, namnSwe);
			}else if(operation.equals("orgChoice")) {
				String kortnamn = req.getParameter("orgChoice");
				req.setAttribute("orgInfo", organizationDatabaseHandler.getOrganization(kortnamn));
			}else if(operation.equals("update")) {
				Organization org = getOrganizationValues(req);
				organizationDatabaseHandler.updateOrg(org);
				req.setAttribute("orgInfo", organizationDatabaseHandler.getOrganization(org.getKortnamn()));
			}
		}
		req.setAttribute("orgList", organizationDatabaseHandler.getServiceOrganizations());
		view.forward(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		doPost(req, resp);
	}
	
	/**
	 * Skapar och s�tter de v�rden som f�tts in fr�n request f�r
	 * en organisation som skall uppdateras
	 * @param req request objekt som inneh�ller parametrarna
	 * @return Organization b�na med de data som skall uppdateras
	 */
	@SuppressWarnings("unchecked")
	public static Organization getOrganizationValues(HttpServletRequest req)
	{
		Organization o = new Organization();
		o.setKortNamn(req.getParameter("kortnamn"));
		o.setNamnSwe(req.getParameter("namnSwe"));
		o.setNamnEng(req.getParameter("namnEng"));
		o.setBeskrivSwe(req.getParameter("beskrivSwe"));
		o.setBeskrivEng(req.getParameter("beskrivEng"));
		o.setAdress1(req.getParameter("adress1"));
		o.setAdress2(req.getParameter("adress2"));
		o.setPostadress(req.getParameter("postadress"));
		o.setKontaktPerson(req.getParameter("kontaktperson"));
		o.setEpostKontaktPerson(req.getParameter("epostKontaktperson"));
		o.setWebsida(req.getParameter("websida"));
		o.setWebsidaKS(req.getParameter("websidaKS"));
		o.setLowressUrl(req.getParameter("lowressUrl"));
		o.setThumbnailUrl(req.getParameter("thumbnailUrl"));
		Map<String,String[]> params = req.getParameterMap();
		List<Service> serviceList = new Vector<Service>();
		for(Map.Entry<String, String[]> entry : params.entrySet()) {
			if(StringUtils.startsWith(entry.getKey(), "service") && !StringUtils.endsWith(entry.getKey(), "name")) {
				Service s = new Service();
				s.setKortnamn(params.get("kortnamn")[0]);
				s.setNamn(params.get(entry.getKey() + "_name")[0]);
				s.setBeskrivning(entry.getValue()[0]);
				serviceList.add(s);
			}
		}
		o.setServiceList(serviceList);
		return o;
	}
}
