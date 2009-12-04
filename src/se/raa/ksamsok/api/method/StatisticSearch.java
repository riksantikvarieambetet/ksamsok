package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopDocs;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * utför en statisticSearch operation
 * @author Henrik Hjalmarsson
 */
public class StatisticSearch extends Statistic 
{
	protected String queryString = null;

	/** metodens namn */
	public static final String METHOD_NAME = "statisticSearch";
	/** parameternamn där query skickas in */
	public static final String QUERY_PARAMS = "query";
	
	/**
	 * skapar ett objekt av StatisticSearch
	 * @param writer används för att skriva svar
	 * @param queryString sträng med query
	 * @param indexMap set med index namn
	 */
	public StatisticSearch(PrintWriter writer, String queryString, 
							Map<String,String> indexMap)
	{
		super(indexMap, writer);
		this.queryString = queryString;
	}
	
	@Override
	public void performMethod() 
		throws DiagnosticException, MissingParameterException,
		BadParameterException
	{
		IndexSearcher searcher =
			LuceneServlet.getInstance().borrowIndexSearcher();
		Map<String,Set<Term>> termMap = null;
		List<QueryContent> queryList = null;
		try
		{
			//bygger term map
			termMap = buildTermMap(searcher);
			//gör kartesisk produkt av alla värden i term mappen
			queryList = cartesian(termMap);
			//utför sökningen
			doStatisticSearch(searcher, queryList);
			//skriver resultatet
			writeHead(queryList);
			writeResult(queryList);
			writeFot();
			
		}catch(OutOfMemoryError e)
		{
			throw new BadParameterException("de inskickade index värdena " +
					"gav upphov till att för många värden hittades och " +
					"denna sökning gick ej att utföra",
					"StatisticSearch.performMethod", null, false);
		}finally
		{
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	@Override
	protected void writeFot()
	{
		writer.println("<echo>");
		writer.println("<method>" + Statistic.METHOD_NAME + "</method>");
		for(String index : indexMap.keySet())
		{
			writer.println("<index>" + index + "=" + indexMap.get(index) +
					"</index>");
		}
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) +
				"</query>");
		writer.println("</echo>");
	}

	/**
	 * utför sökning
	 * @param searcher som skall användas
	 * @param queryList med queryn som skall utföras
	 */
	protected void doStatisticSearch(IndexSearcher searcher,
			List<QueryContent> queryList)
		throws BadParameterException, DiagnosticException
	{
		CQLParser parser = new CQLParser();
		try
		{
			CQLNode node = parser.parse(queryString);
			Query q2 = CQL2Lucene.makeQuery(node);
			CachingWrapperFilter qwf = 
				new CachingWrapperFilter(new QueryWrapperFilter(q2));
			for(int i = 0; i < queryList.size(); i++)
			{
				QueryContent content = queryList.get(i);
				/*CQLNode node = parser.parse(content.getQueryString(
						queryString));
				Query q = CQL2Lucene.makeQuery(node);*/
				Query q1 = content.getQuery();
				
				TopDocs topDocs = searcher.search(q1, qwf, 1);
				if(topDocs.totalHits >= removeBelow)
				{
					content.setHits(topDocs.totalHits);
					queryList.set(i, content);
				}else
				{
					queryList.remove(i);
					i--;
				}
			}
		}catch(CQLParseException e)
		{
			throw new DiagnosticException("Oväntat perser fel uppstod." +
					" Var god försök igen",
					"StatisticSearch.doStatisticSearch", e.getMessage() +
					"\n" + e.getStackTrace().toString(), true);
		}catch(IOException e)
		{
			throw new DiagnosticException("Oväntat IO fel uppstod. Var" +
					" god försök igen",
					"StatisticSearch.doStatisticSearch", e.getMessage() +
					"\n" + e.getStackTrace().toString(), true);
		}
	}
}