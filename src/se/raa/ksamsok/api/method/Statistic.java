package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * s�ka statistik
 * @author Henrik Hjalmarsson
 */
public class Statistic extends AbstractAPIMethod {
	/** namnet p� metoden */
	public static final String METHOD_NAME = "statistic";
	/** namnet p� parametern som h�ller medskickade index */
	public static final String INDEX_PARAMETER = "index";
	/** namn p� parameter f�r att ta bort nollor i svars XML */
	public static final String REMOVE_BELOW = "removeBelow";
	/** max antal kombinationer av indexv�rden */
	protected static final int MAX_CARTESIAN_COUNT = 20000;
	
	//set med index som skall kollas
	protected Map<String,String> indexMap;
	protected int removeBelow = 0;

	protected List<QueryContent> queryResults;

	/**
	 * Skapar ett nytt statistic objekt
	 * @param indexes de index som skall scannas
	 * @param writer anv�nds f�r att skriva ut svaret
	 */
	public Statistic(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
		queryResults = Collections.emptyList();
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		this.indexMap = extractIndexMap();
		this.removeBelow = getRemoveBelow(params.get(REMOVE_BELOW));
	}

	protected Map<String, String> extractIndexMap() throws MissingParameterException, BadParameterException {
		return getIndexMapDoubleValue(params.get(INDEX_PARAMETER));
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		Map<String, List<Term>> termMap = null;
		try {
			// TODO: om bara ett index borde man kunna k�ra facet rakt av
			SolrQuery query = new SolrQuery();
			query.setQuery("*");
			query.setRows(0);
			// en m�ngd med m�ngder med m�ngder!
			termMap = buildTermMap();
			if (getCartesianCount(termMap)  > MAX_CARTESIAN_COUNT) {
				throw new BadParameterException("Den kartesiska produkten av inskickade index blir f�r stor f�r att utf�ra denna operation.", "Statistic.performMethod", null, false);
			}
			//g�r en kartesisk produkt p� de v�rden i termMap
			queryResults = cartesian(termMap);

			for (int i = 0; i < queryResults.size(); i++) {
				QueryContent content = queryResults.get(i);
				String qs = content.getQueryString().replace("=", ":"); // TODO: bort nu?
				query.setQuery(qs);
				QueryResponse qr = serviceProvider.getSearchService().query(query);

				if (qr.getResults().getNumFound() >= removeBelow) {
					content.setHits((int) qr.getResults().getNumFound());
					queryResults.set(i, content);
				} else {
					queryResults.remove(i);
					i--;
				}
			}
		} catch(OutOfMemoryError e) {
			throw new DiagnosticException("De inskickade index v�rdena gav upphov till att f�r m�nga v�rden hittades och denna s�kning gick ej att utf�ra", "Statistic.performMethod", null, false);
		} catch (Exception e) {
			throw new DiagnosticException("Ov�ntat fel uppstod", "Statistic.performMethod", null, false);
		}
	}
	
	/**
	 * bygger en kartesisk produkt av x antal m�ngder med n antal element
	 * @param data som skall g�ra kartesisk produkt av
	 * @return Kartesisk produkt av indata som en lista
	 * @throws MissingParameterException
	 */
	protected static List<QueryContent> cartesian(Map<String,List<Term>> data)
		throws MissingParameterException
	{
		String index1 = null;
		String index2 = null;
		List<QueryContent> result = null;
		//lite special cases ifall den bara gick 0 eller 1 varv
		for(String index : data.keySet()) {
			if(index1 == null) {//k�rs f�rsta varvet
				index1 = index;
				continue;
			}else if(index2 == null) {//k�rs andra varvet
				index2 = index;
				result = cartesian(index1, index2, data.get(index1),
						data.get(index2));
				continue;
			}else {//k�rs resten av varven
				index1 = index;
				result = cartesian(index1, data.get(index1), result);
			}		
		}
		if(index1 == null && index2 == null) {
			throw new MissingParameterException("minst ett index beh�vs f�r denna operation", "Statistic.cartesian", null, false);
		}else if(index1 != null && index2 == null) {
			result = cartesianWithOneIndex(data, index1);
		}
		return result;
	}
	
	/**
	 * k�rs om endast ett index finns
	 * @param data
	 * @param index1
	 * @return
	 */
	private static List<QueryContent> cartesianWithOneIndex(
			Map<String,List<Term>> data, String index1)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(Term term : data.get(index1))
		{
			QueryContent content = new QueryContent();
			content.addTerm(index1, term.getValue());
			result.add(content);
		}
		return result;
	}
	
	/**
	 * Kollar hur stor den kartesiska produkten kommer bli
	 * @param data
	 * @return
	 */
	protected int getCartesianCount(Map<String,List<Term>> data)
	{
		int count = 1;
		for(String index : data.keySet())
		{
			count *= data.get(index).size();
		}
		return count;
	}
	
	/**
	 * Bygger kartesisk produkt av v�rden i given lista och v�rden i givet set
	 * @param index f�r tillh�rande set
	 * @param set med v�rden
	 * @param list med v�rden
	 * @return ny lista med kartesisk produkt av indata
	 */
	private static List<QueryContent> cartesian(String index, List<Term> set,
			List<QueryContent> list)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(int i = 0; i < list.size(); i++)
		{
			for(Term term : set)
			{
				Map<String,String> map = list.get(i).getTermMap();
				QueryContent content = new QueryContent();
				for(String index2 : map.keySet())
				{	
					content.addTerm(index2, map.get(index2));
				}
				content.addTerm(index, term.getValue());
				result.add(content);
			}
		}
		return result;
	}
	
	/**
	 * bygger kartesisk produkt av de tv� givna setten
	 * @param index1 tillh�rande set1
	 * @param index2 tillh�rande set2
	 * @param set1 med v�rden
	 * @param set2 med v�rden
	 * @return lista med kartesisk produkt av de b�da setten
	 */
	private static List<QueryContent> cartesian(String index1, String index2,
			List<Term> set1, List<Term> set2)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(Term term1 : set1)
		{
			for(Term term2 : set2)
			{
				QueryContent content = new QueryContent();
				content.addTerm(index1, term1.getValue());
				content.addTerm(index2, term2.getValue());
				result.add(content);
			}
		}
		return result;
	}
	
	/**
	 * bygger en term map av den inskickade mappen
	 * @param searcher som anv�nds f�r att s�ka i index
	 * @param indexMap med index och s�kv�rden
	 * @return Map<String,Set<Term>> med index och dess termer
	 * @throws BadParameterException 
	 */
	protected Map<String, List<Term>> buildTermMap() throws DiagnosticException,
		BadParameterException {
		String indexValue;
		HashMap<String, List<Term>> termMap = new HashMap<String, List<Term>>();
		for(String index : indexMap.keySet()) {
			try {
				indexValue = CQL2Solr.translateIndexName(index);
				if (!ContentHelper.indexExists(indexValue)) {
					throw new BadParameterException("Indexet " + index + " existerar inte", "Statistic.buildTermMap", null, false);
				}
				List<Term> extractedTerms = new LinkedList<Term>();
				String value = indexMap.get(index);
				if (ContentHelper.isToLowerCaseIndex(indexValue) || ContentHelper.isAnalyzedIndex(indexValue)) {
					value = value != null ? value.toLowerCase() : value;
				}
				// snabbfiltrering, finns det inte ens tillr�ckligt m�nga tr�ffar
				// totalt s� finns det ju inte sen i s�kningen heller
				List<Term> terms = serviceProvider.getSearchService().terms(indexValue, value, removeBelow, -1);
				extractedTerms.addAll(terms);
				termMap.put(indexValue, extractedTerms);
			} catch(SolrServerException e) {
				throw new DiagnosticException("Ov�ntat fel uppstod. Var god f�rs�k igen", "Statistic.buildTermMap", e.getMessage(), true);
			}
		}
		return termMap;
	}


	/**
	 * skriver ut nedre delen av svars XML
	 */
	@Override
	protected void writeFootExtra() {
		writer.println("<echo>");
		writer.println("<method>" + Statistic.METHOD_NAME + "</method>");
		for(String index : indexMap.keySet()) {
			writer.println("<index>" + index + "=" + indexMap.get(index) + "</index>");
		}
		writer.println("</echo>");
	}

	/**
	 * skriver ut resultat
	 * @param queryResults
	 */
	@Override
	protected void writeResult() {
		for(int i = 0; i < queryResults.size(); i++) {
			QueryContent queryContent = queryResults.get(i);
			writer.println("<term>");
			for(String index : queryContent.getTermMap().keySet()) {
				writer.println("<indexFields>");
				writer.print("<index>");
				writer.print(index);
				writer.println("</index>");
				writer.print("<value>");
				//xmlEscape snodde jag ur SRUServlet
				writer.print(StaticMethods.xmlEscape(
						queryContent.getTermMap().get(index)));
				writer.println("</value>");
				writer.println("</indexFields>");
			}
			writer.print("<records>");
			writer.print(queryContent.getHits());
			writer.println("</records>");
			writer.println("</term>");
		}
	}

	/**
	 * skriver ut �vre del av svars XML
	 * @param queryResults
	 */
	@Override
	protected void writeHeadExtra() {
		//skriver ut hur m�nga v�rden det blev
		writer.println("<numberOfTerms>" + queryResults.size() +
				"</numberOfTerms>");
	}

	/**
	 * returnerar remove Below
	 * @param removeBelowString
	 * @return
	 * @throws BadParameterException
	 */
	public int getRemoveBelow(String removeBelowString) throws BadParameterException {
		int removeBelow = 0;
		if (removeBelowString != null) {
			try {
				removeBelow = Integer.parseInt(removeBelowString);
			} catch(NumberFormatException e) {
				throw new BadParameterException("Parametern removeBelow m�ste inneh�lla ett numeriskt v�rde", "APIMethodFactory.getRemoveBelow", null, false);
			}
		}
		return removeBelow;
	}

	/**
	 * returnerar en index lista d�r alla har samma v�rde
	 * @param indexString
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	public Map<String,String> getIndexMapDoubleValue(String indexString) 
		throws MissingParameterException, BadParameterException {	
		if (indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern " + INDEX_PARAMETER + " saknas eller �r tom", "APIMethodFactory.getStatisticSearchObject", "index parametern saknas", false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<String,String>();
		while (indexTokenizer.hasMoreTokens()) {
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if (tokens.length < 2) {
				throw new BadParameterException("parametern " +  INDEX_PARAMETER + " �r felskriven", "APIMethodFactory.getStatisticSearchObject", "syntax error i index parametern", false);
			}
			for (int i = 0; i < 2; i++) {
				if (i == 0) {
					index = tokens[i];
				}
				if (i == 1) {
					value = tokens[i];
				}
			}
			indexMap.put(index, value);
		}
		return indexMap;
	}
}