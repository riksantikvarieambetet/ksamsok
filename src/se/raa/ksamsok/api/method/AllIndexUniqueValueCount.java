package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Utför metoden allIndexUniqueValue count som returnerar en lista över index
 * och hur många unika värden dessa index har som matchar givet query.
 * @author Henrik Hjalmarsson
 */
public class AllIndexUniqueValueCount implements APIMethod
{
	private String queryString;
	private PrintWriter writer;
	
	private static final Logger logger = Logger.getLogger(
			"se.raa.ksamsok.api.method.AllIndexUniqueValueCount");
	
	/** metodens namn */
	public static final String METHOD_NAME = "allIndexUniqueValueCount";
	/** query parameterns namn */
	public static final String QUERY_PARAMETER = "query";
	
	/**
	 * skapar ett objekt av AllIndexUniqueValueCount från given query sträng
	 * och writer som skall skriva resultatet.
	 * @param queryString
	 * @param writer
	 */
	public AllIndexUniqueValueCount(String queryString, PrintWriter writer)
	{
		this.queryString = queryString;
		this.writer = writer;
	}
	
	@Override
	public void performMethod()
		throws DiagnosticException, BadParameterException
	{
		IndexSearcher searcher = 
			LuceneServlet.getInstance().borrowIndexSearcher();
		try
		{
			BooleanQuery.setMaxClauseCount(10000);
			List<String> indexList =
				LuceneServlet.getInstance().getIndexList();
			
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			Query q1 = CQL2Lucene.makeQuery(node);
			doAllIndexUniqueValueCount(searcher, indexList, q1);
		} catch (IOException e)
		{
			throw new DiagnosticException("oväntat IO fel uppstod",
					"AllIndexUniqueValueCount.performMethod", e.getMessage(),
					true);
		} catch (CQLParseException e)
		{
			throw new DiagnosticException("Oväntat parser fel uppstod",
					"AllIndexUniqueValueCount.performMethod", e.getMessage(),
					true);
		}finally
		{
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}

	private void doAllIndexUniqueValueCount(IndexSearcher searcher,
			List<String> indexList, Query q1)
		throws DiagnosticException
	{
		int numq = 0;
		// använd frågan som ett filter och cacha upp filterresultatet
		Filter qwf = new CachingWrapperFilter(new QueryWrapperFilter(q1));
		try
		{
			for(int i = 0; i < indexList.size(); i++)
			{
				String index = indexList.get(i);
				Term term = new Term(index, "*");
				Query q = new WildcardQuery(term);
				Query rq = searcher.rewrite(q);
				Set<Term> terms = new HashSet<Term>();
				rq.extractTerms(terms);
				++numq; // inte en ren fråga, men borde räknas ändå
				int counter = 0;
				for(Term t : terms)
				{
					Query q2 = new TermQuery(t);
					TopDocs topDocs = searcher.search(q2, qwf, 1);
					++numq;
					if(topDocs.totalHits > 0)
					{
						counter++;
					}
				}
				if(counter > 0)
				{
					writeResult(index,counter);
				}
			}
		}catch(IOException e)
		{
			throw new DiagnosticException("Oväntat IO fel uppstod",
					"AllIndexUniqueValueCount.doAllIndexUniqueValueCount",
					e.getMessage(), true);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Ställde totalt " + numq + " frågor");
		}
	}

	private void writeResult(String index, int count)
	{
		writer.println("<index>");
		writer.println("<name>" + index + "</name>");
		writer.println("<uniqueValues>" + count + "</uniqueValues>");
		writer.println("</index>");
	}
}