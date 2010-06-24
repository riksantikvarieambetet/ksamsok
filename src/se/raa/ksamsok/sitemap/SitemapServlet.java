package se.raa.ksamsok.sitemap;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class SitemapServlet extends HttpServlet
{
	private static final long serialVersionUID = -2449533768187277307L;
	
	private static final String DATASOURCE_NAME = "harvestdb";
	private static DataSource ds = null;
	
	private static final String BATCH_PARAMETER_NAME = "batch";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		String batch = req.getParameter(BATCH_PARAMETER_NAME);
		PrintWriter writer = resp.getWriter();
		if(batch != null) {
			int batchNumber = Integer.parseInt(batch);
			SitemapBuilder sitemapBuilder = new SitemapBuilder(writer, ds, batchNumber);
			sitemapBuilder.writeSitemap();
		}else {
			SitemapIndexBuilder sitemapIndexBuilder = new SitemapIndexBuilder(writer, ds);
			sitemapIndexBuilder.writeSitemapIndex();
		}
	}

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}

	
}
