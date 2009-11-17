package se.raa.ksamsok.api.exception;

/**
 * kastas om obligatoriska parametrar saknas
 * @author Henrik Hjalmarsson
 */
public class MissingParameterException extends APIException
{
	public MissingParameterException(String message, String className,
			String details, boolean logg)
	{
		super(message, className, details, logg);
	}

	private static final long serialVersionUID = -8768120716193554602L;
}