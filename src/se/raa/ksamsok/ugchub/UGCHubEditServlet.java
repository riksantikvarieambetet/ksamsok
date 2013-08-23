package se.raa.ksamsok.ugchub;

import java.io.IOException;
import java.util.List;

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

import se.raa.ksamsok.ugchub.UGCHubManager;

/**
 * Servlet implementation class UGCHubEditServlet
 */
@WebServlet("/UGCHubEditServlet")
public class UGCHubEditServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
  
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
		String pathInfo = req.getServletPath();
		req.setAttribute("pathInfo", pathInfo);
		
		if (StringUtils.contains(pathInfo, "editugc")) {
			view = req.getRequestDispatcher("editUgc.jsp");
			UGCHub ugcHub = new UGCHub();
			ugcHub.setId(Integer.parseInt(id));
			ugcHub.setObjectUri("some uri");
			ugcHub.setCreateDate("2013-08-22");
			ugcHub.setUserName("Johan");
			ugcHub.setApplicationName("Kringla");
			ugcHub.setTag("MyTextTag");
			ugcHub.setCoordinate("32.754, 41.654");
			ugcHub.setComment("Jo då så att");
			ugcHub.setRelatedUri("some url");
			ugcHub.setRelationType("sameAs");
			ugcHub.setImageUrl("image url");
			//TODO !! get one record from UGC-hub, requires implementations of an API-method in UGC-hub.
			req.setAttribute("ugchub", ugcHub);
		} else if (StringUtils.contains(pathInfo, "deleteugc")) {
			view = req.getRequestDispatcher("deleteUgc.jsp");
		}
				
		
//		try {
//			req.setAttribute("ugcHubs", ugcHubManager.getUGCHubsPaginate(String.valueOf(limit), String.valueOf(offset)));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		view.forward(req, resp);
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
//		int limit = ugcHubManager.getLimit();
//		int offset = Integer.parseInt(req.getParameter("pageNumber") != null ? req.getParameter("pageNumber") : "1");
//		req.setAttribute("pageNumber", offset);
//		offset = (offset - 1) * limit; 
//		String tot = "";
//		HttpSession session = req.getSession();
//		
//		if (session.getAttribute("numberOfRelations") == null) {
//			try {
//				int count = ugcHubManager.getCount();
//				tot = String.valueOf((int) Math.ceil(((double)count)/(double)limit));
//				session.setAttribute("tot", Integer.parseInt(tot));
//			} catch (Exception e) {				
//				e.printStackTrace();
//			}
//		}
		
		RequestDispatcher view = req.getRequestDispatcher("editUgc.jsp");
		
//		try {
//			req.setAttribute("ugcHubs", ugcHubManager.getUGCHubsPaginate(String.valueOf(limit), String.valueOf(offset)));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		view.forward(req, resp);
	}
	
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
//		int limit = ugcHubManager.getLimit();
//		int offset = Integer.parseInt(req.getParameter("pageNumber") != null ? req.getParameter("pageNumber") : "1");
//		req.setAttribute("pageNumber", offset);
//		offset = (offset - 1) * limit; 
//		String tot = "";
//		HttpSession session = req.getSession();
//		
//		if (session.getAttribute("numberOfRelations") == null) {
//			try {
//				int count = ugcHubManager.getCount();
//				tot = String.valueOf((int) Math.ceil(((double)count)/(double)limit));
//				session.setAttribute("tot", Integer.parseInt(tot));
//			} catch (Exception e) {				
//				e.printStackTrace();
//			}
//		}
		
		RequestDispatcher view = req.getRequestDispatcher("editUgc.jsp");
		req.setAttribute("ugcObject", "has id bla bla");
//		try {
//			req.setAttribute("ugcHubs", ugcHubManager.getUGCHubsPaginate(String.valueOf(limit), String.valueOf(offset)));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		view.forward(req, resp);
	}

}