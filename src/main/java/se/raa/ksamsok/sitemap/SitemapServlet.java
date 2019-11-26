package se.raa.ksamsok.sitemap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;

public class SitemapServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	
	@Autowired
	@Qualifier("dataSourceReader")
	private DataSource ds;

	private static final String BATCH_PARAMETER_NAME = "batch";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		String batch = req.getParameter(BATCH_PARAMETER_NAME);
		PrintWriter writer = resp.getWriter();
		if (batch != null) {
			int batchNumber = Integer.parseInt(batch);
			SitemapBuilder sitemapBuilder = new SitemapBuilder(writer, ds, batchNumber, req);
			sitemapBuilder.writeSitemap();
		} else {
			SitemapIndexBuilder sitemapIndexBuilder = new SitemapIndexBuilder(writer, ds, req);
			sitemapIndexBuilder.writeSitemapIndex();
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
	}
	
	
}
