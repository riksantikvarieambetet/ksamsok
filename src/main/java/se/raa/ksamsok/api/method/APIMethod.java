package se.raa.ksamsok.api.method;





import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

/**
 * Interface för API metoder
 * @author Henrik Hjalmarsson
 */
public interface APIMethod 
{
	/** versionen för detta API */
    String API_VERSION = "1.0";
	/** namnet på metod parametern */
    String METHOD = "method";
	/** API nyckel parameter namn */
    String API_KEY_PARAM_NAME = "x-api";
	/** delare för att dela query strängar */
    String DELIMITER = "|";
	
	/** De olika formatent*/
    enum Format {
		RDF, HTML, MUSEUMDAT, XML, JSON_LD
    }
	/** Pretty print indrag för json*/
    int indentFactor = 4;

	
	/**
	 * utför API metod
	 * @throws MissingParameterException om obligatorisk parameter saknas
	 * @throws BadParameterException om parameter är felformaterad
	 * @throws DiagnosticException vid oväntat fel
	 */
    void performMethod()
		throws MissingParameterException, BadParameterException,
			DiagnosticException;


	void setFormat(Format format);

}