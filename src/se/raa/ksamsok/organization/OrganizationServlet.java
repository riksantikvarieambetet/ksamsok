package se.raa.ksamsok.organization;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet som hanterar uppdateringar och visningar av information om
 * organisationer anslutna till k-samsök
 * @author Henrik Hjalmarsson
 */
public class OrganizationServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private static final Logger logger = Logger.getLogger(OrganizationServlet.class);

	@Autowired
	private OrganizationManager organizationManager;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		RequestDispatcher view = req.getRequestDispatcher("serviceOrganizationAdmin.jsp");
		String operation = req.getParameter("operation");
		if (operation != null) {
			if (operation.equals("passwordAdmin")) {
				req.setAttribute("passwords", organizationManager.getPasswords());
				view = req.getRequestDispatcher("passwordAdmin.jsp");
			} else if (operation.equals("addOrg")) {
				String kortnamn = req.getParameter("kortnamn");
				String namnSwe = req.getParameter("namnSwe");
				logger.debug("added: " + kortnamn + " : " + namnSwe);
				organizationManager.addOrganization(kortnamn, namnSwe);
			} else if (operation.equals("orgChoice")) {
				String kortnamn = req.getParameter("orgChoice");
				req.setAttribute("orgInfo", organizationManager.getOrganization(kortnamn, false));
			} else if (operation.equals("update")) {
				Organization org = getOrganizationValues(req);
				organizationManager.updateOrg(org);
				req.setAttribute("orgInfo", organizationManager.getOrganization(org.getKortnamn(), false));
			} else if (operation.equals("remove")) {
				String kortnamn = req.getParameter("kortnamn");
				organizationManager.removeOrganization(kortnamn);
			} else if (operation.equals("updatePasswords")){
				Map<String, String> passwordMap = new HashMap<String, String>();
				String[] passwords = req.getParameterValues("passwords");
				String[] organizations = req.getParameterValues("organizations"); 
				for (int i = 0; i < organizations.length; i++){
					passwordMap.put(organizations[i], passwords[i]);
				}
				organizationManager.setPassword(passwordMap);
			}
		}
		req.setAttribute("orgList", organizationManager.getServiceOrganizations());
		view.forward(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	/**
	 * Skapar och sätter de värden som fåtts in från request för
	 * en organisation som skall uppdateras
	 * @param req request objekt som innehåller parametrarna
	 * @return Organization böna med de data som skall uppdateras
	 */
	@SuppressWarnings("unchecked")
	public static Organization getOrganizationValues(HttpServletRequest req) {
		Organization o = new Organization();
		o.setKortnamn(req.getParameter("kortnamn"));
		o.setServ_org(req.getParameter("serv_org"));
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
		for (Map.Entry<String, String[]> entry : params.entrySet()) {
			if (StringUtils.startsWith(entry.getKey(), "service") && !StringUtils.endsWith(entry.getKey(), "name")) {
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
