package se.raa.ksamsok.resolve;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnyPredicateNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.TripleFactory;
import org.jrdf.graph.URIReference;
import org.jrdf.graph.global.BlankNodeImpl;
import org.jrdf.graph.global.URIReferenceImpl;
import org.jrdf.util.ClosableIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
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
		RDF, HTML, MUSEUMDAT, XML;

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
				if (logger.isDebugEnabled()) {
					logger.debug("Invalid format: " + pathComponents[2]);
				}
				resp.sendError(404, "Invalid format " + pathComponents[2]);
				return;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[3];
		} else {
			format = Format.RDF;
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[2];
		}
		try {
			PrintWriter writer;
			String urli, urle;
			String content = null;
			byte[] xmlContent;
			urli = "http://kulturarvsdata.se/" + path;
			SolrQuery q = new SolrQuery();
			q.setQuery(ContentHelper.IX_ITEMID + ":" + ClientUtils.escapeQueryChars(urli));
			q.setRows(1);
			// hämta bara nödvändigt fält
			switch (format) {
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
			if (logger.isDebugEnabled()) {
				logger.debug("resolve of (" + format + ") uri: " + urli);
			}
			QueryResponse response = searchService.query(q);
			SolrDocumentList hits = response.getResults();
			if (hits.getNumFound() != 1) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find record for q: " + q);
				}
				// specialfall för att hantera itemForIndexing=n, bara för rdf
				// vid detta fall ligger rdf:en bara i databasen och inte i lucene
				// men det är ett undantagsfall så vi provar alltid lucene först
				if (format == Format.RDF) {
					content = hrm.getXMLData(urli);
					if (content != null) {
						resp.setContentType("application/rdf+xml; charset=UTF-8");
						writer = resp.getWriter();
						// xml-header
						writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
						writer.println(content);
						writer.flush();
						return;
					}
				}
				else if (format == Format.HTML){
					content=hrm.getXMLData(urli);
					if (content != null){
						urle = getRedirectUrl(content);
						if (urle != null) {
							if (urle.toLowerCase().startsWith(badURLPrefix)) {
								logger.warn("HTML link is wrong, points to " + badURLPrefix +
										" for " + urli + ": " + urle);
								resp.sendError(404, "Invalid html url to pass on to");
							} else {
								resp.sendRedirect(urle);
							}
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Could not find html url for record with uri: " + urli);
							}
							resp.sendError(404, "Could not find html url to pass on to");
						}
						return;
					}
				}
				resp.sendError(404, "Could not find record for path");
				return;
			}
			switch (format) {
			case RDF:
				xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_RDF);
				// hämta ev från hack-cachen
				if (ShmSiteCacherHackTicket3419.useCache(req.getParameter(ShmSiteCacherHackTicket3419.KRINGLA), urli)) {
					content = ShmSiteCacherHackTicket3419.getOrRecache(urli, xmlContent);
				} else {
					if (xmlContent != null) {
						content = new String(xmlContent, "UTF-8");
					}
					// TODO: NEK ta bort när allt är omindexerat
					if (content == null) {
						content = hrm.getXMLData(urli);
					}
				}
				if (content == null) {
					logger.warn("Could not find rdf for record with uri: " + urli);
					resp.sendError(404, "No rdf for record");
					return;
				}
				resp.setContentType("application/rdf+xml; charset=UTF-8");
				writer = resp.getWriter();
				// xml-header
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println(content);
				writer.flush();
				break;

			case XML:
				xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_PRES);
				if (xmlContent != null) {
					content = new String(xmlContent, "UTF-8");
				}
				if (content == null) {
					logger.warn("Could not find xml for record with uri: " + urli);
					resp.sendError(404, "No presentation xml for record");
					return;
				}
				resp.setContentType("text/xml; charset=UTF-8");
				writer = resp.getWriter();
				// xml-header
				writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				writer.println(content);
				writer.flush();
				break;

			case HTML:
				urle = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_HTML_URL);
				if (urle != null) {
					if (urle.toLowerCase().startsWith(badURLPrefix)) {
						logger.warn("HTML link is wrong, points to " + badURLPrefix +
								" for " + urli + ": " + urle);
						resp.sendError(404, "Invalid html url to pass on to");
					} else {
						resp.sendRedirect(urle);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find html url for record with uri: " + urli);
					}
					resp.sendError(404, "Could not find html url to pass on to");
				}
				break;

			case MUSEUMDAT:
				urle = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_MUSEUMDAT_URL);
				if (urle != null) {
					if (urle.toLowerCase().startsWith(badURLPrefix)) {
						logger.warn("Museumdat link is wrong, points to " + badURLPrefix +
								" för " + urli + ": " + urle);
						resp.sendError(404, "Invalid museumdat url to pass on to");
					} else {
						resp.sendRedirect(urle);
					}
				} else {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not find museumdat url for record with uri: " + urli);
					}
					resp.sendError(404, "Could not find museumdat url to pass on to");
				}
				break;

				default:
					logger.warn("Invalid format: " + format);
					resp.sendError(404, "Invalid format");
			}

		} catch (Exception e) {
			logger.error("Error when resolving url, path:" + path + ", format: " + format, e);
			throw new ServletException("Error when resolving url", e);
		}
	}

	private String getRedirectUrl(String content) {
		try {
			Graph graph = RDFUtil.parseGraph(content);
			URIReference predicateToFind=graph.getElementFactory().createURIReference(URI.create("http://kulturarvsdata.se/ksamsok#url"));
			TripleFactory tripleFactory = graph.getTripleFactory();
			Triple tripleToFind = tripleFactory.createTriple(AnySubjectNode.ANY_SUBJECT_NODE ,predicateToFind, AnyObjectNode.ANY_OBJECT_NODE);
			ClosableIterable<Triple> foundTriples=graph.find(tripleToFind);
			String testOut="";
			String url="";
			for (Triple t:foundTriples)
			{
				url=t.getSubject().toString();
				testOut=t.toString();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String redirectUrl = null;
		int startIndex=content.indexOf("<url>");
		if (startIndex>0){
			int endIndex=content.indexOf("</url>");
			if (endIndex>(startIndex+5))
			{
				redirectUrl=content.substring(startIndex+5, endIndex);
			}
		}
		return redirectUrl;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
