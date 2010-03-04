package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
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
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Hanterar sökningar efter objekt
 * @author Henrik Hjalmarsson
 */
public class Search implements APIMethod
{
	protected String queryString = null;
	protected PrintWriter writer = null;
	protected int hitsPerPage;
	protected int startRecord;
	protected String sort = null;
	protected boolean sortDesc = false;
	protected String recordSchema = null;

	/** standardvärdet för antalet träffar per sida */
	public static final int DEFAULT_HITS_PER_PAGE = 50;
	/** standardvärdet för startpositionen i sökningen */
	public static final int DEFAULT_START_RECORD = 1;
	/** metodnamn som anges för att använda denna klass */
	public static final String METHOD_NAME = "search";
	/** parameternamn där sökparametrarna skall ligga när en sökning görs */
	public static final String SEARCH_PARAMS = "query";
	/** parameternamnet som anges för att välja antalet träffar per sida */
	public static final String HITS_PER_PAGE = "hitsPerPage";
	/** parameternamnet som anges för att välja startRecord */
	public static final String START_RECORD = "startRecord";
	/** parameternamn för sort */
	public static final String SORT = "sort";
	/** parameternamn för sort configuration */
	public static final String SORT_CONFIG = "sortConfig";
	/** parametervärde för descending sort */
	public static final String SORT_DESC = "desc";
	/** parametervärde för ascending sort */
	public static final String SORT_ASC = "asc";
	/** record shema för presentations data */
	public static final String NS_SAMSOK_PRES =
		"http://kulturarvsdata.se/presentation#";
	/** parameternamn för record schema */
	public static final String RECORD_SCHEMA = "recordSchema";
	/** bas URL till record schema */
	public static final String RECORD_SCHEMA_BASE =
		"http://kulturarvsdata.se/";
	
	private static final Logger logger = Logger.getLogger(
			"se.raa.ksamsok.api.Search");
	
	/**
	 * skapar ett Search objekt
	 * @param params sökparametrar
	 * @param hitsPerPage träffar som skall visas per sida
	 * @param startRecord startposition i sökningen
	 * @param writer skrivaren som skall användas för att skriva svaret
	 */
	public Search(String queryString, int hitsPerPage, int startRecord,
				PrintWriter writer)
	{
		this.queryString = queryString;
		this.writer = writer;
		//kontrollerar att hitsPerPage och startRecord har tillåtna värden
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
	 * Anger vilket record schema som skall användas
	 * @param recordSchema
	 */
	public void setRecordSchema(String recordSchema)
	{
		this.recordSchema = recordSchema;
	}
	
	/**
	 * sätter recordSchema
	 */
	protected void setRecordSchema()
	{
		if(recordSchema != null) {
			recordSchema = RECORD_SCHEMA_BASE + recordSchema + "#";
		}
	}
	
	protected MapFieldSelector getFieldSelector()
	{
		final String[] fieldNames = 
		{
			ContentHelper.CONTEXT_SET_REC + "." +
			ContentHelper.IX_REC_IDENTIFIER,
			ContentHelper.I_IX_PRES, 
			ContentHelper.I_IX_LON, 
			ContentHelper.I_IX_LAT
		};
		return new MapFieldSelector(fieldNames);
	}

	@Override
	public void performMethod()
		throws BadParameterException, DiagnosticException
	{	
		Query query = createQuery();
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		final MapFieldSelector fieldSelector = getFieldSelector(); 
		TopDocs hits = null;
		int numberOfDocs = 0;
		try { 
			int nDocs = startRecord - 1 + hitsPerPage;
			//här görs sökningen
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
			writeRecords(searcher, fieldSelector, hits, numberOfDocs, nDocs);
			writeFot();
		}catch(BooleanQuery.TooManyClauses e) {
			throw new BadParameterException("query gav upphov till för många booleska operationer", "Search.performMethod", query.toString(), true);
		}catch(IOException e) {
			throw new DiagnosticException("oväntat IO fel uppstod. Var god försök igen", "Search.performMethod", e.getMessage(), true);
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
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
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) +
				"</query>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}

	/**
	 * skriver ut övre del av XML svar
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
		content = content.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
		writer.println("<record>");
		writer.println(content);
		writer.println("<rel:score xmlns:rel=\"info:srw/extension/2/relevancy-1.0\">" + score + "</rel:score>");
		writer.println("</record>");
	}
	
	/**
	 * hämtar content
	 * @param doc
	 * @param uri
	 * @param hrm
	 * @return
	 * @throws Exception
	 */
	protected String getContent(Document doc, String uri, HarvestRepositoryManager hrm) 
		throws Exception
	{
		String content = null;
		if (NS_SAMSOK_PRES.equals(recordSchema)) {
			byte[] pres = doc.getBinaryValue(ContentHelper.I_IX_PRES);
			if (pres != null) {
				content = new String(pres, "UTF-8");
			} else {
				content = null;
				logger.warn("Hittade inte presentationsdata för " + uri);
			}
		} else {
			content = hrm.getXMLData(uri);
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
	private void writeRecords(IndexSearcher searcher, 
			final MapFieldSelector fieldSelector, TopDocs hits, 
			int numberOfDocs, int nDocs)
		throws DiagnosticException
	{
		try {
			HarvestRepositoryManager hrm = HarvesterServlet.getInstance().getHarvestRepositoryManager();
			for(int i = startRecord - 1;i < numberOfDocs && i < nDocs; i++) {
				Document doc = searcher.doc(hits.scoreDocs[i].doc, fieldSelector);
				double score = hits.scoreDocs[i].score;
				String uri = doc.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				writeContent(getContent(doc, uri, hrm), score);
			}
		}catch(UnsupportedEncodingException e) 	{
			//kan ej uppstå
		}catch(CorruptIndexException e) {
			throw new DiagnosticException("Oväntat index fel uppstod. Var god försök igen", "Search.writeRecords", e.getMessage(), true);
		}catch(IOException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod. Var god försök igen", "Search.writeRecods", e.getMessage(), true);
		}catch(Exception e) {
			throw new DiagnosticException("Fel uppstod när data skulle hämtas från databasen. Var god försök senare", "Search.writeRecords", e.getMessage(), true);
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
			throw new DiagnosticException("Oväntat IO fel uppstod. Var god försök igen", "Search.createQuery", e.getMessage(), true);
		}catch(CQLParseException e) {
			throw new DiagnosticException("Parserfel uppstod. Detta beror troligen på att query strängen inte följer CQL syntax. Var god kontrollera söksträngen eller kontakta system administratör för söksystemet du använder", "Search.createQuery", e.getMessage(), false);
		}
		return query;
	}
}