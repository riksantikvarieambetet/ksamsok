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
	/** delare för att dela query strängar */
    String DELIMITER = "|";
	
	/** De olika formatent*/
    enum Format {
		RDF, HTML, MUSEUMDAT, XML, JSON_LD
    }

	
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