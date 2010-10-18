package se.raa.ksamsok.sitemap;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;

public class SitemapBuilder
{
	private static final Logger logger = Logger.getLogger(SitemapBuilder.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("sv", "SE"));
	
	private PrintWriter writer;
	private DataSource ds;
	private int batch;
	private int batchSize = SitemapIndexBuilder.BATCH_SIZE;
	
	public SitemapBuilder(PrintWriter writer, DataSource ds, int batch) 
	{
		this.writer = writer;
		this.ds = ds;
		this.batch = batch;
	}
	
	public void writeSitemap()
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int start = (batch * batchSize) - batchSize;
		int offset = batch * batchSize;
		try {
			c = ds.getConnection();
			String sql = "select nativeUrl, deleted, changed from content where idnum>=? and idnum<?";
			ps = c.prepareStatement(sql);
			ps.setInt(1, start);
			ps.setInt(2, offset);
			rs = ps.executeQuery();
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			writer.println("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
			while(rs.next()) {
				Timestamp deleted = rs.getTimestamp("deleted");
				String nativeUrl = getUri(rs.getString("nativeUrl"));
				if(deleted == null && nativeUrl != null) {
					writer.println("<url>");
					writer.println("<loc><![CDATA[" + nativeUrl + "]]></loc>");
					String date = getDate(rs.getTimestamp("changed"));
					if(date != null) {
						writer.println("<lastmod>" + date + "</lastmod>");
					}
					writer.println("<changefreq>monthly</changefreq>");
					writer.println("</url>");
				}
			}
		}catch(SQLException e) {
			logger.error(e.getMessage(), e);
		}finally{
			writer.println("</urlset>");
			DBUtil.closeDBResources(rs, ps, c);
		}
	}
	
	private String getDate(Timestamp ts)
	{
		String date = null;
		try {
			date = sdf.format(ts);
		}catch(Exception e) {
			return null;
		}
		return date;
	}
	
	private String getUri(String uri)
	{
		int index = StringUtils.lastIndexOf(uri, "/");
		uri = new StringBuffer(uri).insert(index + 1, "html/").toString();
		return uri;
	}
}
