package se.raa.ksamsok.api.method;

import org.apache.solr.client.solrj.SolrServerException;
import org.w3c.dom.Element;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Utför en prefix sökning för att ge förslag på fortsättningar av ett givet query
 * TODO: denna klarar bara ett index trots att dok på kulturarvsdata.se säger att den ska klara
 *       fler - det enda som händer om man anger fler är att man får resultatet av det sista indexet...
 * @author Henrik Hjalmarsson
 */
public class SearchHelp extends AbstractAPIMethod {
	//private static final Logger logger = LogManager.getLogger(SearchHelp.class);
	
	/** metodens namn */
	public static final String METHOD_NAME = "searchHelp";
	/** parameter namn för prefix */
	public static final String PREFIX_PARAMETER = "prefix";
	/** parameter namn för hur många förslag som önskas */
	public static final String MAX_VALUE_COUNT_PARAMETER = "maxValueCount";
	/** parameter namn för index */
	public static final String INDEX_PARAMETER = "index";
	/** default värde för max value count */
	public static final int DEFAULT_MAX_VALUE_COUNT = 3;

	protected List<Term> termList = Collections.emptyList();

	private String prefix;
	private int maxValueCount;
	private List<String> indexList;

	/**
	 * skapar ett objekt av SearchHelp
	 * @param out
	 * @throws DiagnosticException TODO
	 */
	public SearchHelp(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		indexList = getIndexList(params.get(INDEX_PARAMETER));
		prefix = getPrefix(params.get(PREFIX_PARAMETER));
		maxValueCount = getMaxValueCount(params.get(MAX_VALUE_COUNT_PARAMETER));
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		// TODO: detta är fel då endast ett index stöds, men det är exakt som innan funktionsmässigt, se TODO ovan
		try {
			for (String index : indexList) {
				index = CQL2Solr.translateIndexName(index);
				if (ContentHelper.isToLowerCaseIndex(index) || ContentHelper.isAnalyzedIndex(index)) {
					prefix = prefix != null ? prefix.toLowerCase() : null;
				}
				termList = serviceProvider.getSearchService().terms(index, prefix, 0, maxValueCount);
			}
		} catch (SolrServerException | IOException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod", "SearchHelp.performMethod", e.getMessage(), true);
		}
	}
	@Override
	protected void generateDocument() {
		Element result = super.generateBaseDocument();
		
		Element numberOfTerms = doc.createElement("numberOfTerms");
		numberOfTerms.appendChild(doc.createTextNode(Integer.toString(termList.size(),10)));
		result.appendChild(numberOfTerms);
		
		Element terms = doc.createElement("terms");
		result.appendChild(terms);
		for (Term t : termList){
			Element term = doc.createElement("term");
			
			Element value = doc.createElement("value");
			value.appendChild(doc.createTextNode(t.getValue()));
			term.appendChild(value);
			
			Element count = doc.createElement("count");
			count.appendChild(doc.createTextNode(Long.toString(t.getCount(),10)));
			term.appendChild(count);
			
			terms.appendChild(term);
		}
		Element echo = doc.createElement("echo");
		result.appendChild(echo);
		
		Element method = doc.createElement("method");
		method.appendChild(doc.createTextNode(METHOD_NAME));
		echo.appendChild(method);

		for (String anIndexList : indexList) {
			Element index = doc.createElement("index");
			index.appendChild(doc.createTextNode(anIndexList));
			echo.appendChild(index);
		}
		
		Element maxValueCountEl = doc.createElement("maxValueCount");
		maxValueCountEl.appendChild(doc.createTextNode(Integer.toString(maxValueCount,10)));
		echo.appendChild(maxValueCountEl);
		
		Element prefixEl = doc.createElement("prefix");
		prefixEl.appendChild(doc.createTextNode(prefix));
		echo.appendChild(prefixEl);
	}

	/**
	 * returnerar en lista med index
	 * @param indexString
	 * @return
	 * @throws MissingParameterException
	 */
	public List<String> getIndexList(String indexString)  throws MissingParameterException {
		List<String> indexList = new ArrayList<>();
		if (indexString == null || indexString.trim().length() < 1) {
			throw new MissingParameterException("parametern index saknas eller är tom", "APIMethodFactory.getIndexList", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while (indexTokenizer.hasMoreTokens()) {
			indexList.add(indexTokenizer.nextToken());
		}
		return indexList;
	}

	/**
	 * returnerar ett prefix
	 * @param prefix
	 * @return
	 */
	public String getPrefix(String prefix) {
		if (prefix == null) {
			prefix = "*";
		} else if (!prefix.endsWith("*")) {
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
	public int getMaxValueCount(String maxValueCountString) throws BadParameterException {
		int maxValueCount;
		if (maxValueCountString == null) {
			maxValueCount = DEFAULT_MAX_VALUE_COUNT;
		} else {
			try {
				maxValueCount = Integer.parseInt(maxValueCountString);
			} catch(NumberFormatException e) {
				throw new BadParameterException("parametern maxValueCount måste vara ett numeriskt värde", "APIMethodFactory.getMaxValueCount", null, false);
			}
		}
		return maxValueCount;
	}
}
