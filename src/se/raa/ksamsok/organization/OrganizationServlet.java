package se.raa.ksamsok.organization;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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

/**
 * Servlet som hanterar uppdateringar och visningar av information om
 * organisationer anslutna till k-samsök
 * @author Henrik Hjalmarsson
 */
public class OrganizationServlet extends HttpServlet
{
	private static final long serialVersionUID = 4513891675396512336L;
	
	private DataSource ds = null;
	private OrganizationDBHandler odbh;
	
	static final String DATASOURCE_NAME = "harvestdb";
	
	@Override
	public void init(ServletConfig conf) 
		throws ServletException
	{
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			odbh = new OrganizationDBHandler(ds);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		resp.setCharacterEncoding("UTF-8");
		req.setCharacterEncoding("UTF-8");
		req.setAttribute("orgMap", odbh.getServiceOrganizationMap());
		String org = getParam(req.getParameter("orgChoice"));
		if(org != null) {
			req.setAttribute("orgData", odbh.getOrganization(org));
		}
		
		RequestDispatcher view = req.getRequestDispatcher("serviceOrganizationAdmin.jsp");
		view.forward(req, resp);
	}

	/**
	 * Hämtar ut parametrar med rätt teckenkodning
	 * @param param parametern som skall hämtas ut
	 * @return parametern i rätt teckenkodning
	 */
	private String getParam(String param)
	{
		try { //TODO vet inte om detta är ultimat, men det funkar ;)
			if(param != null) {
				param = URLDecoder.decode(param, "UTF-8");
				param = new String(param.getBytes("ISO-8859-1"), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return param;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		try {
			resp.setCharacterEncoding("UTF-8");
			req.setCharacterEncoding("UTF-8");
			Organization o = getOrganizationValues(req);
			odbh.updateOrg(o);
		} catch(Exception e) {
			e.printStackTrace();
		}
		doGet(req, resp);
	}
	
	/**
	 * Skapar och sätter de värden som fåtts in från request för
	 * en organisation som skall uppdateras
	 * @param req request objekt som innehåller parametrarna
	 * @return Organization böna med de data som skall uppdateras
	 */
	@SuppressWarnings("unchecked")
	private Organization getOrganizationValues(HttpServletRequest req)
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
