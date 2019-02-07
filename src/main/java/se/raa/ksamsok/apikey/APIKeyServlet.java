package se.raa.ksamsok.apikey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import se.raa.ksamsok.api.util.StaticMethods;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class APIKeyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@Autowired
	private APIKeyManager keyManager;

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
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		RequestDispatcher view = req.getRequestDispatcher("APIKeyAdmin.jsp");
		String operation = req.getParameter("operation");
		if (operation != null) {
			if (operation.equals("remove")) {
				String apiKey = req.getParameter("APIKey");
				if (apiKey != null)	{
					keyManager.removeAPIKeys(apiKey);
				}
			} else if (operation.equals("insert")) {
				String apiKey = StaticMethods.getParam(req.getParameter("APIKey"));
				String owner = StaticMethods.getParam(req.getParameter("owner"));
				if (apiKey != null && owner != null) {
					keyManager.addNewAPIKey(apiKey, owner);
				}
			}
		}
		req.setAttribute("APIKeys", keyManager.getAPIKeys());
		view.forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
}
















