package se.raa.ksamsok.api.method;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ProcessingInstruction;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Basklass för api-metoder.
 *
 */
public abstract class AbstractAPIMethod implements APIMethod {

	protected static final Logger logger = LogManager.getLogger(AbstractAPIMethod.class);

	protected APIServiceProvider serviceProvider;
	protected Map<String, String> params;
	protected OutputStream out;
	protected String stylesheet;
	protected Document doc;
	protected Format format = Format.XML;

	/**
	 * Skapar ny instans.
	 * @param serviceProvider tillhandahåller tjänster etc
	 * @param writer writer
	 * @param params parametrar
	 * @throws DiagnosticException om det inte går att initiera ett xml-dokument
	 */
	protected AbstractAPIMethod(APIServiceProvider serviceProvider, OutputStream out, Map<String, String> params) throws DiagnosticException {
		this.serviceProvider = serviceProvider;
		this.params = params;
		this.stylesheet = params.get("stylesheet");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			this.doc = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			logger.error(e);
			throw new DiagnosticException("Det är problem med att initiera xml-dokumenthanteraren", AbstractAPIMethod.class.getName(), e.getMessage(), false);
		}
		this.out=out;
	}

	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException {
		// läs ut parametrar och kasta ex vid problem
		extractParameters();
		// utför operationen
		performMethodLogic();
		generateDocument();
		writeResult();
	}


	/**
	 * Skriver resultat av metod.
	 * @throws DiagnosticException vid fel
	 * @throws TransformerConfigurationException 
	 */
	protected void writeResult() throws DiagnosticException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transform;
		try {
			transform = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult strResult;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			// Connect the stream to a byte array output stream, for further processing, if the format is json
			// otherwise connect the stream to the http response
			if (format == Format.JSON_LD){
				strResult = new StreamResult(baos);
				transform.transform(source, strResult);
				String json;
				JSONObject jsonObject = XML.toJSONObject(baos.toString("UTF-8"));
				json = jsonObject.toString();
				out.write(json.getBytes(StandardCharsets.UTF_8));
			} else {
				strResult = new StreamResult(out);
				transform.transform(source, strResult);
			}
		} catch (TransformerException e) {
			logger.error(e);
			throw new DiagnosticException("Det är problem med att initiera xml konverteraren", this.getClass().getName(), e.getMessage(), false);
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
			throw new DiagnosticException("Det är problem med att initiera json konverteraren", this.getClass().getName(), e.getMessage(), false);
		} catch (JSONException e) {
			logger.error(e);
			logger.error("Request param:" + params.toString());
			throw new DiagnosticException("Det är problem med att konvertera xml till json", this.getClass().getName(), e.getMessage(), false);
		} catch (IOException e) {
			logger.error(e);
			throw new DiagnosticException("Det är problem med att skriva resultatet till utströmmen", this.getClass().getName(), e.getMessage(), false);
		}
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
	 * Denna metod genererar xml dokumentet som är grund för api-svaret
	 * @throws DiagnosticException 
	 */
	abstract protected void generateDocument() throws DiagnosticException;
	
	protected Element generateBaseDocument(){
		//Root element
		Element result = doc.createElement("result");
		doc.appendChild(result);
		// Stylesheet
		if(stylesheet!=null && stylesheet.trim().length()>0){
			ProcessingInstruction pi = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\""+ stylesheet +"\"");
			doc.insertBefore(pi, result);
		}
		//Version
		Element version = doc.createElement("version");
		version.appendChild(doc.createTextNode(API_VERSION));
		result.appendChild(version);
		return result;
	}

	/**
	 * Returnerar query-strängen eller kastar ett exception om värdet var null. Om querysträngen
	 * innehåller "replaces=" ersätts det med "multipleReplaces" eftersom det har bytt namn i indexet
	 * @param originalQueryString query-sträng
	 * @return en array med queryString och originalQueryString
	 * @throws MissingParameterException om strängen är null eller tomma strängen
	 */
	public String[] getQueryString(String originalQueryString) throws MissingParameterException {
		if (originalQueryString == null || originalQueryString.trim().length() < 1) {
			throw new MissingParameterException("parametern query saknas eller är tom", "APIMethodFactory.getQueryString", null, false);
		}
		String queryString = originalQueryString.replaceAll("replaces=",  "multipleReplaces=");
		return new String[]{queryString, originalQueryString};
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
		Map<String,String> indexMap = new HashMap<>();
		if (indexString == null || indexString.trim().length() < 1) 	{
			throw new MissingParameterException("parametern index saknas eller är tom", "APIMethodFactory.getIndexMapSingleValue", null, false);
		}
		StringTokenizer indexTokenizer = new StringTokenizer(indexString, DELIMITER);
		while (indexTokenizer.hasMoreTokens()) {
			indexMap.put(indexTokenizer.nextToken(), value);
		}
		return indexMap;
	}

	protected String getMandatoryParameterValue(String key, String infoClassName, String infoDetails) throws MissingParameterException {
		return getParameterValue(key, true, infoClassName, infoDetails);
	}
	protected String getOptionalParameterValue(String key, String infoClassName, String infoDetails) throws MissingParameterException {
		return getParameterValue(key, false, infoClassName, infoDetails);
	}

	protected String getParameterValue(String key, boolean isMandatory, String infoClassName, String infoDetails) throws MissingParameterException {
		String value = StringUtils.trimToNull(params.get(key));
		if (isMandatory && value == null) {
			throw new MissingParameterException("Parametern " + key + " saknas eller är tom",
					infoClassName, infoDetails, false);
		}
		return value;
	}
	
	public void setFormat(Format format){
		this.format=format;
	}
}
