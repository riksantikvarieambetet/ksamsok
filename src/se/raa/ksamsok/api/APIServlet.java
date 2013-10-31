package se.raa.ksamsok.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.java.generationjava.io.xml.SimpleXmlWriter;

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.apikey.APIKeyManager;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar förfrågningar till K-samsöks API
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	//klass specifik logger
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.APIServlet");

	@Autowired
	private APIKeyManager keyManager;

	// fabrik
	private APIMethodFactory apiMethodFactory;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (logger.isInfoEnabled()) {
			logger.info("Startar APIServlet");
		}
		apiMethodFactory = new APIMethodFactory();
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		AutowireCapableBeanFactory awcb = ctx.getAutowireCapableBeanFactory();
		awcb.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		awcb.autowireBeanProperties(apiMethodFactory, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		if (logger.isInfoEnabled()) {
			logger.info("APIServlet startad");
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar APIServlet");
		}
		if (logger.isInfoEnabled()) {
			logger.info("APIServlet stoppad");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {	
		//sätter contentType och character encoding
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String,String> reqParams = null;
		APIMethod method = null;
		PrintWriter writer = null;
		try {
			writer = resp.getWriter();
			String stylesheet = null;
			String apiKey = req.getParameter(APIMethod.API_KEY_PARAM_NAME);
			if (apiKey != null) apiKey = StaticMethods.removeChar(apiKey, '"');
			if (apiKey != null && keyManager.contains(apiKey)) {
					try {
						reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
						stylesheet = reqParams.get("stylesheet");
						//Kollar om frågan till ksamsök har en maxCount satt, och sätter en om frågan inte har maxCount satt.
						if(reqParams.get("maxCount") == null) {
							reqParams.put("maxCount", "10000");
						}
						method = apiMethodFactory.getAPIMethod(reqParams, writer);
						method.performMethod();
						keyManager.updateUsage(apiKey);
					} catch (MissingParameterException | BadParameterException | DiagnosticException e) {
						resp.setStatus(400);
						logger.error("queryString i requesten: "+ req.getQueryString());					
						diagnostic(writer, method, stylesheet, e);
					}
			} else if (apiKey == null){
				resp.setStatus(400);
				diagnostic(writer, method, stylesheet, new DiagnosticException("API-nyckel saknas", "APIServlet.doGet", null, false));
			} else {
				resp.setStatus(400);
				diagnostic(writer, method, stylesheet, new DiagnosticException("Felaktig API-nyckel", "APIServlet.doGet", null, false));
			}
		} catch (IOException e2) {
			resp.setStatus(500);
			logger.error("In doGet", e2);
		} finally {
			if (writer != null){
				writer.close();
			}
		}
	}

	/**
	 * skriver ut felmeddelanden
	 * @param writer
	 * @param e
	 * @throws IOException 
	 */
	private void diagnostic(PrintWriter writer, APIMethod method, String stylesheet, APIException e) throws IOException {
		logger.warn(e.getClassName() + " - " + e.getDetails());
		// TODO: inte riktigt bra detta med header- och footer-kontrollerna men...
		boolean writeHead = true;
		boolean writeFoot = true;
		SimpleXmlWriter xmlWriter=new SimpleXmlWriter(writer);
		if (method != null) {
			writeHead = !method.isHeadWritten();
			writeFoot = !method.isFootWritten();
		}
		if (writeHead){
			xmlWriter.writeXmlVersion("1.0", "UTF-8");
			if(stylesheet!=null && stylesheet.trim().length()>0){
				xmlWriter.writeXmlStyleSheet(stylesheet,"text/xsl");
			}
			xmlWriter.writeEntity("result");
			xmlWriter.writeEntityWithText("version", APIMethod.API_VERSION);
		}
		xmlWriter.writeEntityWithText("error", e.getMessage());
		if (writeFoot){
			xmlWriter.endEntity();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
	
}