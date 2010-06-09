package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.api.util.statisticLogg.StatisticLoggData;
import se.raa.ksamsok.api.util.statisticLogg.StatisticLogger;
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Hanterar s�kningar efter objekt
 * @author Henrik Hjalmarsson
 */
@SuppressWarnings("unused")
public class Search implements APIMethod
{
	protected String queryString = null;
	protected PrintWriter writer = null;
	protected int hitsPerPage;
	protected int startRecord;
	protected String sort = null;
	protected boolean sortDesc = false;
	protected String recordSchema = null;
	private String APIKey;

	/** standardv�rdet för antalet tr�ffar per sida */
	public static final int DEFAULT_HITS_PER_PAGE = 50;
	/** standardv�rdet f�r startpositionen i s�kningen */
	public static final int DEFAULT_START_RECORD = 1;
	/** metodnamn som anges f�r att anv�nda denna klass */
	public static final String METHOD_NAME = "search";
	/** parameternamn d�r s�kparametrarna skall ligga n�r en s�kning g�rs */
	public static final String SEARCH_PARAMS = "query";
	/** parameternamnet som anges f�r att v�lja antalet tr�ffar per sida */
	public static final String HITS_PER_PAGE = "hitsPerPage";
	/** parameternamnet som anges f�r att v�lja startRecord */
	public static final String START_RECORD = "startRecord";
	/** parameternamn f�r sort */
	public static final String SORT = "sort";
	/** parameternamn f�r sort configuration */
	public static final String SORT_CONFIG = "sortConfig";
	/** parameterv�rde f�r descending sort */
	public static final String SORT_DESC = "desc";
	/** parameterv�rde f�r ascending sort */
	public static final String SORT_ASC = "asc";
	/** record shema f�r presentations data */
	public static final String NS_SAMSOK_PRES =	"http://kulturarvsdata.se/presentation#";
	/** parameternamn f�r record schema */
	public static final String RECORD_SCHEMA = "recordSchema";
	/** bas URL till record schema */
	public static final String RECORD_SCHEMA_BASE =
		"http://kulturarvsdata.se/";
	
	private static final Logger logger = Logger.getLogger(
			"se.raa.ksamsok.api.Search");
	// f�lt att h�mta fr�n lucene
	private static final MapFieldSelector RDF_FIELDS = new MapFieldSelector(
			new String[] {
					ContentHelper.I_IX_RDF,
					ContentHelper.CONTEXT_SET_REC + "."
							+ ContentHelper.IX_REC_IDENTIFIER });
	private static final MapFieldSelector PRES_FIELDS = new MapFieldSelector(
			new String[] {
					ContentHelper.I_IX_PRES,
					ContentHelper.CONTEXT_SET_REC + "."
							+ ContentHelper.IX_REC_IDENTIFIER });
	
	/**
	 * skapar ett Search objekt
	 * @param params s�kparametrar
	 * @param hitsPerPage tr�ffar som skall visas per sida
	 * @param startRecord startposition i s�kningen
	 * @param writer skrivaren som skall anv�ndas f�r att skriva svaret
	 */
	public Search(String queryString, int hitsPerPage, int startRecord,
				PrintWriter writer, String APIKey)
	{
		this.APIKey = APIKey;
		this.queryString = queryString;
		this.writer = writer;
		//kontrollerar att hitsPerPage och startRecord har till�tna v�rden
		if(hitsPerPage < 1 || hitsPerPage > 500) {
			this.hitsPerPage = DEFAULT_HITS_PER_PAGE;
		}else {
			this.hitsPerPage = hitsPerPage;
		}
		if(startRecord < 1) {
			this.startRecord = DEFAULT_START_RECORD;
		}else {
			this.startRecord = startRecord;
		}
	}
	
	/**
	 * Anger vilket index resultatet skall sorteras efter
	 * @param field
	 */
	public void sortBy(String field)
	{
		sort = field;
	}
	
	/**
	 * Anger om resultatet skall sorteras descending eller inte
	 * @param b
	 */
	public void sortDesc(boolean b)
	{
		sortDesc = b;
	}
	
	/**
	 * Anger vilket record schema som skall anv�ndas
	 * @param recordSchema
	 */
	public void setRecordSchema(String recordSchema)
	{
		this.recordSchema = recordSchema;
	}
	
	/**
	 * s�tter recordSchema
	 */
	protected void setRecordSchema()
	{
		if(recordSchema != null) {
			recordSchema = RECORD_SCHEMA_BASE + recordSchema + "#";
		}
	}

	@Override
	public void performMethod()
		throws BadParameterException, DiagnosticException
	{	
		Query query = createQuery();
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher(); 
		TopDocs hits = null;
		int numberOfDocs = 0;
		try {
			loggData(query, searcher);
			int nDocs = startRecord - 1 + hitsPerPage;
			//h�r g�rs s�kningen
			if(sort == null) {
				hits = searcher.search(query, nDocs == 0 ? 1 : nDocs);
			}else {
				if(!ContentHelper.indexExists(sort)) {
					throw new BadParameterException("sorterings indexet " + sort + " finns inte.", "Search.performMethod", null, false);
				}
				Sort s = new Sort(new SortField(sort, sortDesc));
				hits = searcher.search(query, null, nDocs == 0 ? 1 : nDocs, s);
			}
			numberOfDocs = hits.totalHits;
			writeHead(numberOfDocs);
			writeRecords(searcher, hits, numberOfDocs, nDocs);
			writeFot();
		}catch(BooleanQuery.TooManyClauses e) {
			throw new BadParameterException("query gav upphov till f�r m�nga booleska operationer", "Search.performMethod", query.toString(), true);
		}catch(IOException e) {
			throw new DiagnosticException("ov�ntat IO fel uppstod. Var god f�rs�k igen", "Search.performMethod", e.getMessage(), true);
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	/**
	 * Loggar data f�r s�kningen
	 * @param query
	 * @param searcher
	 * @throws DiagnosticException
	 */
	private void loggData(Query query, IndexSearcher searcher)
		throws DiagnosticException
	{
		try {
			Query tempQuery = query.rewrite(searcher.getIndexReader());
			Set<Term> termSet = new HashSet<Term>();
			tempQuery.extractTerms(termSet);
			for(Term t : termSet) {
				if(t.field().equals("text")) {
					StatisticLoggData data = new StatisticLoggData();
					data.setParam(t.field());
					data.setAPIKey(APIKey);
					data.setQueryString(t.text());
					StatisticLogger.addToQueue(data);
					break;
				}
			}
		}catch(IOException e) {
			throw new DiagnosticException("Ov�ntat IO fel", "se.raa.ksamsok.api.method.Search", null, true);
		}
	}

	/**
	 * skriver ut nedre del av XML svar
	 */
	private void writeFot()
	{	
		writer.println("<echo>");
		writer.println("<startRecord>" + startRecord + "</startRecord>");
		writer.println("<hitsPerPage>" + hitsPerPage + "</hitsPerPage>");
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) + "</query>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}

	/**
	 * skriver ut �vre del av XML svar
	 * @param numberOfDocs
	 */
	private void writeHead(int numberOfDocs)
	{
		StartEndWriter.writeStart(writer);
		StartEndWriter.hasHead(true);
		writer.println("<totalHits>" + numberOfDocs + "</totalHits>");
		writer.println("<records>");
	}
	
	/**
	 * skriver ut content
	 * @param content
	 * @param score
	 */
	protected void writeContent(String content, double score)
	{
		writer.println("<record>");
		writer.println(content);
		writer.println("<rel:score xmlns:rel=\"info:srw/extension/2/relevancy-1.0\">" + score + "</rel:score>");
		writer.println("</record>");
	}
	
	/**
	 * H�mtar xml-inneh�ll (fragment) fr�n ett lucene-dokument som en str�ng.
	 * @param doc lucenedokument
	 * @param uri postens uri (anv�nds bara f�r log)
	 * @param xmlIndex index att h�mta inneh�ll fr�n
	 * @return xml-fragment med antingen presentations-xml eller rdf; null om data saknas
	 * @throws Exception vid teckenkodningsfel (b�r ej intr�ffa) 
	 */
	protected String getContent(Document doc, String uri, String xmlIndex) 
		throws Exception {
		String content = null;
		byte[] xmlData = doc.getBinaryValue(xmlIndex);
		if (xmlData != null) {
			content = new String(xmlData, "UTF-8");
		}
		// TODO: NEK: ta bort n�r allt �r omindexerat
		if (content == null) {
			content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(uri);
		}
		if (content == null) {
			logger.warn("Hittade inte xml-data (" + xmlIndex + ") f�r " + uri);
		}
		return content;
	}

	/**
	 * skriver ut resultat
	 * @param searcher
	 * @param fieldSelector
	 * @param hits
	 * @param numberOfDocs
	 * @param nDocs
	 */
	private void writeRecords(IndexSearcher searcher, TopDocs hits, 
			int numberOfDocs, int nDocs)
		throws DiagnosticException
	{
		try {
			final String xmlIndex;
			final MapFieldSelector fieldSelector;
			// ta fram rätt data
			setRecordSchema();
			if (NS_SAMSOK_PRES.equals(recordSchema)) {
				fieldSelector = PRES_FIELDS;
				xmlIndex = ContentHelper.I_IX_PRES;
			} else {
				fieldSelector = RDF_FIELDS;
				xmlIndex = ContentHelper.I_IX_RDF;
			}
			for(int i = startRecord - 1;i < numberOfDocs && i < nDocs; i++) {
				Document doc = searcher.doc(hits.scoreDocs[i].doc, fieldSelector);
				double score = hits.scoreDocs[i].score;
				String uri = doc.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				writeContent(getContent(doc, uri, xmlIndex), score);
			}
		}catch(UnsupportedEncodingException e) 	{
			//kan ej uppst�
		}catch(CorruptIndexException e) {
			throw new DiagnosticException("Ov�ntat index fel uppstod. Var god f�rs�k igen", "Search.writeRecords", e.getMessage(), true);
		}catch(IOException e) {
			throw new DiagnosticException("Ov�ntat IO fel uppstod. Var god f�rs�k igen", "Search.writeRecods", e.getMessage(), true);
		}catch(Exception e) {
			throw new DiagnosticException("Fel uppstod n�r data skulle h�mtas. Var god f�rs�k senare", "Search.writeRecords", e.getMessage(), true);
		}finally {
			writer.println("</records>");
		}
	}

	/**
	 * Skapar ett query
	 * @return query
	 */
	protected Query createQuery()
		throws DiagnosticException, BadParameterException
	{
		Query query = null;
		try {
			CQLParser parser = new CQLParser();
			CQLNode rootNode = parser.parse(queryString);
			query = CQL2Lucene.makeQuery(rootNode);
		}catch(IOException e) {
			throw new DiagnosticException("Ov�ntat IO fel uppstod. Var god f�rs�k igen", "Search.createQuery", e.getMessage(), true);
		}catch(CQLParseException e) {
			throw new DiagnosticException("Parserfel uppstod. Detta beror troligen p� att query str�ngen inte f�ljer CQL syntax. Var god kontrollera s�kstr�ngen eller kontakta system administrat�r f�r s�ksystemet du anv�nder", "Search.createQuery", e.getMessage(), false);
		}
		return query;
	}
}