package se.raa.ksamsok.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.statisticLogg.StatisticLogger;
import se.raa.ksamsok.harvest.DBUtil;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar förfrågningar till K-samsöks API
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet
{
	private static final long serialVersionUID = -7746449046954514364L;
	//klass specifik logger
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.APIServlet");
	private static DataSource ds = null; //Databas källa
	private Set<String> APIKeys; //Set med de API nycklar som finns
	static final String DATASOURCE_NAME = "harvestdb";
	private static final StatisticLogger statisticLogger = new StatisticLogger();
	private static Thread loggerThread; //Loggar sökningar
	
	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		loggerThread = new Thread(statisticLogger);
		loggerThread.start();
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds = (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
			APIKeys = new HashSet<String>();
			c = ds.getConnection();
			String sql = "SELECT apikey FROM apikeys";
			ps = c.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next()) {
				APIKeys.add(rs.getString("apikey"));
			}
		}catch(NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getSQLState());
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}

	@Override
	public void destroy()
	{
		super.destroy();
		loggerThread.interrupt(); //dödar logger tråden
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{	
		//sätter contentType och character encoding
		StartEndWriter.reset();
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String,String> reqParams = null;
		APIMethod method = null;
		PrintWriter writer = resp.getWriter();
		String APIKey = req.getParameter(APIMethod.API_KEY_PARAM_NAME);
		if(APIKey != null && APIKeys.contains(APIKey)) {
			
			try {
				reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
				StartEndWriter.setStylesheet(reqParams.get("stylesheet"));
				method = APIMethodFactory.getAPIMethod(reqParams, writer);
				method.performMethod();
				updateStatistics(APIKey);
			} catch (APIException e) {
				Diagnostic(writer , e);
			}catch (Exception e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}else {
			Diagnostic(writer, new DiagnosticException("API nyckel saknas", "APIServlet.doGet", null, false));
		}
	}
	
	/**
	 * Uppdaterar statistik databasen med +1 sökning från given API nyckel
	 * @param APIKey där sökningen kommer ifrån
	 */
	private void updateStatistics(String APIKey)
	{
		Connection c = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			c = ds.getConnection();
			String sql = "UPDATE apikeys SET total=total+1 WHERE apikey=?";
			ps = c.prepareStatement(sql);
			ps.setString(1, APIKey);
			ps.executeUpdate();
			DBUtil.commit(c);
		}catch(SQLException e) {
			logger.error("error. Doing rollback");
			DBUtil.rollback(c);
			e.printStackTrace();
		}finally {
			DBUtil.closeDBResources(rs, ps, c);
		}
	}

	/**
	 * skriver ut felmeddelanden
	 * @param writer
	 * @param e
	 */
	private void Diagnostic(PrintWriter writer, APIException e)
	{
		logger.error(e.getClassName() + "\n" + e.getDetails());
		StartEndWriter.writeError(writer, e);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException 
	{
		doGet(req, resp);
	}
}