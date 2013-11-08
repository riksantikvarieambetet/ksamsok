package se.raa.ksamsok.api.method;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;
import org.z3950.zing.cql.CQLTermNode;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.SamsokContentHelper;
import se.raa.ksamsok.statistic.StatisticLoggData;
import se.raa.ksamsok.util.ShmSiteCacherHackTicket3419;

/**
 * Hanterar sökningar efter objekt
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
	public static final String NS_SAMSOK_PRES =	"http://kulturarvsdata.se/presentation#";
	/** record schema för valbara fält (xml) */
	public static final String NS_SAMSOK_XML =	"http://kulturarvsdata.se/xml#";
	/** parameternamn för record schema */
	public static final String RECORD_SCHEMA = "recordSchema";
	/** bas URL till record schema */
	public static final String RECORD_SCHEMA_BASE = "http://kulturarvsdata.se/";

	// index att använda för sortering (transparent) istället för itemName
	private static final String ITEM_NAME_SORT = "itemNameSort";

	// TODO: detta är inte det mest effektiva sättet att få ut valbara fält
	//       bättre och snabbare vore att lagra fälten i solr och hämta därifrån,
	//       men detta är snabbare att implementera och kräver inte med disk för solr-indexet
	// specialvärden/variabler för valbara fält	
	private static final String FIELD_THUMBNAIL = "thumbnail";
	private static final String FIELD_URL = "url";
	private static final String FIELD_LON = "lon";
	private static final String FIELD_LAT = "lat";
	// återanvänd samma kod som används för indexering
	private static final SamsokContentHelper sch = new SamsokContentHelper();
	// specialhanterade fält som antingen kräver extra hantering eller som inte blir vettiga
	private static final List<String> extraFields = Collections.unmodifiableList(
			Arrays.asList(FIELD_THUMBNAIL, FIELD_LON, FIELD_LAT, FIELD_URL));
	private static final List<String> disallowedFields = Collections.unmodifiableList(Arrays.asList(
			ContentHelper.IX_ADDEDTOINDEXDATE,  // blir inte rätt beräknat med dummy-tjänst
			ContentHelper.IX_BOUNDING_BOX,      // bara för sök
			ContentHelper.IX_POINT_DISTANCE     // bara för sök
			// TODO: fler?
	));
	// SamsokContentHelper.createSolrDocument() kräver en tjänst så vi skapar en dummy
	private static final HarvestService dummyService;
	static {
		dummyService = new HarvestServiceImpl();
		dummyService.setId("dummy");
		dummyService.setName("dummy");
	}

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.Search");

	protected String sort = null;
	protected boolean sortDesc = false;
	protected String recordSchema = null;
	protected String apiKey;
	protected String binDataField = null;
	protected Set<String> fields = null;

	/**
	 * skapar ett Search objekt
	 * @param params sökparametrar
	 * @param hitsPerPage träffar som skall visas per sida
	 * @param startRecord startposition i sökningen
	 * @param out skrivaren som skall användas för att skriva svaret
	 * @throws ParserConfigurationException 
	 */
	public Search(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws ParserConfigurationException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		super.extractParameters();
		this.apiKey = params.get(APIMethod.API_KEY_PARAM_NAME);
		sort = params.get(Search.SORT);
		if (sort != null) {
			if (!ContentHelper.indexExists(sort)) {
				throw new BadParameterException("Sorteringsindexet " + sort + " finns inte.", "Search.performMethod", null, false);
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
			String reqFields = getMandatoryParameterValue(FIELDS, "Search", null, false);
			String[] splitFields = StringUtils.split(reqFields, ",");
			if (splitFields == null || splitFields.length == 0) {
				throw new BadParameterException("Inga efterfrågade fält.", "Search.performMethod", null, false);
			}
			fields = new LinkedHashSet<String>();
			// ta alltid med itemId så att man vet vilken post det är
			fields.add(ContentHelper.IX_ITEMID);
			for (String field: splitFields) {
				field = StringUtils.trimToNull(field);
				// godkänn bara fält/index som finns, är ej interna och ev extra specialhanterade fält
				if (field != null && !disallowedFields.contains(field) && !field.startsWith("_") &&
						(ContentHelper.indexExists(field) || extraFields.contains(field))) {
					fields.add(field);
				} else {
					throw new BadParameterException("Det efterfrågade fältet/indexet " + field + " finns inte eller stöds inte.", "Search.performMethod", null, false);
				}
			}
		} else {
			binDataField = ContentHelper.I_IX_RDF;
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
				query.addSortField(sort, sortDesc ? ORDER.desc : ORDER.asc);
			}
			query.addField(ContentHelper.IX_ITEMID);
			query.addField("score"); // score är "solr-special" för uhm, score...
			// ta fram rätt data
			query.addField(binDataField);
			QueryResponse qr = serviceProvider.getSearchService().query(query);
			hitList = qr.getResults();
		} catch(SolrServerException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod. Var god försök igen", "Search.performMethod", e.getMessage(), true);
		} catch (BadParameterException e) {
			throw new DiagnosticException(e.getMessage(), "Search.performMethod", e.getMessage(), true);
		}
	}
	@Override
	protected void generateDocument() throws ParserConfigurationException, SAXException, IOException {
		Element result = super.generateBaseDocument();
		
		Element totalHits = doc.createElement("totalHits");
		totalHits.appendChild(doc.createTextNode(Long.toString(hitList.getNumFound(),10)));
		result.appendChild(totalHits);
		
		Element records = doc.createElement("records");
		for (SolrDocument d : hitList){
			Float score = (Float) d.getFieldValue("score");
			String ident = (String) d.getFieldValue(ContentHelper.IX_ITEMID);
			String content = getContent(d, ident);
			if (content!=null){
				Element record = doc.createElement("record");
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document contentDoc = docBuilder.parse(new ByteArrayInputStream(content.getBytes("UTF-8")));
				for (int i = 0; i < contentDoc.getChildNodes().getLength(); i++){
					Node imp = doc.importNode(contentDoc.getChildNodes().item(i),true);
					record.appendChild(imp);
					Element relScore = doc.createElement("rel:score");
					relScore.setAttribute("xmlns:rel", "info:srw/extension/2/relevancy-1.0");
					relScore.appendChild(doc.createTextNode(Float.toString(score)));
					record.appendChild(relScore);
				}
				records.appendChild(record);
			}
		}
		result.appendChild(records);
		
		Element echo = doc.createElement("echo");
		result.appendChild(echo);
		
		Element startRecordEl = doc.createElement("startRecord");
		startRecordEl.appendChild(doc.createTextNode(Integer.toString(startRecord,10)));
		echo.appendChild(startRecordEl);
		
		Element hitsPerPageEl = doc.createElement("hitsPerPage");
		hitsPerPageEl.appendChild(doc.createTextNode(Integer.toString(hitsPerPage, 10)));
		echo.appendChild(hitsPerPageEl);
		
		Element query = doc.createElement("query");
		query.appendChild(doc.createTextNode(queryString));
		echo.appendChild(query);
	}


	/**
	 * Hämtar xml-innehåll (fragment) från ett lucene-dokument som en sträng.
	 * @param doc solrdokument
	 * @param uri postens uri (används bara för log)
	 * @return xml-fragment med antingen presentations-xml, rdf eller xml med valbara fält; null om data saknas
	 * @throws Exception vid teckenkodningsfel (bör ej inträffa)
	 */
	protected String getContent(SolrDocument doc, String uri) {
		String content = null;
		byte[] xmlData = (byte[]) doc.getFieldValue(binDataField);
		try {
			// hämta ev från hack-cachen
			if (ShmSiteCacherHackTicket3419.useCache(params.get(ShmSiteCacherHackTicket3419.KRINGLA), uri)) {
				content = ShmSiteCacherHackTicket3419.getOrRecache(uri, xmlData);
			} else {
				if (xmlData != null) {
					content = new String(xmlData, "UTF-8");
				}
				// TODO: NEK: ta bort när allt är omindexerat
				if (content == null && !NS_SAMSOK_PRES.equals(recordSchema)) {
					content = serviceProvider.getHarvestRepositoryManager().getXMLData(uri);
				}
			}
			if (content == null) {
				logger.warn("Hittade inte xml-data (" + binDataField + ") för " + uri);
			}
			if (content != null && NS_SAMSOK_XML.equals(recordSchema)) {
				SolrInputDocument resDoc = sch.createSolrDocument(dummyService, content, new Date());
				// nödvändigt då createSolrDocument lägger in felmeddelanden mm
				ContentHelper.getAndClearProblemMessages();
				// (ful-)hämta ut tumnagel då den inte indexeras
				// alternativet är att parsa rdf:en och hämta ut den, men det skulle fn innebära
				// dubbelparsning av rdf:en och iom att detta med att gå via ett solr-dokument
				// redan är långsamt och troligen är en temporär lösning så får det bli så här
				// tills vidare (motsvarar hämtning av SamsokProtocol.uri_rThumbnail) och
				// förhoppningsvis funkar det för så gott som alla fall
				if (fields.contains(FIELD_THUMBNAIL)) {
					String thumb = StringUtils.substringBetween(content, "thumbnail>", "<");
					if (!StringUtils.isEmpty(thumb)) {
						resDoc.addField(FIELD_THUMBNAIL, thumb);
					}
				}
				content = "";
				for (String field: fields) {
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
						for (Object value: fieldValues) {
							if (value != null && (fieldValue = StringUtils.trimToNull(value.toString())) != null) {
								content += "<field name=\"" + field + "\">";
								content += StaticMethods.xmlEscape(fieldValue);
								content += "</field>\n";
							}
						}
					}
				}
			}

		} catch (Exception e) {
			logger.error("Fel vid hämtande av xml-data (" + binDataField + ") för " + uri);
		}
		return content;
	}

	/**
	 * Skapar ett query
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
				// logga sökdata
				loggData(rootNode);
			}
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod. Var god försök igen", "Search.createQuery", e.getMessage(), true);
		} catch (CQLParseException e) {
			throw new DiagnosticException("Parserfel uppstod. Detta beror troligen på att query-strängen inte följer CQL syntax. Var god kontrollera söksträngen eller kontakta systemadministratör för söksystemet du använder", "Search.createQuery", e.getMessage(), false);
		}
		return query;
	}

	/**
	 * returnerar true om sortConfig är satt till "desc"
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

	/**
	 * Loggar data för sökningen för indexet "text".
	 * @param query cql
	 * @throws DiagnosticException
	 */
	private void loggData(CQLNode query) throws DiagnosticException {
		if (query == null) {
			return;
		}
		if (query instanceof CQLBooleanNode) {
			CQLBooleanNode bool = (CQLBooleanNode) query;
			loggData(bool.left);
			loggData(bool.right);
		} else if (query instanceof CQLTermNode) {
			CQLTermNode t = (CQLTermNode) query;
			// bara för "text"
			if (t.getIndex().equals(ContentHelper.IX_TEXT)) {
				StatisticLoggData data = new StatisticLoggData();
				data.setParam(t.getIndex());
				data.setAPIKey(apiKey);
				data.setQueryString(t.getTerm());
				serviceProvider.getStatisticsManager().addToQueue(data);
			}
		}
	}
}