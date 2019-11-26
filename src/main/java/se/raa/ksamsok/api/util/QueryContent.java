package se.raa.ksamsok.api.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Klass som innehåller ett info om ett query
 * @author Henrik Hjalmarsson
 */
public class QueryContent
{
	private HashMap<String,String> terms;
	private Long hits;
	
	/**
	 * skapar ett objekt av QueryContent
	 */
	public QueryContent()
	{
		terms = new HashMap<>();
		hits = 0L;
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
	public void setHits(Long hits)
	{
		this.hits = hits;
	}
	
	/**
	 * returnerar antalet träffar för query
	 * @return antal träffar
	 */
	public Long getHits()
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
		StringBuilder queryString = new StringBuilder();
		for(String index : indexSet)
		{
			String term = StaticMethods.escape(terms.get(index));
			queryString.append(index).append(":\"").append(term).append("\" AND ");
		}
		queryString = new StringBuilder(queryString.substring(0, queryString.length() - 5));
		return queryString.toString();
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