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

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.apikey.APIKeyManager;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar fï¿½rfrï¿½gningar till K-samsï¿½ks API
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
			throws ServletException, IOException {	
		//sätter contentType och character encoding
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String,String> reqParams = null;
		APIMethod method = null;
		PrintWriter writer = resp.getWriter();
		try {
			String stylesheet = null;
			String apiKey = req.getParameter(APIMethod.API_KEY_PARAM_NAME);
			if (apiKey != null) apiKey = StaticMethods.removeChar(apiKey, '"');
			if (apiKey != null && keyManager.contains(apiKey)) {
				try {
					reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
					stylesheet = reqParams.get("stylesheet");
					method = apiMethodFactory.getAPIMethod(reqParams, writer);
					method.performMethod();
					keyManager.updateUsage(apiKey);
				} catch (APIException e) {
					logger.error("queryString i requesten: "+ req.getQueryString());					
					diagnostic(writer, method, stylesheet, e);
				} catch (Exception e) {
					logger.error("queryString i requesten: "+ req.getQueryString());
					logger.error("In doGet", e);
				}
			} else if (apiKey == null){
				diagnostic(writer, method, stylesheet, new DiagnosticException("API-nyckel saknas", "APIServlet.doGet", null, false));
			} else {
				diagnostic(writer, method, stylesheet, new DiagnosticException("Felaktig API-nyckel", "APIServlet.doGet", null, false));
			}
		} finally {
			writer.close();
		}
	}

	/**
	 * skriver ut felmeddelanden
	 * @param writer
	 * @param e
	 */
	private void diagnostic(PrintWriter writer, APIMethod method, String stylesheet, APIException e) {
		logger.error(e.getClassName() + " - " + e.getDetails());
		// TODO: inte riktigt bra detta med header- och footer-kontrollerna men...
		boolean writeHead = true;
		boolean writeFoot = true;
		if (method != null) {
			writeHead = !method.isHeadWritten();
			writeFoot = !method.isFootWritten();
		}
		StartEndWriter.writeError(writer, writeHead, writeFoot, stylesheet,	e);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}
	
}