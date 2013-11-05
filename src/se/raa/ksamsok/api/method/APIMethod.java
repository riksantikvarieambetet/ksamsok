package se.raa.ksamsok.api.method;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.json.JSONException;

import com.sun.syndication.io.FeedException;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod.Format;

/**
 * Interface för API metoder
 * @author Henrik Hjalmarsson
 */
public interface APIMethod 
{
	/** versionen för detta API */
	public static final String API_VERSION = "1.0";
	/** namnet på metod parametern */
	public static final String METHOD = "method";
	/** API nyckel parameter namn */
	public static final String API_KEY_PARAM_NAME = "x-api";
	/** delare för att dela query strängar */
	public static final String DELIMITER = "|";
	
	/** De olika formatent*/
	public enum Format {
		RDF, HTML, MUSEUMDAT, XML, JSON_LD;
	}
	/** Pretty print indrag för json*/
	public static final int indentFactor = 4;

	
	/**
	 * utför API metod
	 * @throws MissingParameterException om obligatorisk parameter saknas
	 * @throws BadParameterException om parameter är felformaterad
	 * @throws DiagnosticException vid oväntat fel
	 * @throws TransformerConfigurationException 
	 * @throws JSONException 
	 * @throws TransformerException 
	 * @throws FeedException 
	 */
	public void performMethod()
		throws MissingParameterException, BadParameterException,
			DiagnosticException, TransformerConfigurationException, TransformerException, JSONException, FeedException;


	public void setFormat(Format format);

	public void setPrettyPrint(Boolean prettyPrint);

}