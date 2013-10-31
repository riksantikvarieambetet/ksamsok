package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.solr.client.solrj.SolrServerException;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.api.util.parser.CQL2Solr;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Utför en prefix sökning för att ge förslag på fortsättningar av ett givet query
 * TODO: denna klarar bara ett index trots att dok på kulturarvsdata.se säger att den ska klara
 *       fler - det enda som händer om man anger fler är att man får resultatet av det sista indexet...
 * @author Henrik Hjalmarsson
 */
public class SearchHelp extends AbstractAPIMethod {
	//private static final Logger logger = Logger.getLogger(SearchHelp.class);
	
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
	 * @param writer
	 * @param indexList
	 * @param prefix
	 * @param maxValueCount
	 */
	public SearchHelp(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
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
			for (int i = 0; i < indexList.size(); i++) {
				String index = indexList.get(i);
				index = CQL2Solr.translateIndexName(index);
				if (ContentHelper.isToLowerCaseIndex(index) || ContentHelper.isAnalyzedIndex(index)) {
					prefix = prefix != null ? prefix.toLowerCase() : prefix;
				}
				termList = serviceProvider.getSearchService().terms(index, prefix, 0, maxValueCount);
			}
		} catch (SolrServerException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod", "SearchHelp.performMethod", e.getMessage(), true);
		}
	}
	
	/**
	 * skriver ut början av svaret
	 * @throws IOException 
	 */
	@Override
	protected void writeHeadExtra() throws IOException {
		xmlWriter.writeEntityWithText("numberOfTerms", termList.size());
	}
	
	/**
	 * skriver ut resultatet av svaret
	 * @throws IOException 
	 */
	@Override
	protected void writeResult() throws IOException {
		xmlWriter.writeEntity("terms");
		for (Term t: termList) {
			xmlWriter.writeEntity("term");
			xmlWriter.writeEntityWithText("value", t.getValue());
			xmlWriter.writeEntityWithText("count", t.getCount());
			xmlWriter.endEntity();
		}
		xmlWriter.endEntity();
	}
	
	/**
	 * skriver ut foten av svaret
	 * @throws IOException 
	 */
	@Override
	protected void writeFootExtra() throws IOException {
		xmlWriter.writeEntity("echo");
		xmlWriter.writeEntityWithText("method", METHOD_NAME);
		for(int i = 0; i < indexList.size(); i++) {
			xmlWriter.writeEntityWithText("index", indexList.get(i));
		}
		xmlWriter.writeEntityWithText("maxValueCount", maxValueCount);
		xmlWriter.writeEntityWithText("prefix", prefix);
		xmlWriter.endEntity();
	}

	/**
	 * returnerar en lista med index
	 * @param indexString
	 * @return
	 * @throws MissingParameterException
	 */
	public List<String> getIndexList(String indexString)  throws MissingParameterException {
		List<String> indexList = new ArrayList<String>();
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
		int maxValueCount = 0;
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
