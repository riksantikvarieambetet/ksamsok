package se.raa.ksamsok.api.method;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Utför metoden allIndexUniqueValue count som returnerar en lista över index
 * och hur många unika värden dessa index har som matchar givet query.
 * @author Henrik Hjalmarsson
 */
public class AllIndexUniqueValueCount extends AbstractAPIMethod {

	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.method.AllIndexUniqueValueCount");

	/** metodens namn */
	public static final String METHOD_NAME = "allIndexUniqueValueCount";
	/** namnet på parametern som håller medskickade index */
	public static final String INDEX_PARAMETER = "index";
	/** parameternamn där query skickas in */
	public static final String QUERY_PARAMS = "query";


	private static final Map<String, String> defaultIndexMap;

	private static final String PATH = "/" + ContentHelper.class.getPackage().getName().replace('.', '/') + "/";

	static {
		// kopierat från LuceneServlet
		Map<String, String> im = new HashMap<String, String>();
		try {
			String fileName = PATH + "index.xml";
			DataInputStream input = new DataInputStream(ContentHelper.class.getResourceAsStream(fileName));
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document xmlDocument = builder.parse(input);
			xmlDocument.getDocumentElement().normalize();
			NodeList indexList = xmlDocument.getElementsByTagName("index");
			for(int i = 0; i < indexList.getLength(); i++) {
				Node node = indexList.item(i);
				im.put(node.getTextContent(),"*");
			}
		} catch (Exception e) {
			logger.error("Fel vid inläsning av default-index", e);
		}
		defaultIndexMap = Collections.unmodifiableMap(im);
	}

	protected Map<String,String> indexMap;
	protected String queryString;

	protected List<FacetField> facetFields = Collections.emptyList();

	/**
	 * skapar ett objekt av AllIndexUniqueValueCount från given query sträng
	 * och writer som skall skriva resultatet.
	 * @param queryString
	 * @param writer
	 */
	public AllIndexUniqueValueCount(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		queryString = getQueryString(params.get(QUERY_PARAMS));
		String indexString = params.get(INDEX_PARAMETER);
		if (indexString != null) {
			indexMap = getIndexMapSingleValue(indexString, "*");
		}
		if (indexMap == null) {
			indexMap = defaultIndexMap;
		} else {
			for (Entry<String, String> indexEntry: indexMap.entrySet()) {
				checkIfIndexExists(indexEntry.getKey());
			}
		}

	}
	@Override
	protected void performMethodLogic() throws DiagnosticException {
		try {
			SolrQuery query = new SolrQuery();
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			String solrQueryString = CQL2Solr.makeQuery(node);

			query.setQuery(solrQueryString);
			query.setFacet(true);
			query.setFacetMinCount(1);
			query.setRows(0);
			for (Entry<String, String> entry: indexMap.entrySet()) {
				query.addFacetField(entry.getKey());
			}
			QueryResponse qr = serviceProvider.getSearchService().query(query);
			facetFields = qr.getFacetFields();
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod", "AllIndexUniqueValueCount.performMethod", e.getMessage(), true);
		} catch (SolrServerException e) {
			throw new DiagnosticException("Oväntat fel uppstod", "AllIndexUniqueValueCount.performMethod", e.getMessage(), true);
		} catch (CQLParseException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod. Detta beror troligen på att CQL syntax ej följs. Var god kontrollera query sträng eller kontakta systemadministratör för söksystemet du använder", "AllIndexUniqueValueCount.performMethod", e.getMessage(),	true);
		} catch (BadParameterException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod. Detta beror troligen på att CQL syntax ej följs. Var god kontrollera query sträng eller kontakta systemadministratör för söksystemet du använder", "AllIndexUniqueValueCount.performMethod", e.getMessage(),	true);
		}
	} 

	private void checkIfIndexExists(String index) throws BadParameterException {
		if (!ContentHelper.indexExists(index)) {
			throw new BadParameterException("Indexet " + index + " existerar inte.", "AllIndexUniqueValueCount.doAllindexUniqueValueCount", null, false);
		}
	}

	/**
	 * skriver ut resultatet
	 */
	@Override
	protected void writeResult() {
		for (FacetField ff: facetFields) {
			int vc = ff.getValueCount();
			if (vc > 0) {
				writer.println("<index>");
				writer.println("<name>" + ff.getName() + "</name>");
				writer.println("<uniqueValues>" + vc + "</uniqueValues>");
				writer.println("</index>");
			}
		}

	}
}