package se.raa.ksamsok.api;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.AllIndexUniqueValueCount;
import se.raa.ksamsok.api.method.Facet;
import se.raa.ksamsok.api.method.GetServiceOrganization;
import se.raa.ksamsok.api.method.RSS;
import se.raa.ksamsok.api.method.Search;
import se.raa.ksamsok.api.method.SearchHelp;
import se.raa.ksamsok.api.method.Statistic;
import se.raa.ksamsok.api.method.StatisticSearch;
import se.raa.ksamsok.api.method.Stem;

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
		return getMethod(method, params, writer);
	}
	
	/**
	 * returnerar en APIMethod
	 * @param method metodens namn
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getMethod(String method, Map<String,String> params, PrintWriter writer) throws MissingParameterException, BadParameterException
	{
		APIMethod m = null;
		//en ny if sats läggs till för varje ny metod
		if(method.equals(Search.METHOD_NAME)) {
			m = getSearchObject(params, writer);
		}else if(method.equals(Statistic.METHOD_NAME)) {
			m = getStatisticObject(params, writer);
		}else if(method.equals(StatisticSearch.METHOD_NAME)) {
			m = getStatisticSearchObject(params, writer);
		}else if(method.equals(AllIndexUniqueValueCount.METHOD_NAME)) {
			m = getAllIndexUniqueValueCountObject(params, writer);
		}else if(method.equals(Facet.METHOD_NAME)) {
			m = getFacetObject(params, writer);
		}else if(method.equals(SearchHelp.METHOD_NAME)) {
			m = getSearchHelpObject(params, writer);
		}else if(method.equals(RSS.METHOD_NAME)) {
			m = getRSSObject(params, writer);
		}else if(method.equals(GetServiceOrganization.METHOD_NAME)) {
			m = getGetServiceOrganizationsObject(writer, params);
		}else if(method.equals(Stem.METHOD_NAME)) {
			m = getStemObject(writer, params);
		}else {
			throw new MissingParameterException("metoden " + method + " finns inte", "APIMethodFactory.getAPIMethod", "felaktig metod", false);
		}
		return m;
	}

	/**
	 * Returnerar ett objekt för att hantera api-metoden stem
	 * @param writer skrivare som skall användas för att skriva resultatet
	 * @param params inparametrar
	 * @return api-objekt för ordstamning 
	 */
	private static APIMethod getStemObject(PrintWriter writer, Map<String,String> params)
	{
		return new Stem(writer, params);
	}

	/**
	 * Returnerar ett objekt av getServiceOrganization
	 * @param writer skrivare som skall användas för att skriva resultatet
	 * @param params in parametrar
	 * @return getServiceOrganization Objekt
	 */
	private static APIMethod getGetServiceOrganizationsObject(PrintWriter writer, Map<String,String> params)
	{
		GetServiceOrganization m = null;
		String value = params.get(GetServiceOrganization.VALUE);
		m = new GetServiceOrganization(writer, value);
		return m;
	}

	/**
	 * skapar ett objekt av RSS
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getRSSObject(Map<String, String> params,
			PrintWriter writer) 
		throws MissingParameterException, BadParameterException
	{
		RSS m = null;
		String queryString = getQueryString(params.get(RSS.QUERY));
		int hitsPerPage = getHitsPerPage(params.get(RSS.HITS_PER_PAGE));
		int startRecord = getStartRecord(params.get(RSS.START_RECORD));
		m = new RSS(queryString, hitsPerPage, startRecord, writer);
		return m;
	}

	/**
	 * skapar ett objekt av SearchHelp
	 * @param params
	 * @param writer
	 * @return SearchHelp objekt
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getSearchHelpObject(Map<String, String> params,
			PrintWriter writer) throws MissingParameterException, BadParameterException
	{
		SearchHelp m = null;
		List<String> indexList = getIndexList(params.get(SearchHelp.INDEX_PARAMETER));
		String prefix = getPrefix(params.get(SearchHelp.PREFIX_PARAMETER));
		int maxValueCount = getMaxValueCount(params.get(SearchHelp.MAX_VALUE_COUNT_PARAMETER));
		m = new SearchHelp(writer, indexList, prefix, maxValueCount);
		return m;
	}

	

	/**
	 * skapar ett objekt av typen Facet
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getFacetObject(Map<String, String> params,
			PrintWriter writer) 
		throws MissingParameterException, BadParameterException
	{
		Facet m = null;
		String queryString = getQueryString(params.get(Facet.QUERY_PARAMS));
		Map<String,String> indexMap = getIndexMapSingleValue(params.get(Facet.INDEX_PARAMETER), "*");
		m = new Facet(indexMap, writer, queryString);
		int removeBelow = getRemoveBelow(params.get(Facet.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/**
	 * skapar ett ojekt av typen AllIndexUniqueValueCount
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 */
	private static APIMethod getAllIndexUniqueValueCountObject(Map<String,
			String> params, PrintWriter writer)
		throws MissingParameterException
	{
		AllIndexUniqueValueCount m = null;
		String queryString = getQueryString(params.get(AllIndexUniqueValueCount.QUERY_PARAMS));
		String indexString = params.get(AllIndexUniqueValueCount.INDEX_PARAMETER);
		Map<String,String> indexMap = null;
		if(indexString != null)
		{
			indexMap = getIndexMapSingleValue(indexString, "*");
		}
		m = new AllIndexUniqueValueCount(queryString, writer, indexMap);
		return m;
	}

	/**
	 * Skapar ett objekt av StatisticSearch
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getStatisticSearchObject(
			Map<String, String> params, PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		StatisticSearch m = null;
		Map<String,String> indexMap = getIndexMapDoubleValue(params.get(StatisticSearch.INDEX_PARAMETER));
		String queryString = getQueryString(params.get(StatisticSearch.QUERY_PARAMS));
		m = new StatisticSearch(writer, queryString, indexMap);
		int removeBelow = getRemoveBelow(params.get(StatisticSearch.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/**
	 * Skapar ett Statistic objekt
	 * @param params inparametrar
	 * @param writer skriver resultatet
	 * @return Statistik objekt
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getStatisticObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Statistic m;
		Map<String,String> indexMap = getIndexMapDoubleValue(params.get(
				Statistic.INDEX_PARAMETER));
		
		m = new Statistic(indexMap, writer);
		int removeBelow = getRemoveBelow(params.get(Statistic.REMOVE_BELOW));
		m.setRemoveBelow(removeBelow);
		return m;
	}

	/**
	 * Skapar ett objekt av Search
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static APIMethod getSearchObject(Map<String, String> params,
			PrintWriter writer)
		throws MissingParameterException, BadParameterException
	{
		Search m = null;
		String query = getQueryString(params.get(Search.SEARCH_PARAMS));
		//sätter valfria parametrar
		int hitsPerPage = getHitsPerPage(params.get(Search.HITS_PER_PAGE));
		int startRecord = getStartRecord(params.get(Search.START_RECORD));
		m = new Search(query, hitsPerPage, startRecord, writer, params.get(APIMethod.API_KEY_PARAM_NAME));
		m.sortBy(params.get(Search.SORT));
		m.sortDesc(getSortConfig(params.get(Search.SORT), params.get(Search.SORT_CONFIG)));
		m.setRecordSchema(params.get(Search.RECORD_SCHEMA));
		return m;
	}
	
	/**
	 * returnerar true om sortConfig är satt till "desc"
	 * @param sort
	 * @param sortConfig
	 * @return
	 */
	private static boolean getSortConfig(String sort, String sortConfig)
	{
		boolean sortDesc = false;
		if(sort != null) {
			if(sortConfig != null && sortConfig.equals(Search.SORT_DESC)) {
				sortDesc = true;
			}
		}
		return sortDesc;
	}

	/**
	 * returnerar en integer för värdet startRecord
	 * @param param
	 * @return
	 * @throws BadParameterException
	 */
	private static int getStartRecord(String param) 
		throws BadParameterException
	{
		int startRecord = 0;
		if(param != null) {
			try {
				startRecord = Integer.parseInt(param);
			}catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + Search.START_RECORD + " måste innehålla ett numeriskt värde", "APIMethodFactory.getSearchObject", "icke numeriskt värde", false);
			}
		}
		return startRecord;
	}

	/**
	 * returnerar hitsPerPage
	 * @param param
	 * @return
	 * @throws BadParameterException
	 */
	private static int getHitsPerPage(String param) 
		throws BadParameterException
	{
		int hitsPerPage = 0;
		if(param != null) {
			try {
				hitsPerPage = Integer.parseInt(param);
			}catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + Search.HITS_PER_PAGE + " måste innehålla ett numeriskt värde", "APIMethodFactory.getSearchObject", "icke numeriskt värde", false);
			}
		}
		return hitsPerPage;
	}

	/**
	 * returnerar en query sträng
	 * @param queryString
	 * @return
	 * @throws MissingParameterException
	 */
	private static String getQueryString(String queryString) 
		throws MissingParameterException
	{
		if(queryString == null || queryString.trim().length() < 1) {
			throw new MissingParameterException("parametern query saknas eller är tom", "APIMethodFactory.getQueryString", null, false);
		}
		return queryString;
	}
	
	/**
	 * returnerar en lista med index
	 * @param indexString
	 * @return
	 * @throws MissingParameterException
	 */
	private static List<String> getIndexList(String indexString) 
		throws MissingParameterException
	{
		List<String> indexList = new ArrayList<String>();
		if(indexString == null || indexString.trim().length() < 1) {
			throw new MissingParameterException("parametern index saknas eller är tom", "APIMethodFactory.getIndexList", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while(indexTokenizer.hasMoreTokens()) {
			indexList.add(indexTokenizer.nextToken());
		}
		return indexList;
	}
	
	/**
	 * returnerar ett prefix
	 * @param prefix
	 * @return
	 */
	private static String getPrefix(String prefix)
	{
		if(prefix == null) {
			prefix = "*";
		}else if(!prefix.endsWith("*")) {
			prefix += "*";
		}
		return prefix;
	}
	
	/**
	 * returnerar max value count
	 * @param maxValueCountString
	 * @return
	 * @throws BadParameterException
	 */
	private static int getMaxValueCount(String maxValueCountString) 
		throws BadParameterException
	{
		int maxValueCount = 0;
		if(maxValueCountString == null) {
			maxValueCount = SearchHelp.DEFAULT_MAX_VALUE_COUNT;
		}else {
			try {
				maxValueCount = Integer.parseInt(maxValueCountString);
			}catch(NumberFormatException e) {
				throw new BadParameterException("parametern maxValueCount måste vara ett numeriskt värde", "APIMethodFactory.getMaxValueCount", null, false);
			}
		}
		return maxValueCount;
	}

	/**
	 * Returnerar en index-map där indexen får samma värde, det som är inskickat i value.
	 *
	 * @param indexString sträng med indexnamn separerade av {@linkplain #DELIMITER}
	 * @param value värde för index
	 * @return index-map med indexnamn som nyckel och inskickat värde som värde, aldrig null men kan vara tom
	 * @throws MissingParameterException om index-strängen är null eller "tom".
	 */
	private static Map<String,String> getIndexMapSingleValue(String indexString,
			String value) 
		throws MissingParameterException
	{
		Map<String,String> indexMap = new HashMap<String,String>();
		if(indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern index saknas eller är tom", "APIMethodFactory.getIndexMapSingleValue", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while(indexTokenizer.hasMoreTokens()) {
			indexMap.put(indexTokenizer.nextToken(), value);
		}
		return indexMap;
	}

	/**
	 * returnerar remove Below
	 * @param removeBelowString
	 * @return
	 * @throws BadParameterException
	 */
	private static int getRemoveBelow(String removeBelowString) 
		throws BadParameterException
	{
		int removeBelow = 0;
		if(removeBelowString != null) {
			try {
				removeBelow = Integer.parseInt(removeBelowString);
			}catch(NumberFormatException e) {
				throw new BadParameterException("Parametern removeBelow måste innehålla ett numeriskt värde", "APIMethodFactory.getRemoveBelow", null, false);
			}
		}
		return removeBelow;
	}
	
	/**
	 * returnerar en index lista där alla har samma värde
	 * @param indexString
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private static Map<String,String> getIndexMapDoubleValue(String indexString) 
		throws MissingParameterException, BadParameterException
	{	
		if(indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern " + StatisticSearch.INDEX_PARAMETER + " saknas eller är tom", "APIMethodFactory.getStatisticSearchObject", "index parametern saknas", false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<String,String>();
		while(indexTokenizer.hasMoreTokens()) {
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if(tokens.length < 2) {
				throw new BadParameterException("parametern " +  StatisticSearch.INDEX_PARAMETER + " är felskriven", "APIMethodFactory.getStatisticSearchObject", "syntax error i index parametern", false);
			}
			for(int i = 0; i < 2; i++) {
				if(i == 0) {
					index = tokens[i];
				}
				if(i == 1) {
					value = tokens[i];
				}
			}
			indexMap.put(index, value);
		}
		return indexMap;
	}
}