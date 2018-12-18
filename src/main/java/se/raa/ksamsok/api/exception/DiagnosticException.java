package se.raa.ksamsok.api.exception;

/**
 * kastas vid övriga fel
 * @author Henrik Hjalmarsson
 */
public class DiagnosticException extends APIException 
{
	public DiagnosticException(String message, String className,
			String details, boolean logg)
	{
		super(message, className, details, logg);
	}

	private static final long serialVersionUID = 2645339640719635429L;

	
}
