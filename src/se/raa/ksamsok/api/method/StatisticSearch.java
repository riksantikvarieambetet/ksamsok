package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;

/**
 * utför en statisticSearch operation
 * @author Henrik Hjalmarsson
 */
public class StatisticSearch extends Statistic {
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
	public StatisticSearch(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		super.extractParameters();
		queryString = getQueryString(params.get(QUERY_PARAMS));
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		Map<String, List<Term>> termMap = null;
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
					content.setHits((int) qr.getResults().getNumFound());
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
	protected void writeFootExtra() {
		writer.println("<echo>");
		writer.println("<method>" + METHOD_NAME + "</method>");
		for (String index : indexMap.keySet()) {
			writer.println("<index>" + index + "=" + indexMap.get(index) + "</index>");
		}
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) + "</query>");
		writer.println("</echo>");
	}

}