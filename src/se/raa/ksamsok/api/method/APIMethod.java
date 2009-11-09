package se.raa.ksamsok.api.method;

import se.raa.ksamsok.api.BadParameterException;
import se.raa.ksamsok.api.DiagnosticException;
import se.raa.ksamsok.api.MissingParameterException;

/**
 * Interface f�r API metoder
 * @author Henrik Hjalmarsson
 */
public interface APIMethod 
{
	/** versionen f�r detta API */
	public static final String API_VERSION = "1.0";
	/** namnet p� metod parametern */
	public static final String METHOD = "method";
	
	/**
	 * utf�r API metod
	 * @throws MissingParameterException om obligatorisk parameter saknas
	 * @throws BadParameterException om parameter �r felformaterad
	 * @throws DiagnosticException vid ov�ntat fel
	 */
	public void performMethod()
		throws MissingParameterException, BadParameterException,
			DiagnosticException;
}