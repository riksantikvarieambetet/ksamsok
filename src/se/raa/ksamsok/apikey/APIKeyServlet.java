package se.raa.ksamsok.apikey;

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

import se.raa.ksamsok.api.APIServlet;
import se.raa.ksamsok.api.util.StaticMethods;

public class APIKeyServlet extends HttpServlet
{
	private static final long serialVersionUID = 5539180093759217979L;
	
	private DataSource ds = null;
	private APIKeyDatabaseHandler dbHandler;
	
	private static final String DATASOURCE_NAME = "harvestdb";

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			dbHandler = new APIKeyDatabaseHandler(ds);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		RequestDispatcher view = req.getRequestDispatcher("APIKeyAdmin.jsp");
		String operation = req.getParameter("operation");
		if(operation != null) {
			if(operation.equals("remove")) {
				String APIKey = req.getParameter("APIKey");
				if(APIKey != null)	{
					dbHandler.removeAPIKeys(APIKey);
					APIServlet.reloadAPIKeys();
				}
			}else if(operation.equals("insert")) {
				String APIKey = StaticMethods.getParam(req.getParameter("APIKey"));
				String owner = StaticMethods.getParam(req.getParameter("owner"));
				if(APIKey != null && owner != null) {
					dbHandler.addNewAPIKey(APIKey, owner);
					APIServlet.reloadAPIKeys();
				}
			}
		}
		req.setAttribute("APIKeys", dbHandler.getAPIKeys());
		view.forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		doGet(req, resp);
	}
}
















