package se.raa.ksamsok.resolve;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

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
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
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
import se.raa.ksamsok.solr.SearchService;

/**
 * Enkel servlet som söker i lucene mha pathInfo som en identifierare och gör redirect till
 * respektive tjänst beroende på format eller levererar lagrad rdf eller xml.
 */
public class ResolverServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LogManager.getLogger(ResolverServlet.class);
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
		RDF("rdf"),
		HTML("html"),
		MUSEUMDAT("museumdat"),
		XML("xml"),
		JSON_LD("jsonld");

		String format;

		private static final HashMap<String, Format> lookupTable = new HashMap<>();

		// populate lookup table
		static {
			for (Format format : Format.values()) {
				lookupTable.put(format.getFormat(), format);
			}
		}

		Format(String format) {
			this.format = format;
		}

		String getFormat() {
			return format;
		}

		/**
		 * Parsar en sträng med formatet och ger motsvarande konstant.
		 *
		 * @param formatString formatsträng
		 * @return formatkonstant eller null
		 */
		static Format parseFormat(String formatString) {
			return lookupTable.get(formatString);
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
		String[] pathComponents = getPathComponents(path);
		if (pathComponents == null) {
			forwardRequest(req, resp);
		}
		return pathComponents;
	}

	private String[] getPathComponents(String path) {
		String[] pathComponents = null;

		if (path != null && !path.contains(".") && (pathComponents = path.substring(1).split("/")) != null) {
			if (pathComponents.length < 3 || pathComponents.length > 4) {
				pathComponents = null;
			}
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
		Format format = null;
		final String formatLowerCase;
		boolean formatSetInPath = false;

		// hantera olika format
		if (pathComponents.length == 4) {
			formatLowerCase = pathComponents[2].toLowerCase();
			format = Format.parseFormat(formatLowerCase);
			if (format == null) {
				logger.debug("Invalid format: " + pathComponents[2]);
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid format " + pathComponents[2]);
				return;
			}
			formatSetInPath = true;
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[3];
		} else {
			// Check which format the respond should be
			String acceptFormatString = req.getHeader("Accept") != null ? req.getHeader("Accept").toLowerCase() : "";

			// A request header can contain several accepted formats, we honor the priority order
			String[] acceptFormats = acceptFormatString.split(",");
			for (String acceptFormat : acceptFormats) {
				if (acceptFormat.contains("rdf")) {
					format = Format.RDF;
					break;
				} else if (acceptFormat.contains("xml")) {
					format = Format.RDF;
					break;
				} else if (acceptFormat.contains("json")) {
					format = Format.JSON_LD;
					break;
				} else if (acceptFormat.contains("html")) {
					format = Format.HTML;
					break;
				}
			}
			// fallback to rdf
			if (format == null) {
				format = Format.RDF;
			}
			path = pathComponents[0] + "/" + pathComponents[1] + "/" + pathComponents[2];
		}
		switch (format) {
			case JSON_LD:
				resp.setContentType("application/json; charset=UTF-8");
				break;
			case MUSEUMDAT:
			case XML:
				resp.setContentType("application/xml; charset=UTF-8");
				break;
			case RDF:
				resp.setContentType("application/rdf+xml; charset=UTF-8");
				break;
			case HTML:
			default:
				resp.setContentType("text/html; charset=UTF-8");
				break;
		}
		try {
			String urli = "http://kulturarvsdata.se/" + path;
			// Get content from solr or db
			PreparedResponse preparedResponse = prepareResponse(urli, format, req, formatSetInPath);
			// Make response
			makeResponse(preparedResponse, format, urli, resp);
		} catch (Exception e) {
			logger.error("Error when resolving url, path:" + path + ", format: " + format, e);
			throw new ServletException("Error when resolving url", e);
		}
	}

	/**
	 * This method gets the data from Solr or in some cases from db
	 *
	 * @param urli   - The path to the request, i.e. which object should we get
	 * @param format - The requested response format
	 * @param req    - The http servlet request
	 * @return - A string with the found content or null
	 * @throws Exception
	 */
	private PreparedResponse prepareResponse(String urli, Format format, HttpServletRequest req, boolean formatSetInPath) throws Exception {
		PreparedResponse preparedResponse = new PreparedResponse();
		String stringResponse = null;
		byte[] xmlContent;
		SolrQuery q = new SolrQuery();

		final String escapedUrli = ClientUtils.escapeQueryChars(urli);

		String sb = ContentHelper.IX_ITEMID + ":" + escapedUrli +

				// we have to append a special case to also fetch any posts that have the id in a "replaces"-tag
				" OR " + ContentHelper.IX_REPLACES + ":" + escapedUrli;
		q.setQuery(sb);

		// we want any items that are replacing old items to come first
		q.setSort(ContentHelper.IX_REPLACES, SolrQuery.ORDER.desc);

		// se till att vi får tillbaka alla i svaret
		q.setRows(null);

		// vi måste alltid ha rdf för att kunna kolla replaces
		q.setFields(ContentHelper.I_IX_RDF);

		// hämta bara nödvändigt fält
		switch (format) {
			case JSON_LD:
			case RDF:
				break;
			case XML:
				q.addField(ContentHelper.I_IX_PRES);
				break;
			case HTML:
				q.addField(ContentHelper.I_IX_HTML_URL);
				break;
			case MUSEUMDAT:
				q.addField(ContentHelper.I_IX_MUSEUMDAT_URL);
				break;
		}
		logger.debug("resolve of (" + format + ") uri: " + urli);

		// Get data
		QueryResponse response = searchService.query(q);
		SolrDocumentList hits = response.getResults();
		if (hits.getNumFound() < 1) {
			logger.debug("Could not find record for q: " + q);
			// om objektet inte finns i indexet kan det ändå finnas i databasen,
			// exempelvis om itemForIndexing=n, om objektet inte har någon medialicense
			// eller om objektet har blivit borttaget. I dessa fall ska vi returnera
			// 410 Gone
			if (hrm.existsInDatabase(urli)) {
				preparedResponse.setGone(true);
			}
		} else {
			// objektet finns i indexet

			for (int i = 0; i < hits.getNumFound(); i++) {
				// vi måste alltid hämta ut rdf:en för att kolla replaces
				xmlContent = (byte[]) hits.get(i).getFieldValue(ContentHelper.I_IX_RDF);
				if (xmlContent != null) {
					stringResponse = new String(xmlContent, StandardCharsets.UTF_8);
				} else {
					stringResponse = hrm.getXMLData(urli);
				}

				if (stringResponse != null) {
					Model m = ModelFactory.createDefaultModel();
					m.read(new ByteArrayInputStream(stringResponse.getBytes(StandardCharsets.UTF_8)), "UTF-8");
					final ResIterator resIterator = m.listSubjects();

					while (resIterator.hasNext()) {
						Resource res = resIterator.next();
						final StmtIterator statementIterator = res.listProperties();
						while (statementIterator.hasNext()) {
							Statement statement = statementIterator.next();
							Property predicate = statement.getPredicate();
							if ("replaces".equals(predicate.getLocalName())
									&& urli.equals(statement.getObject().toString())) {
								// we have found one that replaces the requested one
								String replacedByUri = res.getURI();
								if (replacedByUri != null) {
									if (formatSetInPath) {
										// the format has been explicitly requested in the url path, we have to
										// put it back in there
										final String formatString = format.getFormat();
										int formatEntyIndex = replacedByUri.lastIndexOf("/") + 1;
										StringBuilder tmpBuf = new StringBuilder(replacedByUri);

										tmpBuf.insert(formatEntyIndex, formatString + "/");
										replacedByUri = tmpBuf.toString();
									}
									preparedResponse.addReplacedByUri(replacedByUri);
								} else {
									logger.warn("Found replaces: " + statement.getSubject().getLocalName()
											+ " but no URL to redirect to");
								}
							}
						}
					}
				}
			}

			// in the case of json or rdf, we already have our stringResp from above, otherwise replace with correct content
			switch (format) {
				case JSON_LD:
				case RDF:
					// do nothing
					break;
				case XML:
					xmlContent = (byte[]) hits.get(0).getFieldValue(ContentHelper.I_IX_PRES);
					if (xmlContent != null) {
						stringResponse = new String(xmlContent, StandardCharsets.UTF_8);
					}
					break;
				case HTML:
					stringResponse = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_HTML_URL);
					break;
				case MUSEUMDAT:
					stringResponse = (String) hits.get(0).getFieldValue(ContentHelper.I_IX_MUSEUMDAT_URL);
					break;
				default:
					break;
			}
		}
		preparedResponse.setResponse(stringResponse);
		return preparedResponse;
	}

	private String buildReplacedByMultipleUrisJsonReply(ArrayList<String> replacedByUris) {
		StringBuffer jsonBuf = new StringBuffer();
		if (replacedByUris.size() > 1) {
			// create a json with all the redirect possibilities
			jsonBuf = new StringBuffer("{\"isReplacedBy:\" [");
			int counter = 0;
			for (String replacedByUri : replacedByUris) {
				jsonBuf.append("{\"record\": \"").append(replacedByUri).append("\"}");
				if (++counter < replacedByUris.size()) {
					jsonBuf.append(", ");
				}

			}
			jsonBuf.append("]}");
		}
		return jsonBuf.toString();
	}

	private String buildReplacedByMultipleUrisXmlReply(ArrayList<String> replacedByUris) {
		StringBuffer xmlBuf = new StringBuffer();
		if (replacedByUris.size() > 1) {
			// create an xml with all the redirect possibilities
			xmlBuf = new StringBuffer("<isReplacedBy>");
			for (String replacedByUri : replacedByUris) {
				xmlBuf.append("<record>").append(replacedByUri).append("</record>");


			}
			xmlBuf.append("</isReplacedBy>");
		}
		return xmlBuf.toString();
	}

	private String buildReplacedByMultipleUrisHtmlReply(ArrayList<String> replacedByUris) {
		StringBuffer htmlBuf = new StringBuffer();
		if (replacedByUris.size() > 1) {
			// create an html with all the redirect possibilities
			htmlBuf = new StringBuffer();
			htmlBuf.append("<body>");
			htmlBuf.append("<h2>Den efterfrågade URI:n har ersatts av följande URI:er</h2>");
			htmlBuf.append("<ul>");
			for (String replacedByUri : replacedByUris) {
				htmlBuf.append("<li>").append("<a href=\"").append(replacedByUri).append("\">").append(replacedByUri).append("</a>").append("</li>");


			}
			htmlBuf.append("</ul>");
			htmlBuf.append("</body>");
		}
		return htmlBuf.toString();
	}

	/**
	 * This method writes the response
	 *
	 * @param preparedResponse - The content from solr or db
	 * @param format   - The requested response format
	 * @param urli     - The path to the request, i.e. which object should we get
	 * @param resp     - The http servlet response
	 * @throws IOException
	 * @throws DiagnosticException
	 */
	private void makeResponse(PreparedResponse preparedResponse, Format format, String urli,
							  HttpServletResponse resp) throws IOException, DiagnosticException {
		if (preparedResponse.isGone()) {
			// reply that the object is gone
			resp.sendError(HttpServletResponse.SC_GONE, "Object " + urli + " is gone");
			return;
		}

		// if we found only one replacedByUri, redirect immediately:
		if (preparedResponse.getReplacedByUris().size() == 1) {
			resp.sendRedirect(preparedResponse.getReplacedByUris().get(0));
			return;
		}

		switch (format) {
			case JSON_LD:
				if (preparedResponse.getReplacedByUris().size() > 1) {
					String jsonReply = buildReplacedByMultipleUrisJsonReply(preparedResponse.getReplacedByUris());
					resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);

					PrintWriter out = resp.getWriter();
					resp.setContentType("application/json");
					resp.setCharacterEncoding("UTF-8");
					out.print(jsonReply);
					out.flush();
				} else if (preparedResponse.getResponse() != null) {
					Model m = ModelFactory.createDefaultModel();
					m.read(new ByteArrayInputStream(preparedResponse.getResponse().getBytes(StandardCharsets.UTF_8)), "UTF-8");
					// It is done in APIServlet.init JenaJSONLD.init();
					RDFDataMgr.write(resp.getOutputStream(), m, RDFFormat.JSONLD10_COMPACT_FLAT);
				} else {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find record for path");
				}
				break;
			case RDF:
			case XML:
				if (preparedResponse.getReplacedByUris().size() > 1) {
					String jsonReply = buildReplacedByMultipleUrisXmlReply(preparedResponse.getReplacedByUris());
					resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);

					PrintWriter out = resp.getWriter();
					resp.setContentType("text/xml");
					resp.setCharacterEncoding("UTF-8");
					out.print(jsonReply);
					out.flush();
				} else if (preparedResponse.getResponse() != null) {
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder;
					Document doc;
					try {
						docBuilder = docFactory.newDocumentBuilder();
						doc = docBuilder.parse(new ByteArrayInputStream(preparedResponse.getResponse().getBytes(StandardCharsets.UTF_8)));
						TransformerFactory transformerFactory = TransformerFactory.newInstance();
						Transformer transform;
						transform = transformerFactory.newTransformer();
						DOMSource source = new DOMSource(doc);
						StreamResult strResult = new StreamResult(resp.getOutputStream());
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
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No rdf for record");
				} else {
					logger.warn("Could not find xml for record with uri: " + urli);
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No presentation xml for record");
				}
				break;
			case HTML:
			case MUSEUMDAT:
				if (preparedResponse.getReplacedByUris().size() > 1) {
					String jsonReply = buildReplacedByMultipleUrisHtmlReply(preparedResponse.getReplacedByUris());
					resp.setStatus(HttpServletResponse.SC_MULTIPLE_CHOICES);

					PrintWriter out = resp.getWriter();
					resp.setContentType("text/html");
					resp.setCharacterEncoding("UTF-8");
					out.print(jsonReply);
					out.flush();
				} else if (preparedResponse.getResponse() != null) {
					if (preparedResponse.getResponse().toLowerCase().startsWith(badURLPrefix)) {
						if (format == Format.HTML) {
							logger.warn(
									"HTML link is wrong, points to " + badURLPrefix + " for " + urli + ": " + preparedResponse.getResponse());
							resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid html url to pass on to");
						} else {
							logger.warn("Museumdat link is wrong, points to " + badURLPrefix + " för " + urli + ": " +
									preparedResponse.getResponse());
							resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid museumdat url to pass on to");
						}
					} else {
						resp.sendRedirect(preparedResponse.getResponse());
					}
				} else if (format == Format.HTML) {
					logger.debug("Could not find html url for record with uri: " + urli);
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find html url to pass on to");
				} else {
					logger.debug("Could not find museumdat url for record with uri: " + urli);
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Could not find museumdat url to pass on to");
				}
				break;
			default:
				logger.warn("Invalid format: " + format);
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid format");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
