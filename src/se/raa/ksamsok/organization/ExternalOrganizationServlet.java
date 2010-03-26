package se.raa.ksamsok.organization;

import java.io.IOException;

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

/**
 * Hanterar GUI för Institutioner som vill ändra information om sin organisation och de 
 * tjänster de levererar till K-samsök
 * @author Henrik Hjalmarsson
 */
public class ExternalOrganizationServlet extends HttpServlet
{
	private static final long serialVersionUID = -6524358510332198460L;
	
	private DataSource ds = null;
	private OrganizationDatabaseHandler organizationDatabaseHandler;
	
	static final String DATASOURCE_NAME = "harvestdb";
	
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

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		resp.setCharacterEncoding("UTF-8");
		req.setCharacterEncoding("UTF-8");
		String operation = req.getParameter("operation");
		RequestDispatcher view = req.getRequestDispatcher("userAdmin.jsp");
		if(operation != null) {
			if(operation.equals("authenticate")) {
				String username = req.getParameter("username");
				String password = req.getParameter("password");
				if(organizationDatabaseHandler.Authenticate(username, password)) {
					req.getSession().setAttribute("isAuthenticated", "j");
					req.setAttribute("orgData", organizationDatabaseHandler.getOrganization(username));
				}else {
					req.getSession().setAttribute("isAuthenticated", "e");
				}
			}else if(operation.equals("logout")) {
				req.getSession().removeAttribute("isAuthenticated");
			}else if(operation.equals("update")) {
				Organization org = OrganizationServlet.getOrganizationValues(req);
				organizationDatabaseHandler.updateOrg(org);
				req.setAttribute("orgData", organizationDatabaseHandler.getOrganization(org.getKortnamn()));
			}else if(operation.equals("unAuthenticate")) {
				req.getSession().removeAttribute("isAuthenticated");
			}
		}
		view.forward(req, resp);
	}
}
