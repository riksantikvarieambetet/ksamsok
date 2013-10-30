package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Solr;

/**
 * Klass gjort för att enkelt implementera facet sökningar i TA
 * @author Henrik Hjalmarsson
 */
public class Facet extends StatisticSearch {	
	/** metodens namn */
	public static final String METHOD_NAME = "facet";
	
	//private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.method.Facet");

	/**
	 * skapar ett objekt av Facet
	 * @param indexMap de index som skall ingå i facetten
	 * @param writer för att skriva resultatet
	 * @param queryString filtrerar resultatet efter query
	 */
	public Facet(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params); 
	}

	protected Map<String, String> extractIndexMap() throws MissingParameterException, BadParameterException {
		return getIndexMapSingleValue(params.get(INDEX_PARAMETER), "*");
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException  {
		try {
			SolrQuery query = new SolrQuery();
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			String queryString = CQL2Solr.makeQuery(node);
			query.setQuery(queryString);
			query.setFacet(true);
			query.setFacetMinCount(removeBelow);
			query.setRows(0);
			for (Entry<String, String> entry: indexMap.entrySet()) {
				query.addFacetField(entry.getKey());
			}
			QueryResponse qr = serviceProvider.getSearchService().query(query);
			List<FacetField> facetFields = qr.getFacetFields();
			if (facetFields != null && facetFields.size() > 0) {
				queryResults = new LinkedList<QueryContent>();
				for (FacetField ff: facetFields) {
					List<Count> facetValues = ff.getValues();
					if (facetValues != null && facetValues.size() > 0) {
						for (Count value: facetValues) {
							QueryContent qc = new QueryContent();
							qc.addTerm(ff.getName(), value.getName());
							qc.setHits((int) value.getCount()); // TODO: int/long
							queryResults.add(qc);
						}
					}
				}
			}
		} catch (CQLParseException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod - detta beror troligen på att query strängen inte följer CQL syntax. Var god kontrollera query-strängen eller kontakta systemadministratören för systemet du använder dig av.", "Facet.performMethod", null, false);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel, var god försök igen", "Facet.performMethod", e.getMessage(), true); 
		} catch (SolrServerException e) {
			throw new DiagnosticException("Oväntat sök-fel, var god försök igen", "Facet.performMethod", e.getMessage(), true);
		} catch (BadParameterException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod - detta beror troligen på att query strängen inte följer CQL syntax. Var god kontrollera query-strängen eller kontakta systemadministratören för systemet du använder dig av.", "Facet.performMethod", null, false);
		}
	}

	@Override
	protected void writeFootExtra() throws IOException {
		xmlWriter.writeEntity("echo");
		xmlWriter.writeEntityWithText("method", METHOD_NAME);
		for(String index : indexMap.keySet()) {
			xmlWriter.writeEntityWithText("index", index);
		}
		xmlWriter.writeEntityWithText("query", queryString);
		xmlWriter.endEntity();
	}

}