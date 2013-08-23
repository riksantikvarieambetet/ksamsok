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
		int offset = Integer.parseInt(req.getParameter("pageNumber") != null ? req.getParameter("pageNumber") : "1");
		req.setAttribute("pageNumber", offset);
		offset = (offset - 1) * limit; 
		int tot = 0;
		HttpSession session = req.getSession();
		
		if (session.getAttribute("numberOfRelations") == null) {
			try {
				int count = ugcHubManager.getCount();
				tot = (int) Math.ceil(((double)count)/(double)limit);
				session.setAttribute("tot", tot);
			} catch (Exception e) {				
				e.printStackTrace();
			}
		}
		
		RequestDispatcher view = req.getRequestDispatcher("ugchub.jsp");
		req.setAttribute("pageDisp", ugcHubManager.getPageDisp());
		try {
			req.setAttribute("ugcHubs", ugcHubManager.getUGCHubsPaginate(String.valueOf(limit), String.valueOf(offset)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		view.forward(req, resp);
	}

}