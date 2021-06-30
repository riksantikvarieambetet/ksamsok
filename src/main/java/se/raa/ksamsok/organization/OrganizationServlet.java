package se.raa.ksamsok.organization;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Servlet som hanterar uppdateringar och visningar av information om
 * organisationer anslutna till k-samsök
 * @author Henrik Hjalmarsson
 */
public class OrganizationServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private static final Logger logger = LogManager.getLogger(OrganizationServlet.class);

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
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		RequestDispatcher view = req.getRequestDispatcher("serviceOrganizationAdmin.jsp");
		String operation = req.getParameter("operation");
		if (operation != null) {
			switch (operation) {
				case "passwordAdmin":
					req.setAttribute("passwords", organizationManager.getPasswords());
					view = req.getRequestDispatcher("passwordAdmin.jsp");
					break;
				case "addOrg": {
					String kortnamn = req.getParameter("kortnamn");
					String namnSwe = req.getParameter("namnSwe");
					logger.debug("added: " + kortnamn + " : " + namnSwe);
					organizationManager.addOrganization(kortnamn, namnSwe);
					break;
				}
				case "orgChoice": {
					String kortnamn = req.getParameter("orgChoice");
					req.setAttribute("orgInfo", organizationManager.getOrganization(kortnamn, false));
					break;
				}
				case "update":
					Organization org = getOrganizationValues(req);
					organizationManager.updateOrg(org);
					req.setAttribute("orgInfo", organizationManager.getOrganization(org.getKortnamn(), false));
					break;
				case "remove": {
					String kortnamn = req.getParameter("kortnamn");
					organizationManager.removeOrganization(kortnamn);
					break;
				}
				case "updatePasswords":
					Map<String, String> passwordMap = new HashMap<>();
					String[] passwords = req.getParameterValues("passwords");
					String[] organizations = req.getParameterValues("organizations");
					for (int i = 0; i < organizations.length; i++) {
						passwordMap.put(organizations[i], passwords[i]);
					}
					organizationManager.setPassword(passwordMap);
					break;
				default:
					logger.warn("Unexpected operation " + operation + " in doPost");
			}
		}
		req.setAttribute("orgList", organizationManager.getServiceOrganizations());
		try {
			view.forward(req, resp);
		} catch (ServletException | IOException e) {
			logger.error("Can't show organizations", e);
			throw e;
		}
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
		o.setEpostKontaktPerson(req.getParameter("epostKontaktperson"));
		o.setWebsida(req.getParameter("websida"));
		o.setWebsidaKS(req.getParameter("websidaKS"));
		o.setLowressUrl(req.getParameter("lowressUrl"));
		o.setThumbnailUrl(req.getParameter("thumbnailUrl"));
		Map<String,String[]> params = req.getParameterMap();
		List<Service> serviceList = new Vector<>();
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
