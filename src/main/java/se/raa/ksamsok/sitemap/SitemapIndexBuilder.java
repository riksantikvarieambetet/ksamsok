package se.raa.ksamsok.sitemap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.raa.ksamsok.harvest.DBUtil;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class SitemapIndexBuilder {
	private static final Logger logger = LogManager.getLogger(SitemapIndexBuilder.class);
	private PrintWriter writer;
	private DataSource ds;
	private HttpServletRequest request;

	public static final int BATCH_SIZE = 40000;

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("sv", "SE"));
	private static Date lastChanged = new Date();
	private static int lastRecordCount = 0;

	private static final String SITEMAP_URL = "http://kulturarvsdata.se/sitemap?batch=";

	/**
	 * You can only index domains you "own". So we only index some domains for now
	 * we can probably confim ownership of more domains later on
	 * http://www.google.com/support/webmasters/bin/answer.py?answer=75712
	 * <p>
	 * nativeUrls containing one of these will be shown in the sitemap
	 */
	private static List<String> getFilteredUrls(HttpServletRequest request) {
		List<String> result = new ArrayList<>();
		Properties p = new Properties();
		try {
			InputStream stream = new FileInputStream(
					request.getSession().getServletContext().getRealPath("/WEB-INF/sitemap.properties"));
			p.load(stream);
		} catch (IOException e) {
			logger.error("Check that sitemap.properties exists in WEB-INF", e);
		}

		String[] urlArray = ((String) p.get("sitemap.filtered.urls")).split(",");
		for (String url : urlArray) {
			result.add(url.trim());
		}
		return result;
	}

	/**
	 * Added at the end of a sqlquery to filter some domains
	 */
	public static String getFilterSitemapUrlsQuery(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		List<String> urls = getFilteredUrls(request);
		if (!CollectionUtils.isEmpty(urls)) {
			result = new StringBuilder(" AND (");
			boolean firstLoop = true;
			for (String url : urls) {
				if (!firstLoop) {
					result.append(" OR ");
				} else {
					firstLoop = false;
				}
				result.append("nativeurl LIKE '%").append(url).append("%'");
			}
			result.append(")");
		}
		return result.toString();
	}

	public SitemapIndexBuilder(PrintWriter writer, DataSource ds, HttpServletRequest request) {
		this.writer = writer;
		this.ds = ds;
		this.request = request;
	}

	public void writeSitemapIndex() {
		int numberOfBatches = getNumberOfBatches();
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
		for (int i = 1; i <= numberOfBatches; i++) {
			writeSitemapTag(i);
		}
		writer.println("</sitemapindex>");
	}

	private void writeSitemapTag(int batchNumber) {
		writer.println("<sitemap>");
		writer.println("<loc>" + SITEMAP_URL + batchNumber + "</loc>");
		writer.println("<lastmod>" + sdf.format(lastChanged) + "</lastmod>");
		writer.println("</sitemap>");
	}

	private int getNumberOfBatches() {
		int numberOfBatches;
		int recordCount = getRecordCount();

		if (recordCount != lastRecordCount) {
			lastRecordCount = recordCount;
			lastChanged = new Date();
		}

		if (recordCount % BATCH_SIZE == 0) {
			numberOfBatches = recordCount / BATCH_SIZE;
		} else {
			numberOfBatches = (recordCount / BATCH_SIZE) + 1;
		}
		return numberOfBatches;
	}

	private int getRecordCount() {
		int recordCount = 0;
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "select count(*) from content WHERE deleted IS NULL " + getFilterSitemapUrlsQuery(request);
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			if (rs.next()) {
				recordCount = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("Error when getting record count");
			e.printStackTrace();
		} finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
		return recordCount;
	}
}
