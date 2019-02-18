package se.raa.ksamsok.statistic;

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
import java.util.List;

/**
 * Servlet som hanterar anrop från Admin gränssnittet som hanterar visning av statistik
 * @author Henrik Hjalmarsson
 */
public class StatisticServlet extends HttpServlet
{
	private static final long serialVersionUID = 2L;
	private static final Logger logger = LogManager.getLogger("se.raa.ksamsok.statistic.StatisticServlet");
	
	@Autowired
	private StatisticsManager statisticsManager;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
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
				List<Statistic> statisticData = statisticsManager.getStatistic(APIKey, sortBy, sortConf);
				req.setAttribute("statisticData", statisticData);
			}
		}
		req.setAttribute("apikeys", statisticsManager.getOverviewStatistics());
		view.forward(req, resp);
	}
}












