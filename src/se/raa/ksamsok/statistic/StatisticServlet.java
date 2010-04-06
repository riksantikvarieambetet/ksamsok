package se.raa.ksamsok.statistic;

import java.io.IOException;
import java.util.List;

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

import org.apache.log4j.Logger;

/**
 * Servlet som hanterar anrop från Admin gränssnittet som hanterar visning av statistik
 * @author Henrik Hjalmarsson
 */
public class StatisticServlet extends HttpServlet
{
	private static final long serialVersionUID = -1584680246222508094L;
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.statistic.StatisticServlet");
	private static final String DATASOURCE_NAME = "harvestdb";
	
	private DataSource ds = null;
	private StatisticDatabaseHandler statisticDatabaseHandler = null;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			statisticDatabaseHandler = new StatisticDatabaseHandler(ds);
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
		RequestDispatcher view = req.getRequestDispatcher("statistic.jsp");
		String operation = req.getParameter("operation");
		if(operation != null) {
			if(operation.equals("show")) {
				String APIKey = req.getParameter("APIKey");
				String sortBy = req.getParameter("sortBy");
				String sortConf = req.getParameter("sortConf");
				if(logger.isDebugEnabled()) {
					logger.debug("APIKey:" + APIKey + " sortBy:" + sortBy + " sortConf:" + sortConf);
				}
				List<Statistic> statisticData = statisticDatabaseHandler.getStatistic(APIKey, sortBy, sortConf);
				req.setAttribute("statisticData", statisticData);
			}
		}
		req.setAttribute("apikeys", statisticDatabaseHandler.getOverviewStatistics());
		view.forward(req, resp);
	}
}












