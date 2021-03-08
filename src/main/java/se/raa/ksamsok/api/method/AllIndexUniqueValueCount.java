package se.raa.ksamsok.api.method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utför metoden allIndexUniqueValue count som returnerar en lista över index
 * och hur många unika värden dessa index har som matchar givet query.
 * @author Henrik Hjalmarsson
 */
public class AllIndexUniqueValueCount extends AbstractAPIMethod {


	private static final Logger logger = LogManager.getLogger(AllIndexUniqueValueCount.class);

	/** metodens namn */
	public static final String METHOD_NAME = "allIndexUniqueValueCount";
	/** namnet på parametern som håller medskickade index */
	public static final String INDEX_PARAMETER = "index";
	/** parameternamn där query skickas in */
	public static final String QUERY_PARAMS = "query";


	private static final Map<String, String> defaultIndexMap;

	static {
		// kopierat från LuceneServlet
		Map<String, String> im = new HashMap<>();
		try {
			String fileName = "/index.xml";
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
	protected String originalQueryString;

	protected List<FacetField> facetFields = Collections.emptyList();

	public AllIndexUniqueValueCount(APIServiceProvider serviceProvider, OutputStream out, Map<String, String> params) throws DiagnosticException{
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		final String[] queryStrings = getQueryString(params.get(QUERY_PARAMS));
		this.queryString = queryStrings[0];
		this.originalQueryString = queryStrings[1];
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
		} catch (CQLParseException | BadParameterException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod. Detta beror troligen på att CQL syntax ej följs. Var god kontrollera query sträng eller kontakta systemadministratör för söksystemet du använder", "AllIndexUniqueValueCount.performMethod", e.getMessage(),	true);
		}
	} 

	private void checkIfIndexExists(String index) throws BadParameterException {
		if (!ContentHelper.indexExists(index)) {
			throw new BadParameterException("Indexet " + index + " existerar inte.", "AllIndexUniqueValueCount.doAllindexUniqueValueCount", null, false);
		}
	}

	@Override
	protected void generateDocument() {
		//Root element
		Element result = doc.createElement("result");
		doc.appendChild(result);
		//Version element
		Element version = doc.createElement("version");
		version.appendChild(doc.createTextNode(APIMethod.API_VERSION));
		result.appendChild(version);

		for (FacetField ff: facetFields) {
			int vc = ff.getValueCount();
			if (vc > 0) {
				//Index 
				Element index = doc.createElement("index");
				//Name of index
				Element name = doc.createElement("name");
				name.appendChild(doc.createTextNode(ff.getName()));
				index.appendChild(name);
				//Number of values of index
				Element uniqueValues =  doc.createElement("uniqueValues");
				uniqueValues.appendChild(doc.createTextNode(Integer.toString(vc, 10)));
				index.appendChild(uniqueValues);
				result.appendChild(index);
			}
		}
		
		//Echo
		Node echo = doc.createElement("echo");
		Node method = doc.createElement(METHOD);
		method.appendChild(doc.createTextNode(METHOD_NAME));
		echo.appendChild(method);
		for (String s : indexMap.keySet()) {
			Node index = doc.createElement("index");
			index.appendChild(doc.createTextNode(s));
			echo.appendChild(index);
		}
		Node query = doc.createElement("query");
		query.appendChild(doc.createTextNode(originalQueryString));
		echo.appendChild(query);
		result.appendChild(echo);
	}
}