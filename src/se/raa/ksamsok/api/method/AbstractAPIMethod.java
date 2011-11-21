package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StartEndWriter;

/**
 * Basklass för api-metoder.
 *
 */
public abstract class AbstractAPIMethod implements APIMethod {

	protected APIServiceProvider serviceProvider;
	protected Map<String, String> params;
	protected PrintWriter writer;
	protected String stylesheet;
	protected boolean headWritten;
	protected boolean footWritten;

	/**
	 * Skapar ny instans.
	 * @param serviceProvider tillhandahåller tjänster etc
	 * @param writer writer
	 * @param params parametrar
	 */
	protected AbstractAPIMethod(APIServiceProvider serviceProvider, PrintWriter writer, Map<String, String> params) {
		this.serviceProvider = serviceProvider;
		this.writer = writer;
		this.params = params;
		this.stylesheet = params.get("stylesheet");
	}

	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException {
		// läs ut parametrar och kasta ex vid problem
		extractParameters();
		// utför operationen
		performMethodLogic();
		// skriv huvud
		writeHead();
		writer.flush();
		// skriv data
		writeResult();
		writer.flush();
		// skriv fot
		writeFoot();
	}

	/**
	 * Skriver huvud och anropar sen {@linkplain #writeHeadExtra()}.
	 */
	protected void writeHead() {
		StartEndWriter.writeStart(writer, stylesheet);
		headWritten = true;
		writeHeadExtra();
	}

	/**
	 * Extrasaker att skriva ut efter huvudet, överlagra i subklasser.
	 */
	protected void writeHeadExtra() {}

	/**
	 * Skriver resultat av metod.
	 * @throws DiagnosticException vid fel
	 */
	protected void writeResult() throws DiagnosticException {}

	/**
	 * Anropar {@linkplain #writeFootExtra()} och skriver sen ut fot.
	 */
	protected void writeFoot() {
		writeFootExtra();
		StartEndWriter.writeEnd(writer);
		footWritten = true;
	}

	/**
	 * Extrasaker att skriva ut före foten, överlagra i subklasser.
	 */
	protected void writeFootExtra() {}


	@Override
	public boolean isHeadWritten() {
		return headWritten;
	}

	@Override
	public boolean isFootWritten() {
		return footWritten;
	}

	/**
	 * Tar ut och kontrollerar parametrar.
	 * @throws MissingParameterException om parameter saknas
	 * @throws BadParameterException om parameter är felaktig
	 */
	abstract protected void extractParameters() throws MissingParameterException, BadParameterException;

	/**
	 * Utför metodens logik.
	 * @throws DiagnosticException vid problem
	 */
	abstract protected void performMethodLogic() throws DiagnosticException;

	/**
	 * Returnerar query-strängen eller kastar ett exception om värdet var null
	 * @param queryString query-sträng
	 * @return queryString
	 * @throws MissingParameterException om strängen är null eller tomma strängen
	 */
	public String getQueryString(String queryString) throws MissingParameterException {
		if (queryString == null || queryString.trim().length() < 1) {
			throw new MissingParameterException("parametern query saknas eller är tom", "APIMethodFactory.getQueryString", null, false);
		}
		return queryString;
	}

	/**
	 * Returnerar en index-map där indexen får samma värde, det som är inskickat i value.
	 *
	 * @param indexString sträng med indexnamn separerade av {@linkplain #DELIMITER}
	 * @param value värde för index
	 * @return index-map med indexnamn som nyckel och inskickat värde som värde, aldrig null men kan vara tom
	 * @throws MissingParameterException om index-strängen är null eller "tom".
	 */
	public Map<String,String> getIndexMapSingleValue(String indexString,
			String value)  throws MissingParameterException {
		Map<String,String> indexMap = new HashMap<String,String>();
		if (indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern index saknas eller är tom", "APIMethodFactory.getIndexMapSingleValue", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while (indexTokenizer.hasMoreTokens()) {
			indexMap.put(indexTokenizer.nextToken(), value);
		}
		return indexMap;
	}

	protected String getMandatoryParameterValue(String key, String infoClassName, String infoDetails,
			boolean logIfMissing) throws MissingParameterException {
		return getParameterValue(key, true, infoClassName, infoDetails, logIfMissing);
	}
	protected String getOptionalParameterValue(String key, String infoClassName, String infoDetails,
			boolean logIfMissing) throws MissingParameterException {
		return getParameterValue(key, false, infoClassName, infoDetails, logIfMissing);
	}

	protected String getParameterValue(String key, boolean isMandatory, String infoClassName, String infoDetails,
			boolean logIfMissing) throws MissingParameterException {
		String value = StringUtils.trimToNull(params.get(key));
		if (isMandatory && value == null) {
			throw new MissingParameterException("Parametern " + key + " saknas eller är tom",
					infoClassName, infoDetails, logIfMissing);
		}
		return value;
	}
}
