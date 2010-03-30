package se.raa.ksamsok.sru;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.solr.util.NumberUtils;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLTermNode;

import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;
import se.raa.ksamsok.lucene.ContentHelper.Index;

/**
 * Servlet som svarar på sru-anrop och som söker i indexet.
 */
@SuppressWarnings("unused")
public class SRUServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.sru.SRUServlet");

	private static final long serialVersionUID = 1L;

	// namespaces, prefix mm
	public static final String NS_SRW = "http://www.loc.gov/zing/srw/";
	public static final String NS_ZR_20 = "http://explain.z3950.org/dtd/2.0/";
	public static final String NS_SAMSOK_PRES = "http://kulturarvsdata.se/presentation#";
	public static final String PREFIX_NS_SAMSOK_PRES = "pres";
	public static final String LOCATION_NS_SAMSOK_PRES = "http://kulturarvsdata.se/resurser/presentation.xsd";  // TODO: rätt schemaplats
	//public static final String NS_OAI_DC = "http://www.openarchives.org/OAI/2.0/oai_dc/";
	// TODO: k-samsöks-rdf?
	public static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String PREFIX_NS_RDF = "rdf";
	public static final String LOCATION_NS_RDF = "http://www.w3.org/2000/07/rdf.xsd";  // TODO: rätt schemaplats

	public static final String SRU_VERSION = "1.1";

	private static final int DEFAULT_NUM_RECORDS = 10;
	private static final int MAX_NUM_RECORDS = 500;

	// TODO: bättre datastruktur för att kunna använda i explain och överallt
	private static final Map<String, String> knownSchemas = new HashMap<String, String>();

	//private static final XMLOutputFactory xmlof = XMLOutputFactory.newInstance();

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		if (knownSchemas.size() == 0) {
			//knownSchemas.add("oai_dc");
			//knownSchemas.add(NS_OAI_DC);
			knownSchemas.put(NS_RDF, NS_RDF);
			knownSchemas.put(PREFIX_NS_RDF, NS_RDF);
			knownSchemas.put(NS_SAMSOK_PRES, NS_SAMSOK_PRES);
			knownSchemas.put(PREFIX_NS_SAMSOK_PRES, NS_SAMSOK_PRES);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// parametrar in ska vara kodade i utf-8 enligt sru-standard
		Map<String, String> reqParams;
		try {
			reqParams = ContentHelper.extractUTF8Params(req.getQueryString());
		} catch (Exception e) {
			throw new ServletException("Fel i query-sträng", e);
		}
		String operation = reqParams.get("operation");
		// tala om att det är xml vi skickar 
		resp.setContentType("text/xml; charset=UTF-8");
		XMLStreamWriter xmlsw = null;
		PrintWriter writer = resp.getWriter();
		try {
			/* bort tills vidare
			xmlsw = xmlof.createXMLStreamWriter(writer);
			xmlsw.writeStartDocument();
			xmlsw.setPrefix("srw", NS_SRW);
			xmlsw.setPrefix("zr", NS_ZR_20);
			*/
			// xml-header
			writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			// eka en ev stylesheet parameter
			String stylesheet = reqParams.get("stylesheet");
			if (stylesheet != null && stylesheet.trim().length() > 0) {
				/*
				xmlsw.writeProcessingInstruction("xml-stylesheet type=\"text/xsl\" href=\"" +
						stylesheet + "\"");
				*/
				writer.println("<?xml-stylesheet type=\"text/xsl\" href=\""
						+ stylesheet.replace("\"", "&quot;") + "\"?>");
			}
	
			// hantera respektive operation
			if (operation != null) {
				if (operation.equals("searchRetrieve")) {
					handleSearchRetrieve(reqParams, writer);
				} else if (operation.equals("scan")) {
					handleScan(reqParams, writer);
				} else if (operation.equals("explain")) {
					handleExplain(reqParams, writer);
				} else {
					// fel i (eller ej stödd) parameter operation
					diagnostics(writer, 4, "Unsupported operation");
				}
			} else {
				handleExplain(reqParams, writer);
			}
/*
		} catch (XMLStreamException e) {
			throw new ServletException(e);
*/
		} finally {
			if (xmlsw != null) {
				try {
					xmlsw.close();
				} catch (Exception e) {
					logger.warn("Fel vid stängning av XMLStreamWriter", e);
				}
			}
		}
	}

	/*
 		Känns inte lika läsbart som den nedan tyvärr
	private void handleExplain(HttpServletRequest request,	HttpServletResponse response,
			XMLStreamWriter xmlsw) throws ServletException, IOException, XMLStreamException {
		xmlsw.writeStartElement(NS_SRW, "explainResponse");
		xmlsw.writeNamespace("srw", NS_SRW);
		xmlsw.writeNamespace("zr", NS_ZR_20);
		xmlsw.writeStartElement(NS_SRW, "version");
		xmlsw.writeCharacters(SRU_VERSION);
		xmlsw.writeEndElement();
		xmlsw.writeStartElement(NS_SRW, "record");
		xmlsw.writeStartElement(NS_SRW, "recordPacking");
		xmlsw.writeCharacters("xml");
		xmlsw.writeEndElement();
		xmlsw.writeStartElement(NS_SRW, "recordSchema");
		xmlsw.writeCharacters(NS_ZR_20);
		xmlsw.writeEndElement();
		xmlsw.writeStartElement(NS_SRW, "recordData");
		xmlsw.writeStartElement(NS_ZR_20, "explain");
		xmlsw.writeStartElement(NS_ZR_20, "databaseInfo");
		xmlsw.writeStartElement("title");
		xmlsw.writeAttribute("lang", "en");
		xmlsw.writeAttribute("primary", "true");
		xmlsw.writeCharacters("K-samsök");
		xmlsw.writeEndElement();
		xmlsw.writeStartElement("description");
		xmlsw.writeAttribute("lang", "en");
		xmlsw.writeAttribute("primary", "true");
		xmlsw.writeCharacters("Information skördad från flera källor");
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeStartElement(NS_ZR_20, "schemaInfo");
		xmlsw.writeStartElement(NS_ZR_20, "schema");
		xmlsw.writeAttribute("identifier", "http://www.openarchives.org/OAI/2.0/oai_dc/");
		xmlsw.writeAttribute("location", "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
		xmlsw.writeAttribute("sort", "false");
		xmlsw.writeAttribute("retrieve", "true");
		xmlsw.writeStartElement("title");
		xmlsw.writeAttribute("lang", "en");
		xmlsw.writeCharacters("OAI Dublin Core");
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
		xmlsw.writeEndElement();
	}
	*/

	// hantera ett sru-explain-anrop
	private void handleExplain(Map<String, String> reqParams, PrintWriter writer) throws ServletException, IOException {
		// 2-ställig är sv, 3-ställig är swe, se http://www.loc.gov/standards/iso639-2/
		final String langSV = "lang=\"sv\"";
		writer.println("<srw:explainResponse xmlns:srw=\"" + NS_SRW + "\">");
		writer.println("  <srw:version>" + SRU_VERSION + "</srw:version>");
		writer.println("  <srw:record>");
		writer.println("    <srw:recordPacking>XML</srw:recordPacking>");
		writer.println("    <srw:recordSchema>" + NS_ZR_20 + "</srw:recordSchema>");
		writer.println("    <srw:recordData>");

		writer.println("      <explain xmlns=\"" + NS_ZR_20 + "\">");
		writer.println("        <databaseInfo>");
		writer.println("           <title " + langSV + " primary=\"true\">K-Samsök</title>");
		writer.println("           <description " + langSV + " primary=\"true\">");
		writer.println("               Information skördad från flera källor. Index markerade med [*]");
		writer.println("               har även en prefixad variant per sammanhang (värden av index ");
		writer.println("               " + ContentHelper.IX_CONTEXTTYPE + ") som enbart söker i data");
		writer.println("               för det sammanhanget. De prefix som finns fn är [");
		// hämta dynamiskt ut alla värden för contextType
		IndexSearcher s = null;
		String prefix = null;
		try {
			s = LuceneServlet.getInstance().borrowIndexSearcher();
			Query q = new WildcardQuery(new Term(ContentHelper.IX_CONTEXTTYPE, "*"));
			Set<Term> extractedTermSet = new HashSet<Term>();
			q = s.rewrite(q);
			q.extractTerms(extractedTermSet);
			for (Term t: extractedTermSet) {
				writer.println("               '" + t.text() + "'");
				if (prefix == null) {
					prefix = t.text();
				}
			}
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(s);
		}
		writer.println("               ]");
		writer.println("               Pga av antalet prefixade individuella index listas de ej nedan.");
		if (prefix != null) {
			writer.println("               Exempel på prefixad sökning: " + prefix + "_" +
					ContentHelper.IX_PARISHNAME + "=felestad");
		}
		writer.println("           </description>");
		writer.println("        </databaseInfo>");

		writer.println("        <indexInfo>");
		writer.println("           <set identifier=\"" + ContentHelper.CONTEXT_SET_REC_IDENTIFIER +"\" name=\"" + ContentHelper.CONTEXT_SET_REC + "\"/>");
		writer.println("           <set identifier=\"" + ContentHelper.CONTEXT_SET_SAMSOK_IDENTIFIER + "\" name=\"" + ContentHelper.CONTEXT_SET_SAMSOK + "\"/>");
		for (Index index: ContentHelper.getPublicIndices()) {
			writer.println("           <index>");
			writer.println("              <title " + langSV + ">" + xmlEscape(index.getTitle()) + "</title>");
			writer.println("              <map>");
			writer.println("                 <name set=\"" + ContentHelper.CONTEXT_SET_SAMSOK + "\">" + index.getIndex() + "</name>");
			// special för itemId/rec.identifier
			if (ContentHelper.IX_ITEMID.equals(index.getIndex())) {
				writer.println("                 <name set=\"" + ContentHelper.CONTEXT_SET_REC + "\">" + ContentHelper.IX_REC_IDENTIFIER + "</name>");
			}
			writer.println("              </map>");
			writer.println("           </index>");
		}
		writer.println("        </indexInfo>");

		writer.println("        <schemaInfo>");
		writer.println("           <schema name=\"" + PREFIX_NS_RDF + "\" identifier=\"" + NS_RDF + "\"");
		writer.println("                 location=\"" + LOCATION_NS_RDF + "\""); 
		writer.println("                 sort=\"false\" retrieve=\"true\">");
		writer.println("              <title " + langSV + ">RDF</title>");
		writer.println("           </schema>");
		writer.println("           <schema name=\"" + PREFIX_NS_SAMSOK_PRES + "\" identifier=\"" + NS_SAMSOK_PRES + "\"");
		writer.println("                 location=\"" + LOCATION_NS_SAMSOK_PRES + "\""); 
		writer.println("                 sort=\"false\" retrieve=\"true\">");
		writer.println("              <title " + langSV + ">Presentation K-Samsök</title>");
		writer.println("           </schema>");
		writer.println("        </schemaInfo>");

		writer.println("        <configInfo>");
		writer.println("           <default type=\"numberOfRecords\">" + DEFAULT_NUM_RECORDS + "</default>");
		writer.println("           <default type=\"contextSet\">" + ContentHelper.CONTEXT_SET_SAMSOK + "</default>");
		writer.println("           <default type=\"index\">" + ContentHelper.IX_TEXT + "</default>");
		writer.println("           <default type=\"retrieveSchema\">" + NS_SAMSOK_PRES + "</default>");
		writer.println("           <setting type=\"maximumRecords\">" + MAX_NUM_RECORDS + "</setting>");
		writer.println("           <setting type=\"recordPacking\">xml</setting>");
		writer.println("           <supports type=\"scan\"/>");
		writer.println("        </configInfo>");

		writer.println("      </explain>");

		writer.println("    </srw:recordData>");
		writer.println("  </srw:record>");
		writer.println("</srw:explainResponse>");
	}

	// hantera ett scan-anrop
	private void handleScan(Map<String, String> reqParams, PrintWriter writer) throws ServletException, IOException {
		writer.println("<srw:scanResponse xmlns:srw=\"" + NS_SRW + "\">");
		writer.println("  <srw:version>" + SRU_VERSION + "</srw:version>");
		try {
			handleScanOp(reqParams, writer);
		} finally {
			writer.println("</srw:scanResponse>");
		}
	}

	private void handleScanOp(Map<String, String> reqParams, PrintWriter writer) throws ServletException, IOException {

		String version = reqParams.get("version");
		String scanClause = reqParams.get("scanClause");
		String responsePosition = reqParams.get("responsePosition");
		String maximumTerms = reqParams.get("maximumTerms");
		//String extra_request_data = request.getParameter("extraRequestData");

		// TODO: stöd max + responsepos
		// kontrollera versionsparametern
		if (!checkVersion(writer, version, false)) {
			return;
		}

		IndexSearcher s = null;
		CQLParser cqlParser = new CQLParser();
		if (scanClause != null && scanClause.length() > 0) {
			try {
				s = LuceneServlet.getInstance().borrowIndexSearcher();
				CQLNode rootNode = cqlParser.parse(scanClause);
				if (!(rootNode instanceof CQLTermNode)) {
					throw new DiagnosticsException(48, "Query feature unsupported",
		        			"Scan query must be on the form: index relation term");
				}
				CQL2Lucene.dumpQueryTree(rootNode);
				CQLTermNode termNode = (CQLTermNode) rootNode;
				// hämta ut relationen
				String rel = termNode.getRelation().toCQL();
				// kontrollera att relationen är giltig
				final String[] validRels = { "=", "any" }; // "exact", "all" };
				final List<String> validRelsList = Arrays.asList(validRels);
				if (!validRelsList.contains(rel)) {
					throw new DiagnosticsException(19, "Unsupported relation",
        			rel + " is unsupported for the scan operation");
					
				}
				String index = CQL2Lucene.translateIndexName(termNode.getIndex());
				if (ContentHelper.isSpatialVirtualIndex(index)) {
					throw new DiagnosticsException(4, "Unsupported operation",
							"scan not supported for index " + index);
				}
				Query q = CQL2Lucene.makeQuery(rootNode);
				// TODO: är det här ok med alla relationstyper vi godkänner ovan?
				//       för exact ska vi kanske returnera fältinnehållet?
				//       typ:
				//		TermDocs td = s.getIndexReader().termDocs(t);
				//		final MapFieldSelector fieldSelector = new MapFieldSelector(new String[] { searchField });
				//		while (td.next()) {
				//			Document d = s.doc(td.doc(), fieldSelector);
				//			Field field = d.getField(searchField); // kan bli null för ej lagrade fält
				//			CQL2Lucene.countTerm(termMap, field.stringValue());
				//		}
				//		td.close();

				Query rq = q.rewrite(s.getIndexReader());
				Set<Term> extractedTermSet = new HashSet<Term>();
				rq.extractTerms(extractedTermSet);
				writer.println("  <srw:terms>");
				for (Term t: extractedTermSet) {
			        writer.println("     <srw:term>");
			        writer.println("        <srw:value>" + xmlEscape(t.text()) + "</srw:value>");
			        writer.println("        <srw:numberOfRecords>" + s.docFreq(t) + "</srw:numberOfRecords>");
			        writer.println("     </srw:term>");
				}
				writer.println("  </srw:terms>");
	
				writer.println("  <srw:echoedScanRequest>");
				writer.println("    <srw:version>"+version+"</srw:version>");
				writer.println("    <srw:scanClause>"+xmlEscape(scanClause)+"</srw:scanClause>");
				if (responsePosition != null) {
					writer.println("    <srw:responsePosition>"+xmlEscape(responsePosition)+"</srw:responsePosition>");
				}
				if (maximumTerms != null) {
					writer.println("    <srw:maximumTerms>"+xmlEscape(maximumTerms)+"</srw:maximumTerms>");
				}
				writer.println("  </srw:echoedScanRequest>");
			} catch (CQLParseException e) {
				diagnostics(writer, 10, "Query syntax error", e.getMessage());
				if (logger.isDebugEnabled()) {
					logger.debug("Fel vid parse i scan", e);
				}
			} catch (DiagnosticsException de) {
				diagnostics(writer, de.getErrorKey(), de.getMessage(), de.getDetails());
				if (logger.isDebugEnabled()) {
					logger.debug("Diagnostic-fel i scan", de);
				}
			} catch (TooManyClauses e) {
				// wildcard som ger för många or-villkor
				diagnostics(writer, 38, "Too many boolean operators in query", String.valueOf(BooleanQuery.getMaxClauseCount()));
				if (logger.isDebugEnabled()) {
					logger.debug("Wildcard gav upphov till för många villkor i scan", e);
				}
			} catch (Exception e) {
				logger.error("Fel vid scan", e);
				diagnostics(writer,e);
			} finally {
				LuceneServlet.getInstance().returnIndexSearcher(s);
			}
		} else {
			diagnostics(writer, 7, "Mandatory parameter not supplied", "scanClause");
		}
	}

	// hantera ett sök-anrop
	private void handleSearchRetrieve(Map<String, String> reqParams, PrintWriter writer) throws ServletException, IOException {
		writer.println("<srw:searchRetrieveResponse xmlns:srw=\"" + NS_SRW + "\">");
		writer.println("  <srw:version>" + SRU_VERSION + "</srw:version>");
		try {
			handleSearchRetrieveOp(reqParams, writer);
		} finally {
			writer.println("</srw:searchRetrieveResponse>");
		}
	}

	private void handleSearchRetrieveOp(Map<String, String> reqParams, PrintWriter writer) throws ServletException, IOException {
		String version = reqParams.get("version");
		String query = reqParams.get("query");
		String start_record = reqParams.get("startRecord");
		String maximum_records = reqParams.get("maximumRecords");
		String record_schema = reqParams.get("recordSchema");
		String record_packing = reqParams.get("recordPacking");

		// specialfall tills vidare för sortering istället för att införa
		// generell sortering då detta är enklare
		boolean europeanaSort = "true".equals(reqParams.get("x-europeana-sort"));
		/*
		String record_xpath = request.getParameter("recordXPath");
		String result_set_ttl = request.getParameter("resultSetTTL");
		String sort_keys = request.getParameter("sortKeys");
		String extra_request_data = request.getParameter("extraRequestData");
		*/
		
		int num_hits_per_page = DEFAULT_NUM_RECORDS;
		int first_record = 1;

		// TODO: flytta upp parameterkontroller till yttre metod

		// kontrollera versionsparametern
		if (!checkVersion(writer, version, true)) {
			return;
		}
			
		if (maximum_records != null) {
			num_hits_per_page = Integer.parseInt(maximum_records);
			if (num_hits_per_page < 0 || num_hits_per_page > MAX_NUM_RECORDS) {
				diagnostics(writer, 6, "Unsupported parameter value", "maximumRecords", true);
				return;
			}
		}

		if (start_record != null) {
			first_record = Integer.parseInt(start_record);
			if (first_record <= 0) {
				diagnostics(writer, 61, "First record position out of range", true);
				return;
			}
		}

		// default schema
		if (record_schema == null) {
			record_schema = NS_SAMSOK_PRES;
		}

		if (!knownSchemas.containsKey(record_schema)) {
			diagnostics(writer, 66, "Unknown schema for retrieval", record_schema, true);
			return;
		}
		record_schema = knownSchemas.get(record_schema);

		// vi kan bara skicka xml fn
		if (record_packing != null) {
			if (!"xml".equalsIgnoreCase(record_packing)) {
				diagnostics(writer, 71, "Unsupported record packing", true);
				return;
			}
		}

		boolean numberOfRecordsWritten = false;
		TopDocs hits = null;
		IndexSearcher s = null;
		CQLParser cqlParser = new CQLParser();

		if (query != null && query.length() > 0) {
			try {
				s = LuceneServlet.getInstance().borrowIndexSearcher();
				CQLNode rootNode = cqlParser.parse(query);
				CQL2Lucene.dumpQueryTree(rootNode);
				Query q = CQL2Lucene.makeQuery(rootNode);
				int nDocs = first_record - 1 + num_hits_per_page;
				if (q != null) {
					if (europeanaSort) {
						// specialsortering för europeana på lucenes interna dokument-id för att
						// alltid ha en konsekvent sorteringsordning - flera dokument kan ha samma
						// score och då kan det vara odefinierat vilken av dem som kommer först
						// vilket kan ge samma post om en sidobrytpunkt är just där.
						// detta är special istället för att införa sortering generellt
						hits = s.search(q, null, nDocs == 0 ? 1 : nDocs, new Sort(SortField.FIELD_DOC));
					} else {
						// måste ha minst en träff
						hits = s.search(q, nDocs == 0 ? 1 : nDocs);
					}
				} else {
					// ingen query men inget fel, ge då 0 träffar
					hits = new TopDocs(0, null, 0);
				}
				if (hits.totalHits > 0 && hits.totalHits < first_record) {
					diagnostics(writer, 61, "First record position out of range", true);
					return;
				}
				writer.println("  <srw:numberOfRecords>" + hits.totalHits + "</srw:numberOfRecords>");
				numberOfRecordsWritten = true;
				int last_record = 0;

				// hämta rätt xml
				String dataIndex = NS_SAMSOK_PRES.equals(record_schema) ? ContentHelper.I_IX_PRES : ContentHelper.I_IX_RDF;
				// TODO: flytta ut som statiska
				// TODO: bort med lon och lat då de bara används för debug
				final String[] fieldNames = {
						ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER,
						dataIndex, ContentHelper.I_IX_LON, ContentHelper.I_IX_LAT};
				final MapFieldSelector fieldSelector = new MapFieldSelector(fieldNames);

				if (nDocs > 0 && hits.totalHits > 0 ) {
					writer.println("  <srw:records>");
					String content;
					String uri;
					for (int i = first_record - 1; i < hits.totalHits && i < first_record -1 + num_hits_per_page; ++i) {
						content = null; // rensa värde
						// TODO: fler och bättre felkontroller
						// TODO: stödja fler format?
						ScoreDoc scoreDoc = hits.scoreDocs[i];
						double score = scoreDoc.score;
						Document doc = s.doc(scoreDoc.doc, fieldSelector);
						uri = doc.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
						// TODO: ta bort?
						if (logger.isDebugEnabled()) {
							String lon = doc.get(ContentHelper.I_IX_LON);
							String lat = doc.get(ContentHelper.I_IX_LAT);
							logger.debug("hit - uri: " + uri + " -> " +
									"lon=" + (lon != null ? NumberUtils.SortableStr2double(lon) : "??") +
									" lat=" + (lat != null ? NumberUtils.SortableStr2double(lat) : "??"));
						}
						byte[] xmlData = doc.getBinaryValue(dataIndex);
						if (xmlData != null) {
							content = new String(xmlData, "UTF-8");
						}
						// TODO: NEK ta bort nedan när allt är omindexerat
						if (content == null && ContentHelper.I_IX_RDF.equals(dataIndex)) {
							content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(uri); 
						}
						if (content != null) {

							writer.println("  <srw:record>");
							writer.println("     <srw:recordSchema>" + xmlEscape(record_schema) + "</srw:recordSchema>");
							writer.println("     <srw:recordPacking>xml</srw:recordPacking>");
							writer.println("     <srw:recordData>" + content + "</srw:recordData>");
							writer.println("     <srw:extraRecordData>");
							writer.println("        <rel:score xmlns:rel=\"info:srw/extension/2/relevancy-1.0\">" + score + "</rel:score>");
							writer.println("     </srw:extraRecordData>");
							writer.println("  </srw:record>");
						} else {
							logger.warn("Hittade inte xmldata (" + dataIndex + ") för " + uri);
							// skriv ut en diagnostic-post istället
							writer.println("  <srw:record>");
							writer.println("     <srw:recordSchema>info:srw/schema/1/diagnostics-v1.1</srw:recordSchema>");
							writer.println("     <srw:recordPacking>xml</srw:recordPacking>");
							writer.println("     <srw:recordData>");
							diagnostic(writer, 64, "Record temporarily unavailable", null);
							writer.println("     </srw:recordData>");
							writer.println("  </srw:record>");
						}
						last_record = i + 1;
					}
					writer.println("  </srw:records>");
				}

				if (last_record < hits.totalHits && last_record > 0) {
					writer.println("  <srw:nextRecordPosition>" + (last_record+1) + "</srw:nextRecordPosition>");
				}

				writer.println("  <srw:echoedSearchRetrieveRequest>");
				writer.println("    <srw:version>"+version+"</srw:version>");
				writer.println("    <srw:query>"+ xmlEscape(query)+"</srw:query>");
				if (start_record != null) {
					writer.println("    <srw:startRecord>"+xmlEscape(start_record)+"</srw:startRecord>");
				}
				if (maximum_records != null) {
					writer.println("    <srw:maximumRecords>"+xmlEscape(maximum_records)+"</srw:maximumRecords>");
				}
				writer.println("    <srw:recordSchema>"+xmlEscape(record_schema)+"</srw:recordSchema>");
				writer.println("  </srw:echoedSearchRetrieveRequest>");
			} catch (CQLParseException e) {
				diagnostics(writer, 10, "Query syntax error", e.getMessage(), !numberOfRecordsWritten);
				if (logger.isDebugEnabled()) {
					logger.debug("Fel vid parse i searchRetrieve", e);
				}
			} catch (DiagnosticsException de) {
				diagnostics(writer, de.getErrorKey(), de.getMessage(), de.getDetails(), !numberOfRecordsWritten);
				if (logger.isDebugEnabled()) {
					logger.debug("Diagnostic-fel i searchRetrieve", de);
				}
			} catch (TooManyClauses e) {
				// wildcard som ger för många or-villkor
				diagnostics(writer, 38, "Too many boolean operators in query", String.valueOf(BooleanQuery.getMaxClauseCount()), !numberOfRecordsWritten);
				if (logger.isDebugEnabled()) {
					logger.debug("Wildcard gav upphov till för många villkor i searchRetrieve", e);
				}
			} catch ( Exception e ) {
				logger.error("Fel vid searchRetrieve", e);
				diagnostics(writer,e, !numberOfRecordsWritten);
			} finally {
				LuceneServlet.getInstance().returnIndexSearcher(s);
			}
		} else {
			diagnostics(writer, 7, "Mandatory parameter not supplied", "query", true);
		}
	}

	// hjälpmetoder för att skriva diagnostics
	private void diagnostics(PrintWriter writer, int error_key, String message) {
		diagnostics(writer, error_key, message, null, false, true);
	}
	private void diagnostics(PrintWriter writer, int error_key, String message, boolean numRecords) {
		diagnostics(writer, error_key, message, null, numRecords);
	}
	private void diagnostics(PrintWriter writer, int error_key, String message, String details) {
		diagnostics(writer, error_key, message, details, false);
	}
	private void diagnostics(PrintWriter writer, int error_key, String message, String details, boolean numRecords) {
		diagnostics(writer, error_key, message, details, numRecords, false);
	}
	private void diagnostics(PrintWriter writer, int error_key, String message, String details, boolean numRecords, boolean srwNs) {
		if (numRecords) {
			// numberOfRecords är obligatoriskt för searchRetrieve
			writer.println("  <srw:numberOfRecords>0</srw:numberOfRecords>");
		}
		writer.println("<srw:diagnostics" + (srwNs ? " xmlns:srw=\"" + NS_SRW + "\"" : "") + ">");
		diagnostic(writer, error_key, message, details);
		writer.println("</srw:diagnostics>");
	}

	private void diagnostic(PrintWriter writer, int error_key, String message, String details) {
		writer.println("<diagnostic xmlns=\"http://www.loc.gov/zing/srw/diagnostic/\">");
		writer.println("<uri>info:srw/diagnostic/1/"+error_key+"</uri>");
		if (details != null) {
			writer.println("<details>" + xmlEscape(details) + "</details>");
		}
		writer.println("<message>");
		writer.println(message);
		writer.println("</message>");
		writer.println("</diagnostic>");
	}

	private void diagnostics(PrintWriter writer, Throwable t) {
		diagnostics(writer, t, false);
	}

	private void diagnostics(PrintWriter writer, Throwable t, boolean numRecords) {
		// DiagnosticsException och CQLParseException kanske tas om hand i respektive operation
		// men vi har en kontroll här också
		if (t instanceof DiagnosticsException) {
			DiagnosticsException de = (DiagnosticsException) t;
			diagnostics(writer, de.getErrorKey(), de.getMessage(), de.getDetails(), numRecords);
			return;
		}
		if (t instanceof CQLParseException) {
			// ska inte ha en detail egentligen men...
			diagnostics(writer, 10, "Query syntax error", t.getMessage(), numRecords);
			return;
		}
		if (numRecords) {
			// numberOfRecords är obligatoriskt för searchRetrieve
			writer.println("  <srw:numberOfRecords>0</srw:numberOfRecords>");
		}
		writer.println("<srw:diagnostics>");
		writer.println("   <diagnostic xmlns=\"http://www.loc.gov/zing/srw/diagnostic/\">");
		writer.println("   <uri>info:srw/diagnostic/1/1</uri>");
		// writer.println("<details>10</details>");
		writer.println("   <message><![CDATA[");
		t.printStackTrace(writer);
		writer.println("]]></message>");
		writer.println("   </diagnostic>");
		writer.println("</srw:diagnostics>");
	}

	// kontroll av sru-version
	private boolean checkVersion(PrintWriter writer, String version, boolean numRecords) {
		if (version == null) {
			diagnostics(writer, 7, "Mandatory parameter not supplied", "version", numRecords);
			return false;
		}
		// vi stödjer bara 1.1 fn
		if (!SRU_VERSION.equals(version)) {
			diagnostics(writer, 5, "Unsupported version", SRU_VERSION, numRecords);
			return false;
		}
		return true;
	}

	// grundläggande xml-escape
	private static String xmlEscape(String s) {
		// TODO: inte helt 100
		//return s.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
		return StringEscapeUtils.escapeXml(s);
	}

}
