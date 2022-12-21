package se.raa.ksamsok.api;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient;
import org.json.XML;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import se.raa.ksamsok.api.exception.APIException;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;
import se.raa.ksamsok.lucene.ContentHelper;

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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Hanterar förfrågningar till K-samsöks API
 * 
 * @author Henrik Hjalmarsson
 */
public class APIServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;
	private static final Logger logger = LogManager.getLogger(APIServlet.class);

	private APIMethodFactory apiMethodFactory;

	private Format format = Format.XML;

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
	protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
		logger.info("doOptions called");
		res.setHeader("Access-Control-Allow-Origin", "*");
		res.setHeader("Access-Control-Allow-Headers", "Accept, Accept-Encoding, Content-Type");
		res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
		logger.info("header in doOptions: " + res.getHeader("Access-Control-Allow-Methods"));
		// super.doOptions(req, res);
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
		// sätter contentType och character encoding
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("text/xml; charset=UTF-8");
		Map<String, String> reqParams;
		APIMethod method;
		try (OutputStream out = resp.getOutputStream()) {
			String stylesheet = null;
			try {
				reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
				stylesheet = reqParams.get("stylesheet");
				method = apiMethodFactory.getAPIMethod(reqParams, out);
				logger.info("Reqparams " + reqParams + " : Stylesheet " + stylesheet + " : Method " + method);
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
				try {
					method.performMethod();
				} catch (BaseHttpSolrClient.RemoteSolrException e) {
					// convert into BadParameterEXception so we can use the "diagnostic" method
					if (e.getMessage().contains("undefined field")) {
						throw new BadParameterException("Okänt sökfält: " + StringUtils.substringAfterLast(e.getMessage(), " "),
								method.getClass().getName() + ".performMethod()", null, true);
					}
					throw e;
				}

			} catch (MissingParameterException | BadParameterException e) {

				e.printStackTrace();
				resp.setStatus(400);
				logger.error("queryString i requesten: " + req.getQueryString() + ": " + e.getMessage());
				diagnostic(out, stylesheet, e);
			} catch (DiagnosticException e) {

				e.printStackTrace();
				resp.setStatus(500);
				logger.error("queryString i requesten: " + req.getQueryString() + ": " + e.getMessage());
				diagnostic(out, stylesheet, e);
			}

		} catch (IOException | ParserConfigurationException | TransformerException e) {
			resp.setStatus(500);
			logger.error("In doGet", e);
		}
		// Ignore
	}

	/**
	 * skriver ut felmeddelanden
	 *
	 * @param out Ström att skriva felmeddelandet på
	 * @param e   exception som orsakat felutskriften
	 * @throws IOException                  om det inte går att skriva på utströmmen
	 * @throws ParserConfigurationException if a DocumentBuilder cannot be created which satisfies the configuration requested.
	 * @throws TransformerException         If an unrecoverable error occurs during the course of the transformation.
	 */
	private void diagnostic(OutputStream out, String stylesheet, APIException e)
		throws IOException, ParserConfigurationException, TransformerException {
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
			json = XML.toJSONObject(baos.toString(StandardCharsets.UTF_8)).toString();
			out.write(json.getBytes(StandardCharsets.UTF_8));
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		doGet(req, resp);
	}

}
