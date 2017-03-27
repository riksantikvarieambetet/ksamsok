package se.raa.ksamsok.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.XML;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;

import com.github.jsonldjava.jena.JenaJSONLD;

import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.apikey.APIKeyManager;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Hanterar förfrågningar till K-samsöks API
 * 
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	// klass specifik logger
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.APIServlet");

	@Autowired
	private APIKeyManager keyManager;

	// fabrik
	private APIMethodFactory apiMethodFactory;

	private Format format = Format.XML;
	private boolean prettyPrint = false;
	private int indentFactor = 4;

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
		// This method subscribes the Json-ld writers to Jena-RDF writer
		JenaJSONLD.init();
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
	protected void doOptions(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setHeader("Access-Control-Allow-Origin", "*");
		res.setHeader("Access-Control-Allow-Headers", "Accept, Accept-Encoding, Content-Type");
		res.setHeader("Access-Control-Allow-Methods", "HEAD, GET, POST, TRACE, OPTIONS");
		super.doOptions(req, res);
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// sätter contentType och character encoding
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String, String> reqParams = null;
		APIMethod method = null;
		OutputStream out = null;
		try {
			out = resp.getOutputStream();
			String stylesheet = null;
			String apiKey = req.getParameter(APIMethod.API_KEY_PARAM_NAME);
			if (apiKey != null)
				apiKey = StaticMethods.removeChar(apiKey, '"');
			if (apiKey != null && keyManager.contains(apiKey)) {
				try {
					reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
					stylesheet = reqParams.get("stylesheet");
					method = apiMethodFactory.getAPIMethod(reqParams, out);
					logger.info("Reqparams " + reqParams + "\nStylesheet " + stylesheet + "\nMethod " + method);
					// Check which format the respond should be
					String acceptFormat = req.getHeader("Accept");
					if (acceptFormat != null && acceptFormat.toLowerCase().contains("json")) {
						format = Format.JSON_LD;
						method.setFormat(Format.JSON_LD);
						resp.setContentType("application/json; charset=UTF-8");
					} else {
						format = Format.XML;
						method.setFormat(Format.XML);
						resp.setContentType("application/xml; charset=UTF-8");
					}
					resp.setHeader("Access-Control-Allow-Origin", "*");
					method.performMethod();
					keyManager.updateUsage(apiKey);
				} catch (MissingParameterException e) {
					resp.setStatus(400);
					logger.error("queryString i requesten: " + req.getQueryString());
					diagnostic(out, method, stylesheet, e);
				} catch (BadParameterException e) {
					resp.setStatus(400);
					logger.error("queryString i requesten: " + req.getQueryString());
					diagnostic(out, method, stylesheet, e);
				} catch (DiagnosticException e) {
					resp.setStatus(500);
					logger.error("queryString i requesten: " + req.getQueryString());
					diagnostic(out, method, stylesheet, e);
				}
			} else if (apiKey == null) {
				resp.setStatus(400);
				diagnostic(out, method, stylesheet,
					new DiagnosticException("API-nyckel saknas", "APIServlet.doGet", null, false));
			} else {
				resp.setStatus(400);
				diagnostic(out, method, stylesheet,
					new DiagnosticException("Felaktig API-nyckel", "APIServlet.doGet", null, false));
			}
		} catch (Exception e) {
			resp.setStatus(500);
			logger.error("In doGet", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	/**
	 * skriver ut felmeddelanden
	 * 
	 * @param out
	 * @param e
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws JSONException
	 */
	private void diagnostic(OutputStream out, APIMethod method, String stylesheet, APIException e)
		throws IOException, ParserConfigurationException, TransformerException, JSONException {
		logger.warn(e.getClassName() + " - " + e.getDetails());
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		// Root element
		Element result = doc.createElement("result");
		doc.appendChild(result);
		// Stylesheet
		if (stylesheet != null && stylesheet.trim().length() > 0) {
			ProcessingInstruction pi = doc.createProcessingInstruction("xml-stylesheet",
				"type=\"text/xsl\" href=\"" + stylesheet + "\"");
			doc.insertBefore(pi, result);
		}
		// Version
		Element version = doc.createElement("version");
		version.appendChild(doc.createTextNode(APIMethod.API_VERSION));
		result.appendChild(version);
		// Error
		Element error = doc.createElement("error");
		error.appendChild(doc.createTextNode(e.getMessage()));
		result.appendChild(error);
		// Write result
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transform = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult strResult;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (format == Format.JSON_LD) {
			strResult = new StreamResult(baos);
		} else {
			strResult = new StreamResult(out);
		}
		transform.transform(source, strResult);
		if (format == Format.JSON_LD) {
			String json;
			if (prettyPrint) {
				json = XML.toJSONObject(baos.toString("UTF-8")).toString(indentFactor);
			} else {
				json = XML.toJSONObject(baos.toString("UTF-8")).toString();
			}
			out.write(json.getBytes("UTF-8"));
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}

}
