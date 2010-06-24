package se.raa.ksamsok.sitemap;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.DBUtil;

public class SitemapIndexBuilder
{
	private static final Logger logger = Logger.getLogger(SitemapIndexBuilder.class);
	private PrintWriter writer;
	private DataSource ds;
	
	public static final int BATCH_SIZE = 40000;
	
	private static final String SITEMAP_URL = "http://kulturarvsdata.se/sitemap?batch=";
	
	public SitemapIndexBuilder(PrintWriter writer, DataSource ds)
	{
		this.writer = writer;
		this.ds = ds;
	}
	
	public void writeSitemapIndex()
	{
		int numberOfBatches = getNumberOfBatches();
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
		for(int i = 1; i <= numberOfBatches; i++) {
			writeSitemapTag(i);
		}
		writer.println("</sitemapindex>");
	}
	
	private void writeSitemapTag(int batchNumber)
	{
		writer.println("<sitemap>");
		writer.println("<loc>" + SITEMAP_URL + batchNumber + "</loc>");
		writer.println("</sitemap>");
	}
	
	private int getNumberOfBatches()
	{
		int numberOfBatches = 0;
		int recordCount = getRecordCount();
		if(recordCount % BATCH_SIZE == 0) {
			numberOfBatches = recordCount / BATCH_SIZE;
		}else {
			numberOfBatches = (recordCount / BATCH_SIZE) + 1;
		}
		return numberOfBatches;
	}
	
	private int getRecordCount()
	{
		int recordCount = 0;
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "select count(*) from content";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			if(rs.next()) {
				recordCount = rs.getInt(1);
			}
		}catch(SQLException e) {
			logger.error("Error when getting record count");
			e.printStackTrace();
		}finally
		{
			DBUtil.closeDBResources(rs, ps, c);
		}
		return recordCount;
	}
}
