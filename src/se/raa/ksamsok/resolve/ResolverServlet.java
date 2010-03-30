package se.raa.ksamsok.resolve;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Enkel servlet som söker i lucene mha pathInfo som en identifierare och gör redirect 
 * till respektive tjänst beroende på format eller levererar lagrad rdf eller xml. 
 */
public class ResolverServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ResolverServlet.class);
	// urlar att redirecta till får inte starta med detta (gemener)
	private static final String badURLPrefix = "http://kulturarvsdata.se/";

	private static final MapFieldSelector RDF_FIELDS = new MapFieldSelector(
			new String[] { ContentHelper.I_IX_RDF });
	private static final MapFieldSelector PRES_FIELDS = new MapFieldSelector(
			new String[] { ContentHelper.I_IX_PRES });
	private static final MapFieldSelector HTML_FIELDS = new MapFieldSelector(
			new String[] { ContentHelper.I_IX_HTML_URL });
	private static final MapFieldSelector MUSEUMDAT_FIELDS = new MapFieldSelector(
			new String[] { ContentHelper.I_IX_MUSEUMDAT_URL });

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
		IndexSearcher is = LuceneServlet.getInstance().borrowIndexSearcher();
		try {
			PrintWriter writer;
			String urli, urle;
			String content = null;
			byte[] xmlContent;
			urli = "http://kulturarvsdata.se/" + path;
			Query q = new TermQuery(new Term(ContentHelper.IX_ITEMID, urli));
			if (logger.isDebugEnabled()) {
				logger.debug("resolve of (" + format + ") uri: " + urli);
			}
			TopDocs hits = is.search(q, 1);
			if (hits.totalHits != 1) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find record for q: " + q);
				}
				// specialfall för att hantera itemForIndexing=n, bara för rdf
				// vid detta fall ligger rdf:en bara i databasen och inte i lucene
				// men det är ett undantagsfall så vi provar alltid lucene först
				if (format == Format.RDF) {
					content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(urli);
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
					return;
				}
				resp.sendError(404, "Could not find record for path");
				return;
			}
			Document d;
			switch (format) {
			case RDF:
				d = is.doc(hits.scoreDocs[0].doc, RDF_FIELDS);
				xmlContent = d.getBinaryValue(ContentHelper.I_IX_RDF);
				if (xmlContent != null) {
					content = new String(xmlContent, "UTF-8");
				}
				// TODO: NEK ta bort när allt är omindexerat
				if (content == null) {
					content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(urli);
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
				d = is.doc(hits.scoreDocs[0].doc, PRES_FIELDS);
				xmlContent = d.getBinaryValue(ContentHelper.I_IX_PRES);
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
				d = is.doc(hits.scoreDocs[0].doc, HTML_FIELDS);
				urle = d.get(ContentHelper.I_IX_HTML_URL);
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
				d = is.doc(hits.scoreDocs[0].doc, MUSEUMDAT_FIELDS);
				urle = d.get(ContentHelper.I_IX_MUSEUMDAT_URL);
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
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(is);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		forwardRequest(req, resp);
	}
}
