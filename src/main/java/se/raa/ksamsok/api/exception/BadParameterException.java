package se.raa.ksamsok.api.exception;

/**
 * Kastas om en parameter är felaktig eller leder till fel
 * @author Henrik Hjalmarsson
 */
public class BadParameterException extends APIException
{
	

	public BadParameterException(String message, String className,
			String details, boolean logg)
	{
		super(message, className, details, logg);
	}

	private static final long serialVersionUID = -4046819788370603325L;

}