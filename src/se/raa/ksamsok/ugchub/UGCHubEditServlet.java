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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Servlet implementation class UGCHubEditServlet
 */
@WebServlet("/UGCHubEditServlet")
public class UGCHubEditServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(UGCHubEditServlet.class);
  
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
		Gson gson = new Gson();
		RequestDispatcher view = null;

		String id = req.getParameter("id") != null ? req.getParameter("id") : "-1";
		view = req.getRequestDispatcher("editUgc.jsp");
		UGCHub ugchub = new UGCHub();
		
			try {
				ugchub = ugcHubManager.getUGCHub(Integer.parseInt(id));
			} catch (NumberFormatException e) {
				logger.info("Could not properly format object id from parameter ", e);
			} catch (Exception e) {
				logger.info("Got an error fetching the UGC-object ", e);
			}
			
			req.setAttribute("ugchub", ugchub);
			view.forward(req, resp);
			
//		if (isAjax(req)) {
//			resp.setContentType("application/json");
//			PrintWriter writer = resp.getWriter();
//			writer.print(gson.toJson(ugchub));	
//		} else {
//			req.setAttribute("ugchub", ugchub);
//			view.forward(req, resp);
//		}
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		Gson gson = new Gson();
		String redirect = "";
		boolean update = false;
		//error map
		Map<String, String> errors = new HashMap<String, String>();
		UGCHub ugcHub = new UGCHub();
		
		String contentId = req.getParameter("id");
		String objecturi = req.getParameter("objecturi");
		String tag = req.getParameter("tag");
		String coordinate = req.getParameter("coordinate");
		String comment = req.getParameter("comment");
		String relatedto = req.getParameter("relatedto");
		String relationtype = req.getParameter("relationtype");
		String imageurl = req.getParameter("imageurl");
		
		//handle errors in input fields
		if (StringUtils.isEmpty(tag)) {
			errors.put("tag", "Du vill nog ha ett värde här");
		} else {
			errors.remove("tag");
		}
		if (StringUtils.isEmpty(coordinate)) {
			errors.put("coordinate", "Du vill nog ha ett värde här");
		} else {
			errors.remove("coordinate");
		}
		if (StringUtils.isEmpty(comment)) {
			errors.put("comment", "Du vill nog ha ett värde här");
		} else {
			errors.remove("comment");
		}
		if (StringUtils.isEmpty(relatedto)) {
			errors.put("relatedto", "Du vill nog ha ett värde här");
		} else {
			errors.remove("relatedto");
		}
		if (StringUtils.isEmpty(relationtype)) {
			errors.put("relationtype", "Du vill nog ha ett värde här");
		} else {
			errors.remove("relationtype");
		}
		if (StringUtils.isEmpty(imageurl)) {
			errors.put("imageurl", "Du vill nog ha ett värde här");
		} else {
			errors.remove("imageurl");
		}
		
		if (errors.isEmpty()) {
		    
			try {
				ugcHub = ugcHubManager.getUGCHub(Integer.parseInt(contentId));
			} catch (NumberFormatException e) {
				logger.info("Could not properly format object id from parameter during update ", e);
			} catch (Exception e) {
				logger.info("Got an error fetching the UGC-object during update ", e);
			}
			ugcHub.setId(Integer.parseInt(contentId));
			ugcHub.setObjectUri(objecturi);
			ugcHub.setTag(tag);
			ugcHub.setCoordinate(coordinate);
			ugcHub.setComment(comment);
			ugcHub.setRelatedUri(relatedto);
			ugcHub.setRelationType(relationtype);
			ugcHub.setImageUrl(imageurl);
			
			try {
				update = ugcHubManager.updateUGCHub(ugcHub);
				if (update == false) {
					errors.put("submit", "Ett fel uppstod då du sparade din uppdatering.");
				} else {
					errors.remove("submit");
				}
			} catch (Exception e) {
				logger.info("Got an error updating the UGC-object ", e);
			}
		}
		
		if (isAjax(req)) {
			resp.setContentType("application/json");
			PrintWriter writer = resp.getWriter();
			writer.print(gson.toJson(errors));	
		} else {
			if (!errors.isEmpty()) {
				redirect = resp.encodeRedirectURL(req.getContextPath() + "/admin/editUgc.jsp");
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