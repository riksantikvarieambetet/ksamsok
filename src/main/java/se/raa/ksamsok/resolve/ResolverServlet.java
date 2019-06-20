package se.raa.ksamsok.resolve;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.method.AbstractAPIMethod;
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.RDFUtil;
import se.raa.ksamsok.solr.SearchService;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

/**
 * Enkel servlet som söker i lucene mha pathInfo som en identifierare och gör redirect till
 * respektive tjänst beroende på format eller levererar lagrad rdf eller xml.
 */
public class ResolverServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger();
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
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		ctx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE,
				true);
	}

	/**
	 * Försöker kontrollera om requesten är något resolverservleten ska hantera. Om det inte är det
	 * skickas requesten vidare mha request dispatchers.
	 *
	 * @param req  request
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
		} else {
			resp.setHeader("Access-Control-Allow-Origin", "*");
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
	 * Försöker göra forward på ej hanterad request genom att hitta en passande request dispatcher.
	 *
	 * @param req  request
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
			logger.error("Could not find dispatcher for \"" + name + "\"" + ", other names for them or not tomcat?");
			resp.sendError(500, "Could not pass request on, no dispatcher for \"" + name + "\"");
		}
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doOptions(req, resp);
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "HEAD, GET, OPTIONS");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
			// Check which format the respond should be
			String acceptFormat = req.getHeader("Accept") != null ? req.getHeader("Accept").toLowerCase() : "";
			if (acceptFormat.contains("rdf")) {
				format = Format.RDF;
			} else if (acceptFormat.contains("xml")) {
				format = Format.RDF; // Should be XML??
			} else if (acceptFormat.contains("json")) {
				format = Format.JSON_LD;
			} else if (acceptFormat.contains("html")) {
				format = Format.HTML;
			} else {
				format = Format.RDF;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[2];
		}
		// Check if json should be in pretty print
		Map<String, String> reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
		Boolean prettyPrint = false;
		if (reqParams.get("prettyPrint") != null && reqParams.get("prettyPrint").equalsIgnoreCase("true")) {
			prettyPrint = true;
		}
		switch (format) {
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
			// Get content from solr or db
			String response = prepareResponse(urli, format, req);
			// Make response
			makeResponse(response, format, urli, prettyPrint, resp);
		} catch (Exception e) {
			logger.error("Error when resolving url, path:" + path + ", format: " + format, e);
			throw new ServletException("Error when resolving url", e);
		}
	}

	/**
	 * This method gets the data from Solr or in some cases from db
	 *
	 * @param urli   - The path to the request, i.e. which object shoud we get
	 * @param format - The requested response format
	 * @param req    - The http servlet request
	 * @return - A string with the found content or null
	 * @throws Exception
	 */
	private String prepareResponse(String urli, Format format, HttpServletRequest req) throws Exception {
		String prepResp = null;
		byte[] xmlContent;
		SolrQuery q = new SolrQuery();

		final String escapedUrli = ClientUtils.escapeQueryChars(urli);
		StringBuilder sb = new StringBuilder(ContentHelper.IX_ITEMID).append(":").append(escapedUrli);

		// we have to append a special case to also fetch any posts that have the id in a "replaces"-tag
		sb.append(" OR ").append(ContentHelper.IX_REPLACES).append(":").append(escapedUrli);

		q.setQuery(sb.toString());
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
		// Get data
		QueryResponse response = searchService.query(q);
		SolrDocumentList hits = response.getResults();
		if (hits.getNumFound() != 1) {
			logger.debug("Could not find record for q: " + q);
			// specialfall för att hantera itemForIndexing=n, bara för rdf och html
			// vid detta fall ligger rdf:en bara i databasen och inte i lucene
			// men det är ett undantagsfall så vi provar alltid lucene först
			if (format == Format.RDF || format == Format.JSON_LD || format == Format.HTML) {
				String content = hrm.getXMLData(urli);
				if (content != null) {
					prepResp = content;
					if (format == Format.HTML) {
						prepResp = getRedirectUrl(content);
					}
				}
			}
		} else {
			switch (format) {
				case JSON_LD:
				case RDF:
					xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_RDF);
					if (xmlContent != null) {
						prepResp = new String(xmlContent, "UTF-8");
					} else {
						prepResp = hrm.getXMLData(urli);
					}
					break;
				case XML:
					xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_PRES);
					if (xmlContent != null) {
						prepResp = new String(xmlContent, "UTF-8");
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

	private String escapeChars(String urli) {
		return StringUtils.replace(urli, ":", "\\:");
	}

	private String buildReplacedByMultipleUrisJsonReply(ArrayList<String> replaceUris) {
		StringBuffer jsonBuf = new StringBuffer();
		if (replaceUris.size() > 1) {
			// create a json with all the redirect possibilities
			jsonBuf = new StringBuffer("{\"isReplacedBy:\" [");
			int counter = 0;
			for (String replaceUri : replaceUris) {
				jsonBuf.append("{\"record\": \"").append(replaceUri).append("\"}");
				if (++counter < replaceUris.size()) {
					jsonBuf.append(", ");
				}

			}
			jsonBuf.append("]}");
		}
		return jsonBuf.toString();
	}

	private String buildReplacedByMultipleUrisXmlReply(ArrayList<String> replaceUris) {
		StringBuffer xmlBuf = new StringBuffer();
		if (replaceUris.size() > 1) {
			// create an xml with all the redirect possibilities
			xmlBuf = new StringBuffer("<isReplacedBy>");
			for (String replaceUri : replaceUris) {
				xmlBuf.append("<record>").append(replaceUri).append("</record>");


			}
			xmlBuf.append("</isReplacedBy>");
		}
		return xmlBuf.toString();
	}

	/**
	 * This method writes the response
	 *
	 * @param response - The content from solr or db
	 * @param format   - The requested response format
	 * @param urli     - The path to the request, i.e. which object should we get
	 * @param resp     - The http servlet response
	 * @throws IOException
	 * @throws DiagnosticException
	 */
	private void makeResponse(String response, Format format, String urli, Boolean prettyPrint,
							  HttpServletResponse resp) throws IOException, DiagnosticException {
		ArrayList<String> replaceUris = new ArrayList<>();
		if (response != null) {
			Model m = ModelFactory.createDefaultModel();
			m.read(new ByteArrayInputStream(response.getBytes("UTF-8")), "UTF-8");
			final ResIterator resIterator = m.listSubjects();



			while (resIterator.hasNext()) {
				Resource res = resIterator.next();


				final StmtIterator statementIterator = res.listProperties();
				while (statementIterator.hasNext()) {
					Statement statement = statementIterator.next();
					Property predicate = statement.getPredicate();

					if ("replaces".equals(predicate.getLocalName()) && urli.equals(statement.getObject().toString())) {

						if (res.getURI() != null) {
							replaceUris.add(res.getURI());
							//resp.sendRedirect(res.getURI());
						} else {
							logger.warn("Found replaces: " + statement.getSubject().getLocalName() + " but no URL to redirect to");
						}
					}

				}

			}

		}

		// if we found only one replaceUri, redirect immediately:
		if (replaceUris.size() == 1) {
			resp.sendRedirect(replaceUris.get(0));
			return;
		}

		switch (format) {
			case JSON_LD:
				if (replaceUris.size() > 1) {
					String jsonReply = buildReplacedByMultipleUrisJsonReply(replaceUris);
					resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);

					PrintWriter out = resp.getWriter();
					resp.setContentType("application/json");
					resp.setCharacterEncoding("UTF-8");
					out.print(jsonReply);
					out.flush();
				} else if (response != null) {
					Model m = ModelFactory.createDefaultModel();
					m.read(new ByteArrayInputStream(response.getBytes("UTF-8")), "UTF-8");
					// It is done in APIServlet.init JenaJSONLD.init();
					RDFDataMgr.write(resp.getOutputStream(), m, prettyPrint ? RDFFormat.JSONLD_PRETTY : RDFFormat.JSONLD_COMPACT_FLAT);
				} else {
					resp.sendError(404, "Could not find record for path");
				}
				break;
			case RDF:
			case XML:
				if (replaceUris.size() > 1) {
					String jsonReply = buildReplacedByMultipleUrisXmlReply(replaceUris);
					resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);

					PrintWriter out = resp.getWriter();
					resp.setContentType("text/xml");
					resp.setCharacterEncoding("UTF-8");
					out.print(jsonReply);
					out.flush();
				} else if (response != null) {
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder;
					Document doc;
					try {
						docBuilder = docFactory.newDocumentBuilder();
						doc = docBuilder.parse(new ByteArrayInputStream(response.getBytes("UTF-8")));
						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						Transformer transform;
						transform = transformerFactory.newTransformer();
						DOMSource source = new DOMSource(doc);
						StreamResult strResult = new StreamResult(resp.getOutputStream());
						if (prettyPrint) {
							transform.setOutputProperty(OutputKeys.INDENT, "yes");
							transform.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
						}
						transform.transform(source, strResult);
					} catch (ParserConfigurationException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att initiera xml dokument hanteraren",
								AbstractAPIMethod.class.getName(), e.getMessage(), false);
					} catch (SAXException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att skapa xml svaret",
								AbstractAPIMethod.class.getName(), e.getMessage(), false);
					} catch (TransformerConfigurationException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att initiera xml dokument transformeraren",
								AbstractAPIMethod.class.getName(), e.getMessage(), false);
					} catch (TransformerException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att skriva xml dokument till ut-strömmen",
								AbstractAPIMethod.class.getName(), e.getMessage(), false);
					}
				} else if (format == Format.RDF) {
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
						if (format == Format.HTML) {
							logger.warn(
									"HTML link is wrong, points to " + badURLPrefix + " for " + urli + ": " + response);
							resp.sendError(404, "Invalid html url to pass on to");
						} else {
							logger.warn("Museumdat link is wrong, points to " + badURLPrefix + " för " + urli + ": " +
									response);
							resp.sendError(404, "Invalid museumdat url to pass on to");
						}
					} else {
						resp.sendRedirect(response);
					}
				} else if (format == Format.HTML) {
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
	 *
	 * @param content - String containing a rdf
	 * @return - a string with the url, if not found then null
	 */
	private String getRedirectUrl(String content) {
		String redirectUrl = null;
		try {
			Model model = RDFUtil.parseModel(content);
			Property uriPredicate = ResourceFactory.createProperty("http://kulturarvsdata.se/ksamsok#url");
			Selector selector = new SimpleSelector((Resource) null, uriPredicate, (RDFNode) null);
			StmtIterator iter = model.listStatements(selector);
			if (iter.hasNext()) {
				Statement s = iter.next();
				redirectUrl = s.getObject().asLiteral().getString();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return redirectUrl;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
