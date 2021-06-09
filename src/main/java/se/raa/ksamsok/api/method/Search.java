package se.raa.ksamsok.api.method;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RiotException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hanterar sökningar efter objekt
 * 
 * @author Henrik Hjalmarsson
 */
public class Search extends AbstractSearchMethod {
	/** standardvärdet för antalet träffar per sida */
	public static final int DEFAULT_HITS_PER_PAGE = 50;
	/** metodnamn som anges för att använda denna klass */
	public static final String METHOD_NAME = "search";
	/** parameternamn för sort */
	public static final String SORT = "sort";
	/** parameternamn för sort configuration */
	public static final String SORT_CONFIG = "sortConfig";
	/** parametervärde för descending sort */
	public static final String SORT_DESC = "desc";
	/** parametervärde för ascending sort */
	public static final String SORT_ASC = "asc";
	/** parameternam för sort */
	public static final String FIELDS = "fields";
	/** record schema för presentations data */
	public static final String NS_SAMSOK_PRES = "http://kulturarvsdata.se/presentation#";
	/** record schema för valbara fält (xml) */
	public static final String NS_SAMSOK_XML = "http://kulturarvsdata.se/xml#";
	/** record schema (rdf) */
	public static final String NS_SAMSOK_RDF = "http://kulturarvsdata.se/rdf#";

	/** parameternamn för record schema */
	public static final String RECORD_SCHEMA = "recordSchema";
	/** bas URL till record schema */
	public static final String RECORD_SCHEMA_BASE = "http://kulturarvsdata.se/";

	// index att använda för sortering (transparent) istället för itemName
	private static final String ITEM_NAME_SORT = "itemNameSort";

	// TODO: detta är inte det mest effektiva sättet att få ut valbara fält
	// bättre och snabbare vore att lagra fälten i solr och hämta därifrån,
	// men detta är snabbare att implementera och kräver inte med disk för solr-indexet
	// specialvärden/variabler för valbara fält
	private static final String FIELD_URL = "url";
	private static final String FIELD_LON = "lon";
	private static final String FIELD_LAT = "lat";
	// återanvänd samma kod som används för indexering
	private static final SamsokContentHelper sch = new SamsokContentHelper(false);
	// specialhanterade fält som antingen kräver extra hantering eller som inte blir vettiga
	private static final List<String> extraFields = Collections.unmodifiableList(
		Arrays.asList(FIELD_LON, FIELD_LAT, FIELD_URL));
	private static final List<String> disallowedFields = Collections.unmodifiableList(
		Arrays.asList(ContentHelper.IX_ADDEDTOINDEXDATE, // blir inte rätt beräknat med dummy-tjänst
			ContentHelper.IX_BOUNDING_BOX, // bara för sök
			ContentHelper.IX_POINT_DISTANCE // bara för sök
		// TODO: fler?
		));
	// SamsokContentHelper.createSolrDocument() kräver en tjänst så vi skapar en dummy
	private static final HarvestService dummyService;
	static {
		dummyService = new HarvestServiceImpl();
		dummyService.setId("dummy");
		dummyService.setName("dummy");
	}

	private static final Logger logger = LogManager.getLogger(Search.class);

	protected String sort = null;
	protected boolean sortDesc = false;
	protected String recordSchema = null;
	protected String binDataField = null;
	protected Set<String> fields = null;

	/**
	 * skapar ett Search objekt
	 * 
	 * @param params sökparametrar
	 * @param hitsPerPage träffar som skall visas per sida
	 * @param startRecord startposition i sökningen
	 * @param out skrivaren som skall användas för att skriva svaret
	 * @throws DiagnosticException TODO
	 */
	public Search(APIServiceProvider serviceProvider, OutputStream out, Map<String, String> params)
		throws DiagnosticException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException, BadParameterException {
		super.extractParameters();
		sort = params.get(Search.SORT);
		if (sort != null) {
			if (!ContentHelper.indexExists(sort)) {
				throw new BadParameterException("Sorteringsindexet " + sort + " finns inte.", "Search.performMethod",
					null, false);
			} else if (!ContentHelper.indexSortable(sort)) {
				throw new BadParameterException("Indexet " + sort + " kan inte användas för sortering.",
					"Search.performMethod", null, false);
			}
			// TODO: generalisera, lägga i konf-fil?
			// specialhantering för sortering på itemName, istället används itemNameSort
			// transparent som rensar itemName och behåller bara bokstäver och siffor - fix
			// för att tex poster med citationstecken ("konstiga" tecken) kom först
			if (ContentHelper.IX_ITEMNAME.equals(sort)) {
				sort = ITEM_NAME_SORT;
			}
		}
		sortDesc = getSortConfig(params.get(Search.SORT), params.get(Search.SORT_CONFIG));
		recordSchema = params.get(Search.RECORD_SCHEMA);
		if (recordSchema != null) {
			recordSchema = RECORD_SCHEMA_BASE + recordSchema + "#";
		}
		if (NS_SAMSOK_PRES.equals(recordSchema)) {
			binDataField = ContentHelper.I_IX_PRES;
		} else if (NS_SAMSOK_XML.equals(recordSchema)) {
			// valbara fält, använd rdf
			binDataField = ContentHelper.I_IX_RDF;
			String reqFields = getMandatoryParameterValue(FIELDS, "Search", null);
			String[] splitFields = StringUtils.split(reqFields, ",");
			if (splitFields == null || splitFields.length == 0) {
				throw new BadParameterException("Inga efterfrågade fält.", "Search.performMethod", null, false);
			}
			fields = new LinkedHashSet<>();
			// ta alltid med itemId så att man vet vilken post det är
			fields.add(ContentHelper.IX_ITEMID);
			for (String field : splitFields) {
				field = StringUtils.trimToNull(field);
				// godkänn bara fält/index som finns, är ej interna och ev extra specialhanterade
				// fält
				if (field != null && !disallowedFields.contains(field) && !field.startsWith("_") &&
					(ContentHelper.indexExists(field) || extraFields.contains(field))) {
					fields.add(field);
				} else {
					throw new BadParameterException(
						"Det efterfrågade fältet/indexet " + field + " finns inte eller stöds inte.",
						"Search.performMethod", null, false);
				}
			}
		} else if (recordSchema == null || NS_SAMSOK_RDF.equals(recordSchema)) {
			binDataField = ContentHelper.I_IX_RDF;
		} else {
			throw new BadParameterException(
				"Det efterfrågade recordSchema " + recordSchema + " finns inte eller stöds inte.",
				"Search.performMethod", null, false);
		}

	}

	@Override
	protected int getDefaultHitsPerPage() {
		return DEFAULT_HITS_PER_PAGE;
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		try {
			SolrQuery query = createQuery();
			// start är 0-baserad
			query.setStart(startRecord - 1);
			query.setRows(hitsPerPage);
			if (sort != null) {
				query.addSort(sort, sortDesc ? ORDER.desc : ORDER.asc);
			}
			query.addField(ContentHelper.IX_ITEMID);
			query.addField("score"); // score är "solr-special" för uhm, score...
			// ta fram rätt data
			query.addField(binDataField);
			QueryResponse qr = serviceProvider.getSearchService().query(query);
			hitList = qr.getResults();
		} catch (SolrServerException | IOException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod. Var god försök igen", "Search.performMethod",
				e.getMessage(), true);
		} catch (BadParameterException e) {
			throw new DiagnosticException(e.getMessage(), "Search.performMethod", e.getMessage(), true);
		}
	}

	@Override
	protected void generateDocument() throws DiagnosticException {
		// Always create a xml document unless the accept format is json and record schema is not
		// set.
		// If this is the case then should the result be a json with json-ld rdfs. The method
		// xmlToJson does not creates json-ld
		if (format != Format.JSON_LD || (recordSchema != null && !NS_SAMSOK_RDF.equals(recordSchema))) {
			Element result = super.generateBaseDocument();

			Element totalHits = doc.createElement("totalHits");
			totalHits.appendChild(doc.createTextNode(Long.toString(hitList.getNumFound(), 10)));
			result.appendChild(totalHits);

			Element records = doc.createElement("records");
			for (SolrDocument d : hitList) {
				Float score = (Float) d.getFieldValue("score");
				String ident = (String) d.getFieldValue(ContentHelper.IX_ITEMID);
				String content = getContent(d, ident);
				if (content != null) {
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder;
					try {
						docBuilder = docFactory.newDocumentBuilder();
						Document contentDoc = docBuilder.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
						NodeList childNodes;
						if (contentDoc.getFirstChild().getNodeName().equals("recordSchema")) {
							childNodes = contentDoc.getFirstChild().getChildNodes();
						} else {
							childNodes = contentDoc.getChildNodes();
						}
						if (childNodes.getLength() > 0) {
							Element record = doc.createElement("record");
							for (int i = 0; i < childNodes.getLength(); i++) {
								// Import all child nodes from rdf document to result document
								Node imp = doc.importNode(childNodes.item(i), true);
								record.appendChild(imp);
							}
							Element relScore = doc.createElement("rel:score");
							relScore.setAttribute("xmlns:rel", "info:srw/extension/2/relevancy-1.0");
							relScore.appendChild(doc.createTextNode(Float.toString(score)));
							record.appendChild(relScore);
							records.appendChild(record);
						}
					} catch (ParserConfigurationException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att initiera xml dokument hanteraren",
							AbstractAPIMethod.class.getName(), e.getMessage(), false);
					} catch (IOException e) {
						logger.error(e);
						throw new DiagnosticException("Det är problem med att konvertera en sträng till output ström",
							AbstractAPIMethod.class.getName(), e.getMessage(), false);
					} catch (SAXException e) {
						logger.error("Kontent som ska konverteras till ett xml-dokument: " + content);
						logger.error(e);
						throw new DiagnosticException(
							"Det är problem med att konvertera en sträng till ett xml-dokument",
							AbstractAPIMethod.class.getName(), e.getMessage(), false);
					}
				}
			}
			result.appendChild(records);

			Element echo = doc.createElement("echo");
			result.appendChild(echo);

			Element method = doc.createElement("method");
			method.appendChild(doc.createTextNode(METHOD_NAME));
			echo.appendChild(method);
			if (params.containsKey(Search.RECORD_SCHEMA)) {
				Element recordSchemaEl = doc.createElement(Search.RECORD_SCHEMA);
				recordSchemaEl.appendChild(doc.createTextNode(params.get(Search.RECORD_SCHEMA)));
				echo.appendChild(recordSchemaEl);
			}
			if (params.containsKey("fields") && NS_SAMSOK_XML.equals(recordSchema)) {
				for (String field : fields) {
					Element fieldEl = doc.createElement("fields");
					fieldEl.appendChild(doc.createTextNode(field));
					echo.appendChild(fieldEl);
				}
			}

			Element startRecordEl = doc.createElement("startRecord");
			startRecordEl.appendChild(doc.createTextNode(Integer.toString(startRecord, 10)));
			echo.appendChild(startRecordEl);

			Element hitsPerPageEl = doc.createElement("hitsPerPage");
			hitsPerPageEl.appendChild(doc.createTextNode(Integer.toString(hitsPerPage, 10)));
			echo.appendChild(hitsPerPageEl);

			Element query = doc.createElement("query");
			query.appendChild(doc.createTextNode(originalQueryString));
			echo.appendChild(query);
		}
	}

	@Override
	protected void writeResult() throws DiagnosticException {
		if (format != Format.JSON_LD || (recordSchema != null && !NS_SAMSOK_RDF.equals(recordSchema))) {
			super.writeResult();
		} else {
			String content = "";
			try {
				JSONArray records = new JSONArray();
				for (SolrDocument d : hitList) {
					try {
						Float score = (Float) d.getFieldValue("score");
						String ident = (String) d.getFieldValue(ContentHelper.IX_ITEMID);
						content = getContent(d, ident);
						if (content != null) {
							JSONObject record = new JSONObject();
							ByteArrayOutputStream jsonLDRDF = new ByteArrayOutputStream();
							Model m = ModelFactory.createDefaultModel();

							m.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), "UTF-8");

							// Create JSON-LD
							RDFDataMgr.write(jsonLDRDF, m, RDFFormat.JSONLD_COMPACT_FLAT);
							record.put("record", new JSONObject(jsonLDRDF.toString("UTF-8")));
							JSONObject relScore = new JSONObject();
							relScore.put("-xmlns:rel", "info:srw/extension/2/relevancy-1.0");
							relScore.put("#text", score);
							record.put("rel:score", relScore);
							records.put(record);
						}
					} catch (RiotException e) {
						logger.error("Kan inte läsa rdf för " + content + e.getMessage());
					}
				}
				// Create echo object
				JSONObject echo = new JSONObject();
				echo.put("method", METHOD_NAME);
				echo.put("startRecord", startRecord);
				echo.put("hitsPerPage", hitsPerPage);
				echo.put("query", queryString);
				// Create the result object
				JSONObject result = new JSONObject();
				result.put("version", API_VERSION);
				result.put("totalHits", hitList.getNumFound());
				result.put("records", records);
				result.put("echo", echo);
				JSONObject response = new JSONObject();
				response.put("result", result);
				// Write the result
				out.write(response.toString().getBytes(StandardCharsets.UTF_8));

			} catch (UnsupportedEncodingException e) {
				logger.error(e);
				throw new DiagnosticException("Det är problem med att konvertera en sträng till output ström",
					AbstractAPIMethod.class.getName(), e.getMessage(), false);
			} catch (JSONException e) {
				logger.error("Kontent som ska konverteras till ett json objekt: " + content);
				logger.error(e);
				throw new DiagnosticException("Det är problem med att skapa en json från resultatet",
					AbstractAPIMethod.class.getName(), e.getMessage(), false);
			} catch (IOException e) {
				logger.error(e);
				throw new DiagnosticException("Det är problem med att skriva resultatet till utströmmen",
					this.getClass().getName(), e.getMessage(), false);
			}
		}
	}

	/**
	 * Hämtar xml-innehåll (fragment) från ett lucene-dokument som en sträng.
	 * 
	 * @param doc solrdokument
	 * @param uri postens uri (används bara för log)
	 * @return xml-fragment med antingen presentations-xml, rdf eller xml med valbara fält; null om
	 *         data saknas
	 * @throws Exception vid teckenkodningsfel (bör ej inträffa)
	 */
	protected String getContent(SolrDocument doc, String uri) {
		String content = null;
		// Hämta ut dokumentet från solr
		byte[] xmlData = (byte[]) doc.getFieldValue(binDataField);

		try {
			if (xmlData != null) {
				content = new String(xmlData, StandardCharsets.UTF_8);
			}
			if (content == null) {
				logger.warn("Hittade inte xml-data (" + binDataField + ") för " + uri);
			} else if (NS_SAMSOK_XML.equals(recordSchema)) {
				// Filtrera ut den info du vill ha
				SolrInputDocument resDoc = sch.createSolrDocument(dummyService, content, new Date());
				// nödvändigt då createSolrDocument lägger in felmeddelanden mm
				ContentHelper.getAndClearProblemMessages();
				content = "";
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder;
				Document contentDoc;
				Element recordSchema;
				try {
					docBuilder = docFactory.newDocumentBuilder();
					contentDoc = docBuilder.newDocument();
					recordSchema = contentDoc.createElement("recordSchema");
					contentDoc.appendChild(recordSchema);
				} catch (ParserConfigurationException e) {
					logger.error(e);
					throw new DiagnosticException("Det är problem med att initiera xml dokument hanteraren",
						this.getClass().getName(), e.getMessage(), false);
				}
				for (String field : fields) {
					String docField;
					// översätt fält vid behov
					if (FIELD_LON.equals(field)) {
						docField = ContentHelper.I_IX_LON;
					} else if (FIELD_LAT.equals(field)) {
						docField = ContentHelper.I_IX_LAT;
					} else if (FIELD_URL.equals(field)) {
						docField = ContentHelper.I_IX_HTML_URL;
					} else {
						docField = field;
					}
					Collection<Object> fieldValues = resDoc.getFieldValues(docField);
					if (fieldValues != null) {
						String fieldValue;
						for (Object value : fieldValues) {
							if (value != null && (fieldValue = StringUtils.trimToNull(value.toString())) != null) {
								Element fieldEl = contentDoc.createElement("field");
								fieldEl.setAttribute("name", field);
								fieldEl.appendChild(contentDoc.createTextNode(fieldValue));
								recordSchema.appendChild(fieldEl);
							}
						}
					}
				}
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transform;
				try {
					transform = transformerFactory.newTransformer();
					DOMSource source = new DOMSource(contentDoc);
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					transform.transform(source, new StreamResult(baos));
					content = baos.toString("UTF-8");
				} catch (TransformerException e) {
					logger.error(e);
					throw new DiagnosticException("Det är problem med att initiera xml konverteraren",
						this.getClass().getName(), e.getMessage(), false);
				}
			}
		} catch (Exception e) {
			logger.error(e);
			logger.error("Fel vid hämtande av xml-data (" + binDataField + ") för " + uri);
			content = null;
		}
		return content;
	}

	/**
	 * Skapar ett query
	 * 
	 * @return query
	 */
	protected SolrQuery createQuery() throws DiagnosticException, BadParameterException {
		SolrQuery query = null;
		try {
			CQLParser parser = new CQLParser();
			CQLNode rootNode = parser.parse(queryString);
			String solrQueryString = CQL2Solr.makeQuery(rootNode);
			if (solrQueryString != null) {
				query = new SolrQuery(solrQueryString);
			}
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod. Var god försök igen", "Search.createQuery",
				e.getMessage(), true);
		} catch (CQLParseException e) {
			throw new DiagnosticException(
				"Parserfel uppstod. Detta beror troligen på att query-strängen inte följer CQL syntax. Var god kontrollera söksträngen eller kontakta systemadministratör för söksystemet du använder",
				"Search.createQuery", e.getMessage(), false);
		}
		return query;
	}

	/**
	 * returnerar true om sortConfig är satt till "desc"
	 * 
	 * @param sort
	 * @param sortConfig
	 * @return
	 */
	public boolean getSortConfig(String sort, String sortConfig) {
		boolean sortDesc = false;
		if (sort != null) {
			if (sortConfig != null && sortConfig.equals(Search.SORT_DESC)) {
				sortDesc = true;
			}
		}
		return sortDesc;
	}
}
