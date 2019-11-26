package se.raa.ksamsok.sitemap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.raa.ksamsok.harvest.DBUtil;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class SitemapBuilder
{
	private static final Logger logger = LogManager.getLogger(SitemapBuilder.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("sv", "SE"));
	
	private PrintWriter writer;
	private DataSource ds;
	private int batch;
	private HttpServletRequest request;
	
	public SitemapBuilder(PrintWriter writer, DataSource ds, int batch, HttpServletRequest request) 
	{
		this.writer = writer;
		this.ds = ds;
		this.batch = batch;
		this.request = request;
	}
	
	public void writeSitemap()
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int batchSize = SitemapIndexBuilder.BATCH_SIZE;
		int offset = (batch - 1) * batchSize;
		try {
			c = ds.getConnection();
			String sql = "SELECT nativeUrl, changed " +
						 "FROM content " +
						 "WHERE deleted IS NULL " + 
						 	 SitemapIndexBuilder.getFilterSitemapUrlsQuery( request ) +
						 	"LIMIT ? " +
						 	"OFFSET ?";
			
			ps = c.prepareStatement(sql);
			ps.setInt(1, batchSize);
			ps.setInt(2, offset);
			// fetch in smaller groups
			ps.setFetchSize(DBUtil.FETCH_SIZE);
			rs = ps.executeQuery();
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
			while(rs.next()) {
				String nativeUrl = rs.getString("nativeUrl");
				writer.println("<url>");
				// some urls contains " at start and end for some reason
				//TODO remove when fixed
				nativeUrl = nativeUrl.replaceAll("\"", "");
				writer.println("<loc><![CDATA[" + nativeUrl + "]]></loc>");
				String date = getDate(rs.getTimestamp("changed"));
				if(date != null) {
					writer.println("<lastmod>" + date + "</lastmod>");
				}
				writer.println("<changefreq>monthly</changefreq>");
				writer.println("</url>");
				
			}
		}catch(SQLException e) {
			logger.error(e.getMessage(), e);
		} finally {
			// make sure the db resources get closed first
			DBUtil.closeDBResources(rs, ps, c);
			// and then write the end tag and potentially risk an exception
			writer.println("</urlset>");
		}
	}
	
	private String getDate(Timestamp ts)
	{
		String date;
		try {
			date = sdf.format(ts);
		}catch(Exception e) {
			return null;
		}
		return date;
	}
}
