package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.stream.JsonWriter;
import com.java.generationjava.io.xml.SimpleXmlWriter;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

/**
 * Basklass för api-metoder.
 *
 */
public abstract class AbstractAPIMethod implements APIMethod {

	protected static final Logger logger = Logger.getLogger(AbstractAPIMethod.class);

	protected APIServiceProvider serviceProvider;
	protected Map<String, String> params;
	protected PrintWriter writer;
	protected SimpleXmlWriter xmlWriter;
	protected JsonWriter jsonWriter;
	protected String stylesheet;
	protected boolean headWritten;
	protected boolean footWritten;
	
	protected Format format = Format.XML;
	protected Boolean prettyPrint = false;
	
	
	/**
	 * Skapar ny instans.
	 * @param serviceProvider tillhandahåller tjänster etc
	 * @param writer writer
	 * @param params parametrar
	 */
	protected AbstractAPIMethod(APIServiceProvider serviceProvider, PrintWriter writer, Map<String, String> params) {
		this.serviceProvider = serviceProvider;
		this.writer = writer;
		this.xmlWriter=new SimpleXmlWriter(writer);
		this.jsonWriter=new JsonWriter(writer);
		this.params = params;
		this.stylesheet = params.get("stylesheet");
	}

	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException {
		// läs ut parametrar och kasta ex vid problem
		extractParameters();
		if(prettyPrint){
			jsonWriter.setIndent("    ");
		}
		// utför operationen
		performMethodLogic();
		try {
			// skriv huvud
			writeHead();
			// skriv data
			writeResult();
			// skriv fot
			writeFoot();
		} catch (IOException e) {
			logger.error("writeXmlResult: "+e.getMessage());
			throw new DiagnosticException(e.getMessage(),AbstractAPIMethod.class.getName(),e.getCause().getMessage(),false);
		}
	}

	/**
	 * Skriver huvud och anropar sen {@linkplain #writeHeadExtra()}.
	 * @throws IOException 
	 * @throws DiagnosticException 
	 */
	protected void writeHead() throws IOException {
		if (format != Format.JSON_LD){
			xmlWriter.writeXmlVersion("1.0", "UTF-8");
			if(stylesheet!=null && stylesheet.trim().length()>0){
				xmlWriter.writeXmlStyleSheet(stylesheet,"text/xsl");
			}
			xmlWriter.writeEntity("result");
			xmlWriter.writeEntityWithText("version", APIMethod.API_VERSION);

		}
		writeHeadExtra();
		headWritten = true;
	}

	/**
	 * Extrasaker att skriva ut efter huvudet, överlagra i subklasser.
	 * @throws IOException 
	 */
	protected void writeHeadExtra() throws IOException {}

	/**
	 * Skriver resultat av metod.
	 * @throws DiagnosticException vid fel
	 * @throws IOException 
	 */
	protected void writeResult() throws IOException, DiagnosticException {}

	/**
	 * Anropar {@linkplain #writeFootExtra()} och skriver sen ut fot.
	 * @throws IOException 
	 */
	protected void writeFoot() throws IOException {
		writeFootExtra();
		if (format != Format.JSON_LD){
			xmlWriter.endEntity();
			xmlWriter.close();
		}
		footWritten = true;
	}

	/**
	 * Extrasaker att skriva ut före foten, överlagra i subklasser.
	 * @throws IOException 
	 */
	protected void writeFootExtra() throws IOException {}


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
	
	public void setFormat(Format format){
		this.format=format;
	}
	
	public void setPrettyPrint(Boolean prettyPrint){
		this.prettyPrint=prettyPrint;
	}
}
