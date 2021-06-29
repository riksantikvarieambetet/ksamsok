package se.raa.ksamsok.organization;

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

/**
 * Hanterar GUI för Institutioner som vill ändra information om sin organisation och de 
 * tjänster de levererar till K-samsök
 * @author Henrik Hjalmarsson
 */
public class ExternalOrganizationServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;

	private static final Logger logger = LogManager.getLogger(ExternalOrganizationServlet.class);

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setCharacterEncoding("UTF-8");
		req.setCharacterEncoding("UTF-8");
		String operation = req.getParameter("operation");
		RequestDispatcher view = req.getRequestDispatcher("userAdmin.jsp");
		if (operation != null) {
			switch (operation) {
				case "authenticate":
					String username = req.getParameter("username");
					String password = req.getParameter("password");
					if (organizationManager.authenticate(username, password)) {
						req.getSession().setAttribute("isAuthenticated", "j");
						req.setAttribute("orgData", organizationManager.getOrganization(username, false));
					} else {
						req.getSession().setAttribute("isAuthenticated", "e");
					}
					break;
				case "logout":
				case "unAuthenticate":
					req.getSession().removeAttribute("isAuthenticated");
					break;
				case "update":
					Organization org = OrganizationServlet.getOrganizationValues(req);
					organizationManager.updateOrg(org);
					req.setAttribute("orgData", organizationManager.getOrganization(org.getKortnamn(), false));
					break;
				default:
					logger.warn("Unexpected operation " + operation + " in doPost");
			}
		}
		try {
			view.forward(req, resp);
		} catch (ServletException | IOException e) {
			logger.error("Can't show external organizations", e);
			throw e;
		}
	}
}
