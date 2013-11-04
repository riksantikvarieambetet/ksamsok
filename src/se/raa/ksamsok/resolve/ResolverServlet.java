package se.raa.ksamsok.resolve;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.github.jsonldjava.jena.JenaJSONLD;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.java.generationjava.io.xml.SimpleXmlWriter;

import se.raa.ksamsok.api.method.AbstractAPIMethod;
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.RDFUtil;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.util.ShmSiteCacherHackTicket3419;

/**
 * Enkel servlet som söker i lucene mha pathInfo som en identifierare och gör redirect 
 * till respektive tjänst beroende på format eller levererar lagrad rdf eller xml. 
 */
public class ResolverServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ResolverServlet.class);
	// urlar att redirecta till får inte starta med detta (gemener)
	private static final String badURLPrefix = "http://kulturarvsdata.se/";

	@Autowired
	private SearchService searchService;
	@Autowired
	HarvestRepositoryManager hrm;

	/**
	 * Enum för de olika formaten som stöds.
	 */
	private enum Format {
		RDF, HTML, MUSEUMDAT, XML, JSON_LD;

		/**
		 * Parsar en sträng med formatet och ger motsvarande konstant.
		 * 
		 * @param formatString formatsträng
		 * @return formatkonstant eller null
		 */
		static Format parseFormat(String formatString) {
			Format format = null;
			if ("rdf".equals(formatString)) {
				format = RDF;
			} else if ("html".equals(formatString)) {
				format = HTML;
			} else if ("museumdat".equals(formatString)) {
				format = MUSEUMDAT;
			} else if ("xml".equals(formatString)) {
				format = XML; 
			} else if ("jsonld".equals(formatString)) {
				format = JSON_LD;
			}
			return format;
		}
	};

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
	}

	/**
	 * Försöker kontrollera om requesten är något resolverservleten ska hantera. Om det inte
	 * är det skickas requesten vidare mha request dispatchers.
	 * 
	 * @param req request
	 * @param resp response
	 * @return de olika delarna i pathInfo, eller null om requesten inte ska hanteras av resolvern
	 * @throws ServletException
	 * @throws IOException
	 */
	protected String[] checkAndForwardRequests(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		String path = req.getPathInfo();
		// special då resolverservlet "käkar" upp default-sidehanteringen
		if ("/admin/".equals(path) || "/".equals(path)) {
			resp.sendRedirect("index.jsp");
			return null;
		}
		// hantera "specialfallet" med /resurser/a/b/c[/d]
		if (path != null && path.startsWith("/resurser")) {
			path = path.substring(9);
		}
		String[] pathComponents = null;

		if (path != null && !path.contains(".") && (pathComponents = path.substring(1).split("/")) != null) {
			if (pathComponents.length < 3 || pathComponents.length > 4) {
				pathComponents = null;
			}
		}
		if (pathComponents == null) {
			forwardRequest(req, resp);
		}
		return pathComponents;
	}

	/**
	 * Försöker göra forward på ej hanterad request genom att hitta en passande request
	 * dispatcher.
	 * 
	 * @param req request
	 * @param resp response
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void forwardRequest(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		RequestDispatcher rd;
		// TODO: dessa dispatchers fungerar för tomcat, fungerar de för övriga?
		String name = "default";
		if (req.getRequestURI().endsWith(".jsp")) {
			name = "jsp";
		}
		rd = getServletContext().getNamedDispatcher(name);
		if (rd != null) {
			rd.forward(req, resp);
		} else {
			logger.error("Could not find dispatcher for \"" + name + "\"" +
					", other names for them or not tomcat?");
			resp.sendError(500, "Could not pass request on, no dispatcher for \"" +
					name + "\"");
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// ge möjlighet att rensa cache manuellt vid behov
		ShmSiteCacherHackTicket3419.clearCache(req.getParameter(ShmSiteCacherHackTicket3419.CLEAR_CACHE));
		String[] pathComponents = checkAndForwardRequests(req, resp);
		if (pathComponents == null) {
			return;
		}
		String path;
		Format format;
		// hantera olika format
		if (pathComponents.length == 4) {
			format = Format.parseFormat(pathComponents[2].toLowerCase());
			if (format == null) {
				logger.debug("Invalid format: " + pathComponents[2]);
				resp.sendError(404, "Invalid format " + pathComponents[2]);
				return;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[3];
		} else {
			//Check which format the respond should be
			String acceptFormat=req.getHeader("Accept").toLowerCase();
			if (acceptFormat.contains("rdf")){
				format = Format.RDF;
			} else if (acceptFormat.contains("xml")) {
				format = Format.RDF; // Should be XML??
			} else if (acceptFormat.contains("json")) {
				format = Format.JSON_LD;
			} else if (acceptFormat.contains("html")){
				format = Format.HTML;
			} else {
				format = Format.RDF;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[2];
		}
		switch (format){
		case HTML:
			resp.setContentType("text/html; charset=UTF-8");
			break;
		case JSON_LD:
			resp.setContentType("application/json; charset=UTF-8");
			break;
		case MUSEUMDAT:
			resp.setContentType("application/xml; charset=UTF-8");
			break;
		case RDF:
			resp.setContentType("application/rdf+xml; charset=UTF-8");
			break;
		case XML:
			resp.setContentType("application/xml; charset=UTF-8");
			break;
		default:
			resp.setContentType("text/html; charset=UTF-8");
			break;
		}
		try {
			String urli = "http://kulturarvsdata.se/" + path;
			//Get content from solr or db
			String response = prepareResponse(urli, format, req);
			//Make responde
			
			Pattern p = Pattern.compile("prettyPrint=(\\w*)&?");
			Matcher m = p.matcher(req.getQueryString() != null ? req.getQueryString() : "");
			Boolean prettyPrint = false;
			if (m.find()) {
				if (m.groupCount()>0){
					if (m.group(1).contains("true")){
						prettyPrint=true;
					}
				}
			}
			makeResponse(response, format, urli, prettyPrint, resp);
		} catch (Exception e) {
			logger.error("Error when resolving url, path:" + path + ", format: " + format, e);
			throw new ServletException("Error when resolving url", e);
		}
	}

	/**
	 * This method gets the data from Solr or in some cases from db
	 * 
	 * @param urli - The path to the request, i.e. which object shoud we get
	 * @param format - The requested response format
	 * @param req - The http servlet request
	 * @return - A string with the found content or null
	 * @throws Exception
	 */
	private String prepareResponse(String urli, Format format, HttpServletRequest req) throws Exception{
		String prepResp = null;
		byte[] xmlContent;
		SolrQuery q = new SolrQuery();
		q.setQuery(ContentHelper.IX_ITEMID + ":" + ClientUtils.escapeQueryChars(urli));
		q.setRows(1);
		// hämta bara nödvändigt fält
		switch (format) {
		case JSON_LD:
		case RDF:
			q.setFields(ContentHelper.I_IX_RDF);
			break;
		case XML:
			q.setFields(ContentHelper.I_IX_PRES);
			break;
		case HTML:
			q.setFields(ContentHelper.I_IX_HTML_URL);
			break;
		case MUSEUMDAT:
			q.setFields(ContentHelper.I_IX_MUSEUMDAT_URL);
			break;
		}
		logger.debug("resolve of (" + format + ") uri: " + urli);
		//Get data
		QueryResponse response = searchService.query(q);
		SolrDocumentList hits = response.getResults();			
		if (hits.getNumFound() != 1) {
			logger.debug("Could not find record for q: " + q);
			// specialfall för att hantera itemForIndexing=n, bara för rdf och html
			// vid detta fall ligger rdf:en bara i databasen och inte i lucene
			// men det är ett undantagsfall så vi provar alltid lucene först
			if (format == Format.RDF || format == Format.JSON_LD || format == Format.HTML) {
				String content = hrm.getXMLData(urli);
				if (content != null){
					prepResp = content;
					if (format == Format.HTML){
						prepResp = getRedirectUrl(content);
					}
				}
			}
		} else {
			switch (format) {
			case JSON_LD:
			case RDF:
			case XML:
				xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_RDF);
				// hämta ev från hack-cachen
				if (format != Format.XML && ShmSiteCacherHackTicket3419.useCache(req.getParameter(ShmSiteCacherHackTicket3419.KRINGLA), urli)) {
					prepResp = ShmSiteCacherHackTicket3419.getOrRecache(urli, xmlContent);
				} else {
					if (xmlContent != null) {
						prepResp = new String(xmlContent, "UTF-8");
					} else if (format != Format.XML  && xmlContent == null) {
						prepResp = hrm.getXMLData(urli);
					}
				}
				break;
			case HTML:
				prepResp = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_HTML_URL);
				break;
			case MUSEUMDAT:
				prepResp = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_MUSEUMDAT_URL);
				break;
			default:
				break;
			}
		}
		return prepResp;
	}
	
	/**
	 * This method writes the response
	 * @param response - The content from solr or db
	 * @param format - The requested response format
	 * @param urli - The path to the request, i.e. which object shoud we get
	 * @param resp - The http servlet response
	 * @throws IOException
	 */
	private void makeResponse(String response, Format format, String urli, Boolean prettyPrint, HttpServletResponse resp) throws IOException {
		switch (format) {
		case JSON_LD:
			if (response != null){
				Model m = ModelFactory.createDefaultModel();
				m.read(new ByteArrayInputStream(response.getBytes("UTF-8")), "UTF-8");
				//It is done in APIServlet.init JenaJSONLD.init();
				RDFDataMgr.write(resp.getOutputStream(), m, prettyPrint ? JenaJSONLD.JSONLD_FORMAT_PRETTY : JenaJSONLD.JSONLD_FORMAT_FLAT);
			} else {
				resp.sendError(404, "Could not find record for path");
			}
			break;
		case RDF:
		case XML:
			if (response != null) {
				SimpleXmlWriter xmlWriter = new SimpleXmlWriter(resp.getWriter());
				xmlWriter.writeXmlVersion("1.0", "UTF-8");
				xmlWriter.writeXml(response);
			} else if (format == Format.RDF){
				logger.warn("Could not find rdf for record with uri: " + urli);
				resp.sendError(404, "No rdf for record");
			} else {
				logger.warn("Could not find xml for record with uri: " + urli);
				resp.sendError(404, "No presentation xml for record");
			}
			break;
		case HTML:
		case MUSEUMDAT:
			if (response != null) {
				if (response.toLowerCase().startsWith(badURLPrefix)) {
					if (format == Format.HTML){
						logger.warn("HTML link is wrong, points to " + badURLPrefix + " for " + urli + ": " + response);
						resp.sendError(404, "Invalid html url to pass on to");
					} else {
						logger.warn("Museumdat link is wrong, points to " + badURLPrefix + " för " + urli + ": " + response);
						resp.sendError(404, "Invalid museumdat url to pass on to");
					}
				} else {
					resp.sendRedirect(response);
				}
			} else if (format == Format.HTML){
				logger.debug("Could not find html url for record with uri: " + urli);
				resp.sendError(404, "Could not find html url to pass on to");
			} else {
				logger.debug("Could not find museumdat url for record with uri: " + urli);
				resp.sendError(404, "Could not find museumdat url to pass on to");
			}
			break;
		default:
			logger.warn("Invalid format: " + format);
			resp.sendError(404, "Invalid format");
		}
	}


	/**
	 * This method gets the url to the the original storage of the rdf data
	 * @param content - String containing a rdf
	 * @return - a string with the url, if not found then null
	 */
	private String getRedirectUrl(String content) {
		String redirectUrl = null;
		try {
			Model model = RDFUtil.parseModel(content);
			Property uriPredicate=ResourceFactory.createProperty("http://kulturarvsdata.se/ksamsok#url");
			Selector selector = new SimpleSelector((Resource) null, uriPredicate, (RDFNode) null);
			StmtIterator iter = model.listStatements(selector);
			if (iter.hasNext()){
				Statement s = iter.next();
				redirectUrl=s.getObject().asLiteral().getString();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return redirectUrl;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
