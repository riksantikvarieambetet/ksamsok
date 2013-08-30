package se.raa.ksamsok.ugchub;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gson.Gson;

/**
 * Servlet implementation class UGCHubDeleteServlet
 */
@WebServlet("/UGCHubDeleteServlet")
public class UGCHubDeleteServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(UGCHubDeleteServlet.class);
  
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
		RequestDispatcher view = null;

		String id = req.getParameter("id") != null ? req.getParameter("id") : "-1";
		view = req.getRequestDispatcher("deleteUgc.jsp");
		UGCHub ugcHub = new UGCHub();
		
			try {
				ugcHub = ugcHubManager.getUGCHub(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				logger.info("Could not properly format object id from parameter during update ", e);
			} catch (Exception e) {
				logger.info("Got an error fetching the UGC-object ", e);
			}

			req.setAttribute("ugchub", ugcHub);
		
		view.forward(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		Gson gson = new Gson();
		String redirect = "";
		boolean delete = false;
		//error map
		Map<String, String> errors = new HashMap<String, String>();
		String contentId = req.getParameter("id");
		
		try {
			delete = ugcHubManager.deleteUGCHub(Integer.parseInt(contentId));
			if (delete == false) {
				errors.put("submit", "Ett fel uppstod då du tog bort relationen.");
			} else {
				errors.remove("submit");
			}
		} catch (NumberFormatException e) {
			logger.info("Could not properly format object id from parameter during delete ", e);
		} catch (Exception e) {
			logger.info("Got an error deleting the UGC-object ", e);
		}
		
		if (delete) {
			//uppdate records in session.
			int tot = 0;
			HttpSession session = req.getSession();
			
			try {
				tot = ugcHubManager.getNumOfPages();
				session.setAttribute("tot", tot);
			} catch (Exception e) {				
				logger.info("Got an error updating the number of UGC-objects ", e);
			}
		}
		
//		resp.sendRedirect("ugchub");
		if (isAjax(req)) {
			resp.setContentType("application/json");
			PrintWriter writer = resp.getWriter();
			writer.print(gson.toJson(errors));	
		} else {
			if (!errors.isEmpty()) {
				redirect = resp.encodeRedirectURL(req.getContextPath() + "/admin/deleteUgc.jsp");
				resp.sendRedirect(redirect);
			} else {
				resp.sendRedirect("ugchub");
			}
		}
	}
	
	/**
	 * check if the request is using javascript.
	 * @param req the request object.
	 * @return boolean for ajax status.
	 */
	private boolean isAjax(HttpServletRequest req) {
		return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
	}
}