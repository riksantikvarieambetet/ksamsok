package se.raa.ksamsok.api.exception;

/**
 * Abstrakt exception klass för att kategorisera de övriga exceptions i detta
 * program
 * @author Henrik Hjalmarsson
 */
public abstract class APIException extends Exception
{
	private static final long serialVersionUID = 8460660396647539894L;
	
	private String className;
	private String details;
	private boolean logg;

	/**
	 * skapar ett APIException
	 * @param message till externt system
	 * @param className klassen och metoden där exception kastades
	 * @param details detaljer för att se vad som blev fel
	 */
	public APIException(String message, String className, String details,
			boolean logg)
	{
		super(message);
		this.className = className;
		if(details == null)
		{
			this.details = message;
		}else
		{
			this.details = details;
		}
		this.logg = logg;
	}
	
	/**
	 * returnerar true om detta exception bör loggas
	 * @return
	 */
	public boolean logg()
	{
		return logg;
	}
	
	/**
	 * returnerar klassnamn
	 * @return klassnamn
	 */
	public String getClassName()
	{
		return className;
	}
	
	/**
	 * returnerar detaljer
	 * @return detaljer
	 */
	public String getDetails()
	{
		return details;
	}
}