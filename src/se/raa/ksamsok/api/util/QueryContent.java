package se.raa.ksamsok.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.raa.ksamsok.api.exception.DiagnosticException;

/**
 * Klass som innehåller ett info om ett query
 * @author Henrik Hjalmarsson
 */
public class QueryContent
{
	private HashMap<String,String> terms;
	private int hits;
	
	/**
	 * skapar ett objekt av QueryContent
	 */
	public QueryContent()
	{
		terms = new HashMap<String,String>();
		hits = 0;
	}
	
	/**
	 * lägger till en term
	 * @param index för term
	 * @param term värde för term
	 */
	public void addTerm(String index, String term)
	{
		terms.put(index, term);
	}
	
	/**
	 * sätter antalet träffar för query
	 * @param hits träffar
	 */
	public void setHits(int hits)
	{
		this.hits = hits;
	}
	
	/**
	 * returnerar antalet träffar för query
	 * @return antal träffar
	 */
	public int getHits()
	{
		return hits;
	}
	
	/**
	 * returnerar mappen med termer
	 * @return Map
	 */
	public Map<String,String> getTermMap()
	{
		return terms;
	}
	
	/**
	 * skapar en query sträng av termer
	 * @return query sträng
	 */
	public String getQueryString()
	{
		Set<String> indexSet = terms.keySet();
		String queryString = "";
		for(String index : indexSet)
		{//TODO tror denna skall fungera för alla värden
			String term = StaticMethods.escape(terms.get(index));
			queryString += index + "=\"" + term + "\" AND ";
		}
		queryString = queryString.substring(0, queryString.length() - 5);
		return queryString;
	}
	
	/**
	 * returnerar ett query byggt av innehållet av detta
	 * QueryContent
	 * @return
	 * @throws DiagnosticException
	 */
	public Query getQuery()
		throws DiagnosticException
	{
		// om bara en term, gör ingen boolean query
		if (terms.size() == 1) {
			String index = terms.keySet().iterator().next();
			return new TermQuery(new Term(index, terms.get(index)));
		}
		BooleanQuery query = new BooleanQuery();
		for(String index : terms.keySet())
		{
			String value = terms.get(index);
			Query q = new TermQuery(new Term(index, value));
			query.add(q, BooleanClause.Occur.MUST);
		}
		return query;
	}
	
	/**
	 * skapar en query sträng av lagrade termer och given query sträng
	 * @param query
	 * @return query sträng
	 */
	public String getQueryString(String query)
	{
		String queryString = getQueryString();
		queryString += " AND " + query;
		return queryString;
	}
}