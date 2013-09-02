package se.raa.ksamsok.ugchub;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet implementation class UGCHubServlet
 */
@WebServlet("/UGCHubServlet")
public class UGCHubServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(UGCHubServlet.class);
  
    @Autowired
	private UGCHubManager ugcHubManager;

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		int limit = ugcHubManager.getLimit();
		int offset = 1; 
		int tot = 0;
		HttpSession session = req.getSession();
		
		if (session.getAttribute("tot") == null) {
			try {
				tot = ugcHubManager.getNumOfPages();
				session.setAttribute("tot", tot);
			} catch (Exception e) {				
				e.printStackTrace();
			}
		}
		
		if (session.getAttribute("pageNumber") == null) {
			offset = Integer.parseInt((String) (session.getAttribute("pageNumber") != null ? session.getAttribute("pageNumber") : "1"));
			session.setAttribute("pageNumber", offset);
		} else {
			int sessionPageNumber = (Integer) session.getAttribute("pageNumber");
			offset = req.getParameter("pageNumber") != null ? Integer.parseInt(req.getParameter("pageNumber")) : sessionPageNumber;
			session.setAttribute("pageNumber", offset);
		}
		
		offset = (offset - 1) * limit;
		
		RequestDispatcher view = req.getRequestDispatcher("ugchub.jsp");
		req.setAttribute("numberOfPageLinks", ugcHubManager.getPageDisp());
		try {
			req.setAttribute("ugcHubs", ugcHubManager.getUGCHubsPaginate(String.valueOf(limit), String.valueOf(offset)));
		} catch (Exception e) {
			logger.info("Got an error fetching paginated UGC-objects ", e);
		}
		
		view.forward(req, resp);
	}

}