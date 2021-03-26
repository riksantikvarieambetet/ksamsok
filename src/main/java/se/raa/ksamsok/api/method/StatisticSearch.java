package se.raa.ksamsok.api.method;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParser;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * utför en statisticSearch operation
 * @author Henrik Hjalmarsson
 */
public class StatisticSearch extends Statistic {
	protected String queryString = null;
	protected String originalQueryString = null;

	/** metodens namn */
	public static final String METHOD_NAME = "statisticSearch";
	/** parameternamn där query skickas in */
	public static final String QUERY_PARAMS = "query";

	/**
	 * skapar ett objekt av StatisticSearch
	 * @param out används för att skriva svar
	 * @param queryString sträng med query
	 * @param indexMap set med index namn
	 * @throws DiagnosticException 
	 */
	public StatisticSearch(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException{
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		super.extractParameters();
		final String[] queryStrings = getQueryString(params.get(QUERY_PARAMS));
		this.queryString = queryStrings[0];
		this.originalQueryString = queryStrings[1];
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		Map<String, List<Term>> termMap;
		try {
			// TODO: om bara ett index borde man kunna köra facet rakt av
			SolrQuery query = new SolrQuery();
			query.setQuery("*");

			// TODO: enda som skiljer från super? skapa metod att overrida?
			// använd frågan som filter
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			String queryString = CQL2Solr.makeQuery(node);
			query.addFilterQuery(queryString);

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
				String qs = content.getQueryString().replace("=", ":");
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
			throw new DiagnosticException("De inskickade indexvärdena gav upphov till att för många värden hittades och denna sökning gick ej att utföra", "Statistic.performMethod", null, false);
		} catch (Exception e) {
			throw new DiagnosticException("Oväntat fel uppstod", "Statistic.performMethod", null, false);
		}
	}
	@Override
	protected void generateDocument() {
		super.generateDocument();
		NodeList echoNodes = doc.getElementsByTagName("echo");
		if (echoNodes.getLength()==1){
			Element query = doc.createElement("query");
			query.appendChild(doc.createTextNode(originalQueryString));
			echoNodes.item(0).appendChild(query);
		} else if (echoNodes.getLength()<1){
			logger.error("Did not find any echo node in the respond document");
		} else {
			logger.error("Found too many echo nodes in the respond document, number of echo-nodes: "+echoNodes.getLength());
		}
	}
	protected Element generateBaseDocument(){
		return super.generateBaseDocument();
	}

//	@Override
//	protected void writeFootExtra() throws IOException {
//		xmlWriter.writeEntity("echo");
//		xmlWriter.writeEntityWithText("method", METHOD_NAME);
//		for (String index : indexMap.keySet()) {
//			xmlWriter.writeEntityWithText("index", index + "=" + indexMap.get(index));
//		}
//		xmlWriter.writeEntityWithText("query", queryString);
//		xmlWriter.endEntity();
//	}

}