package se.raa.ksamsok.api.method;

import org.apache.solr.common.SolrDocumentList;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

import java.io.OutputStream;
import java.util.Map;

public abstract class AbstractSearchMethod extends AbstractAPIMethod {

	/** default startplats i sökning */
	public static final int DEFAULT_START_RECORD = 1;
	/** parameternamn där sökparametrarna skall ligga när en sökning görs */
	public static final String SEARCH_PARAMS = "query";
	/** parameternamnet som anges för att välja antalet träffar per sida */
	public static final String HITS_PER_PAGE = "hitsPerPage";
	/** parameternamnet som anges för att välja startRecord */
	public static final String START_RECORD = "startRecord";
	/** max antal träffar */
	public static final int MAX_HITS_PER_PAGE = 1000;

	protected String queryString;
	protected String originalQueryString;
	protected int hitsPerPage;
	protected int startRecord;

	protected SolrDocumentList hitList;

	protected AbstractSearchMethod(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException{
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		final String[] queryStrings = getQueryString(params.get(SEARCH_PARAMS));
		this.queryString = queryStrings[0];
		this.originalQueryString = queryStrings[1];
		//sätter valfria parametrar
		int hitsPerPage = getHitsPerPage(params.get(HITS_PER_PAGE));
		int startRecord = getStartRecord(params.get(START_RECORD));
		//kontrollerar att hitsPerPage och startRecord har tillåtna värden
		if (hitsPerPage < 1 || hitsPerPage > getMaxHitsPerPage()) {
			this.hitsPerPage = getDefaultHitsPerPage();
		} else {
			this.hitsPerPage = hitsPerPage;
		}
		if (startRecord < 1) {
			this.startRecord = DEFAULT_START_RECORD;
		} else {
			this.startRecord = startRecord;
		}
	}

	/**
	 * Ger default antal träffar per sida
	 * @return default antal träffar
	 */
	abstract protected int getDefaultHitsPerPage();

	/**
	 * Ger max antal träffar per sida
	 * @return max antal träffar
	 */
	protected int getMaxHitsPerPage() {
		return MAX_HITS_PER_PAGE;
	}

	/**
	 * returnerar en integer för värdet startRecord
	 * @param param
	 * @return
	 * @throws BadParameterException
	 */
	public int getStartRecord(String param) throws BadParameterException {
		int startRecord = 0;
		if (param != null) {
			try {
				startRecord = Integer.parseInt(param);
			} catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + START_RECORD + " måste innehålla ett numeriskt värde", "APIMethodFactory.getSearchObject", "icke numeriskt värde", false);
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
	public int getHitsPerPage(String param) throws BadParameterException {
		int hitsPerPage = 0;
		if (param != null) {
			try {
				hitsPerPage = Integer.parseInt(param);
			} catch(NumberFormatException e) {
				throw new BadParameterException("parametern " + Search.HITS_PER_PAGE + " måste innehålla ett numeriskt värde", "APIMethodFactory.getSearchObject", "icke numeriskt värde", false);
			}
		}
		return hitsPerPage;
	}

}
