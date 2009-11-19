package se.raa.ksamsok.api;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.AllIndexUniqueValueCount;
import se.raa.ksamsok.api.method.Search;
import se.raa.ksamsok.api.method.Statistic;
import se.raa.ksamsok.api.method.StatisticSearch;

/**
 * Factory klass som bygger APIMethod objekt
 * @author Henrik Hjalmarsson
 */
public class APIMethodFactory 
{
	/** delare för att dela query strängar */
	private static final String DELIMITER = "|";
	//logger som används
	
	/**
	 * returnerar en instans av APIMethod beroende på vilka parametrar som
	 * kommer in
	 * @param params mottagna parametrar
	 * @param writer för att skriva svaret
	 * @return APIMethod en istans av någon subklass till APIMethod
	 */
	public static APIMethod getAPIMethod(Map<String, String> params,
				PrintWriter writer)
			throws MissingParameterException, BadParameterException
	{
		//hämtar ut metod namnet från parameter mappen
		String method = params.get(APIMethod.METHOD);
		if(method == null)//måste alltid finnas en metod
		{
			throw new MissingParameterException("obligatorisk parameter " + 
					APIMethod.METHOD + " saknas",
					"APIMethodFactory.getAPIMethod", "metod saknas", false);
		}
		
		APIMethod m = null;
		
		//en ny if sats läggs till för varje ny metod
		if(method.equals(Search.METHOD_NAME))
		{
			m = getSearchObject(params, writer);
		}else if(method.equals(Statistic.METHOD_NAME))
		{
			m = getStatisticObject(params, writer);
		}else if(method.equals(StatisticSearch.METHOD_NAME))
		{
			m = getStatisticSearchObject(params, writer);
		}else if(method.equals(AllIndexUniqueValueCount.METHOD_NAME))
		{
			m = getAllIndexUniqueValueCount(params, writer);
		}
		else
		{
			throw new MissingParameterException("metoden " + method + 
					" finns inte", "APIMethodFactory.getAPIMethod",
					"felaktig metod", false);
		}
		
		return m;
	}

	private static APIMethod getAllIndexUniqueValueCount(Map<String,
			String> params, PrintWriter writer)
		throws MissingParameterException
	{
		AllIndexUniqueValueCount m = null;
		String queryString = params.get(
				AllIndexUniqueValueCount.QUERY_PARAMETER);
		if(queryString != null && !queryString.equals(""))
		{
			m = new AllIndexUniqueValueCount(queryString, writer);
		}else
		{
			throw new MissingParameterException("Parametern " +
					AllIndexUniqueValueCount.QUERY_PARAMETER + " saknas eller " +
							"innehåller inget query",
							"APIMethodFactory.getAllIndexUniqueValueCount",
							null, false);
		}
		return m;
	}

	/*
	 * skapar ett objekt av StatisticSearch
	 */
	private static APIMethod getStatisticSearchObject(
			Map<String, String> params, PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		StatisticSearch m = null;
		String indexString = params.get(StatisticSearch.INDEX_PARAMETER);
		if(indexString == null)
		{
			throw new MissingParameterException("parametern " + 
					StatisticSearch.INDEX_PARAMETER + " saknas",
					"APIMethodFactory.getStatisticSearchObject", "index " +
							"parametern saknas", false);
		}else if(indexString.equals(""))
		{
			throw new MissingParameterException("parametern " + 
					StatisticSearch.INDEX_PARAMETER + " innehåller inga " +
							"värden",
							"APIMethodFactory.getStatisticSearchObject",
							"inga index finns", false); 
		}
		StringTokenizer indexTokenizer = 
			new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<String,String>();
		while(indexTokenizer.hasMoreTokens())
		{
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if(tokens.length < 2)
			{
				throw new BadParameterException("parametern " + 
						StatisticSearch.INDEX_PARAMETER + " är felskriven",
						"APIMethodFactory.getStatisticSearchObject",
						"syntax error i index parametern", false);
			}
			for(int i = 0; i < 2; i++)
			{
				if(i == 0)
				{
					index = tokens[i];
				}
				if(i == 1)
				{
					value = tokens[i];
				}
			}
			indexMap.put(index, value);
		}
		String queryString = params.get(StatisticSearch.QUERY_PARAMS);
		if(queryString == null)
		{
			throw new MissingParameterException("parametern " + 
					StatisticSearch.QUERY_PARAMS + " saknas",
					"APIMethodFactory.getStatisticSearchObject", "query " +
							"saknas", false);
		}else if(queryString.equals(""))
		{
			throw new MissingParameterException("parametern " + 
					StatisticSearch.QUERY_PARAMS + " innehåller inget query",
					"APIMethodFactory.getStatisticSearchObject", "query " +
							"saknas", false);
		}
		m = new StatisticSearch(writer, queryString, indexMap);
		String s = params.get(StatisticSearch.REMOVE_BELOW);
		if(s != null && !s.equals(""))
		{
			try
			{
				int removeBelow = Integer.parseInt(s);
				m.setRemoveBelow(removeBelow);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parameter " +
						StatisticSearch.REMOVE_BELOW + " måste innehålla ett " +
								"numeriskt värde", 
								"APIMethodFactory.getStatisticSearchObject", null,
								false);
			}
		}
		return m;
	}

	/*
	 * skapar ett Statistic objekt
	 */
	private static APIMethod getStatisticObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Statistic m;
		String indexString = params.get(Statistic.INDEX_PARAMETER);
		if(indexString == null)
		{
			throw new MissingParameterException("parametern " + 
					Statistic.INDEX_PARAMETER + " saknas",
					"APIMethodFactory.getStatisticObject", "index parameter" +
							" saknas", false);
		}
		StringTokenizer indexTokenizer = 
			new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<String,String>();
		while(indexTokenizer.hasMoreTokens())
		{
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if(tokens.length < 2)
			{
				throw new BadParameterException("parametern " + 
						Statistic.INDEX_PARAMETER + " är felskriven",
						"APIMethodFactory.getStatisticObject", "syntax fel" +
								" i index parametern", false);
			}
			for(int i = 0; i < 2; i++)
			{
				if(i == 0)
				{
					index = tokens[i];
				}
				if(i == 1)
				{
					value = tokens[i];
				}
			}
			indexMap.put(index, value);
		}
		m = new Statistic(indexMap, writer);
		String s = params.get(Statistic.REMOVE_BELOW);
		if(s != null && !s.equals(""))
		{
			try
			{
				int removeBelow = Integer.parseInt(s);
				m.setRemoveBelow(removeBelow);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parameter " +
						Statistic.REMOVE_BELOW + " måste innehålla ett " +
								"numeriskt värde", 
								"APIMethodFactory.getStatisticObject", null,
								false);
			}
		}
		return m;
	}

	/*
	 * skapar Search objektet
	 */
	private static APIMethod getSearchObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Search m;
		String query = params.get(Search.SEARCH_PARAMS);
		if(query == null)
		{
			throw new MissingParameterException("parametern " + 
					Search.SEARCH_PARAMS + " saknas",
					"APIMethodFactory.getSearchObject", "query saknas",
					false);
		}else if(query.equals(""))
		{
			throw new MissingParameterException("parametern " + 
					Search.SEARCH_PARAMS + " är tom",
					"APIMethodFactory.getSearchObject", "query saknas",
					false);
		}
		
		//sätter valfria parametrar
		int hitsPerPage = Search.DEFAULT_HITS_PER_PAGE;
		String temp = params.get(Search.HITS_PER_PAGE);
		if(temp != null)
		{
			try
			{
				hitsPerPage = Integer.parseInt(temp);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parametern " + 
						Search.HITS_PER_PAGE + " måste innehålla ett " +
								"numeriskt värde",
								"APIMethodFactory.getSearchObject", "icke " +
										"numeriskt värde", false);
			}
		}
		int startRecord = Search.DEFAULT_START_RECORD;
		temp = params.get(Search.START_RECORD);
		if(temp != null)
		{
			try
			{
				startRecord = Integer.parseInt(temp);
			}catch(NumberFormatException e)
			{
				throw new BadParameterException("parametern " + 
						Search.START_RECORD + " måste innehålla ett " +
								"numeriskt värde",
								"APIMethodFactory.getSearchObject", "icke " +
										"numeriskt värde", false);
			}
		}
		m = new Search(query, hitsPerPage, startRecord, writer);
		String sort = params.get(Search.SORT);
		if(sort != null)
		{	
			m.sortBy(sort);
			String sortConfig = params.get(Search.SORT_CONFIG);
			if(sortConfig != null)
			{
				if(sortConfig.equalsIgnoreCase(Search.SORT_DESC))
				{
					m.sortDesc(true);
				}
			}
		}
		
		String recordSchema = params.get(Search.RECORD_SCHEMA);
		if(recordSchema != null)
		{
			m.setRecordSchema(recordSchema);
		}
		return m;
	}
}