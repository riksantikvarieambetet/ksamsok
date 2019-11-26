package se.raa.ksamsok.api.method;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.w3c.dom.Element;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * söka statistik
 * @author Henrik Hjalmarsson
 */
public class Statistic extends AbstractAPIMethod {
	/** namnet på metoden */
	public static final String METHOD_NAME = "statistic";
	/** namnet på parametern som håller medskickade index */
	public static final String INDEX_PARAMETER = "index";
	/** namn på parameter för att ta bort nollor i svars XML */
	public static final String REMOVE_BELOW = "removeBelow";
	/** max antal kombinationer av indexvärden */
	protected static final int MAX_CARTESIAN_COUNT = 20000;
	
	//set med index som skall kollas
	protected Map<String,String> indexMap;
	protected int removeBelow = 0;

	protected List<QueryContent> queryResults;

	/**
	 * Skapar ett nytt statistic objekt
	 * @param indexes de index som skall scannas
	 * @param out används för att skriva ut svaret
	 * @throws DiagnosticException 
	 */
	public Statistic(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException{
		super(serviceProvider, out, params);
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
		Map<String, List<Term>> termMap;
		try {
			// TODO: om bara ett index borde man kunna köra facet rakt av
			SolrQuery query = new SolrQuery();
			query.setQuery("*");
			query.setRows(0);
			// en mängd med mängder med mängder!
			termMap = buildTermMap();
			if (getCartesianCount(termMap)  > MAX_CARTESIAN_COUNT) {
				throw new BadParameterException("Den kartesiska produkten av inskickade index blir för stor för att utföra denna operation.", "Statistic.performMethod", null, false);
			}
			//gör en kartesisk produkt på de värden i termMap
			queryResults = cartesian(termMap);

			for (int i = 0; i < queryResults.size(); i++) {
				QueryContent content = queryResults.get(i);
				String qs = content.getQueryString().replace("=", ":"); // TODO: bort nu?
				query.setQuery(qs);
				QueryResponse qr = serviceProvider.getSearchService().query(query);

				if (qr.getResults().getNumFound() >= removeBelow) {
					content.setHits(qr.getResults().getNumFound());
					queryResults.set(i, content);
				} else {
					queryResults.remove(i);
					i--;
				}
			}
		} catch(OutOfMemoryError e) {
			throw new DiagnosticException("De inskickade index värdena gav upphov till att för många värden hittades och denna sökning gick ej att utföra", "Statistic.performMethod", null, false);
		} catch (Exception e) {
			throw new DiagnosticException("Oväntat fel uppstod", "Statistic.performMethod", null, false);
		}
	}
	
	/**
	 * bygger en kartesisk produkt av x antal mängder med n antal element
	 * @param data som skall göra kartesisk produkt av
	 * @return Kartesisk produkt av indata som en lista
	 * @throws MissingParameterException
	 */
	protected static List<QueryContent> cartesian(Map<String, List<Term>> data)
			throws MissingParameterException {
		String index1 = null;
		String index2 = null;
		List<QueryContent> result = null;
		//lite special cases ifall den bara gick 0 eller 1 varv
		for (String index : data.keySet()) {
			if (index1 == null) {//körs första varvet
				index1 = index;
			} else if (index2 == null) {//körs andra varvet
				index2 = index;
				result = cartesian(index1, index2, data.get(index1),
						data.get(index2));
				continue;
			} else {//körs resten av varven
				index1 = index;
				result = cartesian(index1, data.get(index1), result);
			}
		}
		if (index1 == null && index2 == null) {
			throw new MissingParameterException("minst ett index behövs för denna operation", "Statistic.cartesian", null, false);
		} else if (index1 != null && index2 == null) {
			result = cartesianWithOneIndex(data, index1);
		}
		return result;
	}
	
	/**
	 * körs om endast ett index finns
	 * @param data
	 * @param index1
	 * @return
	 */
	private static List<QueryContent> cartesianWithOneIndex(
			Map<String,List<Term>> data, String index1)
	{
		List<QueryContent> result = new ArrayList<>();
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
	static int getCartesianCount(Map<String,List<Term>> data)
	{
		int count = 1;
		for(List<Term> termsList : data.values()) {
			count *= termsList.size();
		}
		return count;
	}
	
	/**
	 * Bygger kartesisk produkt av värden i given lista och värden i givet set
	 * @param index för tillhörande set
	 * @param set med värden
	 * @param list med värden
	 * @return ny lista med kartesisk produkt av indata
	 */
	private static List<QueryContent> cartesian(String index, List<Term> set,
			List<QueryContent> list)
	{
		List<QueryContent> result = new ArrayList<>();
		for (QueryContent aList : list) {
			for (Term term : set) {
				Map<String, String> map = aList.getTermMap();
				QueryContent content = new QueryContent();
				map.forEach(content::addTerm);
				content.addTerm(index, term.getValue());
				result.add(content);
			}
		}
		return result;
	}
	
	/**
	 * bygger kartesisk produkt av de två givna setten
	 * @param index1 tillhörande set1
	 * @param index2 tillhörande set2
	 * @param set1 med värden
	 * @param set2 med värden
	 * @return lista med kartesisk produkt av de båda setten
	 */
	private static List<QueryContent> cartesian(String index1, String index2,
			List<Term> set1, List<Term> set2)
	{
		List<QueryContent> result = new ArrayList<>();
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
	 * @param searcher som används för att söka i index
	 * @param indexMap med index och sökvärden
	 * @return Map<String,Set<Term>> med index och dess termer
	 * @throws BadParameterException 
	 */
	protected Map<String, List<Term>> buildTermMap() throws DiagnosticException,
		BadParameterException {
		String indexValue;
		HashMap<String, List<Term>> termMap = new HashMap<>();

		for (Map.Entry<String, String> entry : indexMap.entrySet()) {
			try {
				String index = entry.getKey();
				indexValue = CQL2Solr.translateIndexName(index);
				if (!ContentHelper.indexExists(indexValue)) {
					throw new BadParameterException("Indexet " + index + " existerar inte", "Statistic.buildTermMap", null, false);
				}
				String value = entry.getValue();
				if (ContentHelper.isToLowerCaseIndex(indexValue) || ContentHelper.isAnalyzedIndex(indexValue)) {
					value = value != null ? value.toLowerCase() : null;
				}
				// snabbfiltrering, finns det inte ens tillräckligt många träffar
				// totalt så finns det ju inte sen i sökningen heller
				List<Term> terms = serviceProvider.getSearchService().terms(indexValue, value, removeBelow, -1);
				List<Term> extractedTerms = new LinkedList<>(terms);
				termMap.put(indexValue, extractedTerms);
			} catch (SolrServerException | IOException e) {
				throw new DiagnosticException("Oväntat fel uppstod. Var god försök igen", "Statistic.buildTermMap", e.getMessage(), true);
			}
		}
		return termMap;
	}

	@Override
	protected void generateDocument() {
		Element result = generateBaseDocument();
		// echo
		Element echo = doc.createElement("echo");
		result.appendChild(echo);
		// method
		Element method = doc.createElement("method");
		method.appendChild(doc.createTextNode(METHOD_NAME));
		echo.appendChild(method);
		indexMap.forEach((key, value) -> {
			// index
			Element index = doc.createElement("index");
			index.appendChild(doc.createTextNode(key + "=" + value));
			echo.appendChild(index);
		});

		Element removeBelowEl = doc.createElement(REMOVE_BELOW);
		removeBelowEl.appendChild(doc.createTextNode(Integer.toString(removeBelow)));
		echo.appendChild(removeBelowEl);
	}
	
	protected Element generateBaseDocument(){
		//Root element
		Element result = super.generateBaseDocument();
		//Number of terms
		Element numberOfTerms = doc.createElement("numberOfTerms");
		numberOfTerms.appendChild(doc.createTextNode(Integer.toString(queryResults.size(),10)));
		result.appendChild(numberOfTerms);
		for (QueryContent queryContent : queryResults) {
			// term
			Element term = doc.createElement("term");
			for (String indexKey : queryContent.getTermMap().keySet()) {
				// indexFields
				Element indexFields = doc.createElement("indexFields");
				// index
				Element index = doc.createElement("index");
				index.appendChild(doc.createTextNode(indexKey));
				indexFields.appendChild(index);
				// value
				Element value = doc.createElement("value");
				value.appendChild(doc.createTextNode(queryContent.getTermMap().get(indexKey)));
				indexFields.appendChild(value);
				term.appendChild(indexFields);
			}
			// records
			Element records = doc.createElement("records");
			records.appendChild(doc.createTextNode(Long.toString(queryContent.getHits(), 10)));
			term.appendChild(records);
			result.appendChild(term);
		}
		return result;
	}

//	/**
//	 * skriver ut nedre delen av svars XML
//	 * @throws IOException 
//	 */
//	@Override
//	protected void writeFootExtra() throws IOException {
//		xmlWriter.writeEntity("echo");
//		xmlWriter.writeEntityWithText("method", Statistic.METHOD_NAME);
//		for(String index : indexMap.keySet()) {
//			xmlWriter.writeEntityWithText("index", index + "=" + indexMap.get(index));
//		}
//		xmlWriter.endEntity();
//	}
//
//	/**
//	 * skriver ut resultat
//	 * @param queryResults
//	 * @throws IOException 
//	 */
//	@Override
//	protected void writeResult() throws IOException {
//		for(int i = 0; i < queryResults.size(); i++) {
//			QueryContent queryContent = queryResults.get(i);
//			xmlWriter.writeEntity("term");
//			for(String index : queryContent.getTermMap().keySet()) {
//				xmlWriter.writeEntity("indexFields");
//				xmlWriter.writeEntityWithText("index", index);
//				xmlWriter.writeEntityWithText("value", queryContent.getTermMap().get(index));
//				xmlWriter.endEntity();
//			}
//			xmlWriter.writeEntityWithText("records", queryContent.getHits());
//			xmlWriter.endEntity();
//		}
//	}
//
//	/**
//	 * skriver ut övre del av svars XML
//	 * @param queryResults
//	 * @throws IOException 
//	 */
//	@Override
//	protected void writeHeadExtra() throws IOException {
//		xmlWriter.writeEntityWithText("numberOfTerms", queryResults.size());
//	}

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
	public Map<String,String> getIndexMapDoubleValue(String indexString) 
		throws MissingParameterException, BadParameterException {	
		if (indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern " + INDEX_PARAMETER + " saknas eller är tom", "APIMethodFactory.getStatisticSearchObject", "index parametern saknas", false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		HashMap<String,String> indexMap = new HashMap<>();
		while (indexTokenizer.hasMoreTokens()) {
			String[] tokens = indexTokenizer.nextToken().split("=");
			String index = null;
			String value = null;
			if (tokens.length < 2) {
				throw new BadParameterException("parametern " +  INDEX_PARAMETER + " är felskriven", "APIMethodFactory.getStatisticSearchObject", "syntax error i index parametern", false);
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