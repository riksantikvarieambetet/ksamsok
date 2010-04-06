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
	public static final String API_VERSION = "1.0";
	/** namnet på metod parametern */
	public static final String METHOD = "method";
	/** API nyckel parameter namn */
	public static final String API_KEY_PARAM_NAME = "x-apikey";
	
	/**
	 * utför API metod
	 * @throws MissingParameterException om obligatorisk parameter saknas
	 * @throws BadParameterException om parameter är felformaterad
	 * @throws DiagnosticException vid oväntat fel
	 */
	public void performMethod()
		throws MissingParameterException, BadParameterException,
			DiagnosticException;
}