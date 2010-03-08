package se.raa.ksamsok.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar förfrågningar till K-samsöks API
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	//klass specifik logger
	private static final Logger logger = 
		Logger.getLogger("se.raa.ksamsok.api.APIServlet");

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
		try {
			reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
			method = APIMethodFactory.getAPIMethod(reqParams, writer);
			method.performMethod();
		} catch (APIException e) {
			Diagnostic(writer , e);
		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
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