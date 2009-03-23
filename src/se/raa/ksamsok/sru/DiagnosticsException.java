package se.raa.ksamsok.sru;

/**
 * Enkel klass om enkapsulerar en diagnostic.
 * Se http://www.loc.gov/standards/sru/resources/diagnostics-list.html
 */
public class DiagnosticsException extends Exception {

	private static final long serialVersionUID = 1L;

	private final int errorKey;
	private final String details;

	/**
	 * Skapar ny instans med nyckel och meddelande.
	 * 
	 * @param errorKey nyckel
	 * @param message meddelande
	 */
	DiagnosticsException(int errorKey, String message) {
		this(errorKey, message, null);
	}

	/**
	 * Skapar ny instans med nyckel, meddelande och detaljinformation.
	 * 
	 * @param errorKey nyckel
	 * @param message meddelande
	 * @param details detaljinformation
	 */
	DiagnosticsException(int errorKey, String message, String details) {
		super(message);
		this.errorKey = errorKey;
		this.details = details;
	}

	/**
	 * Ger nyckeln.
	 * @return nyckel
	 */
	public int getErrorKey() {
		return errorKey;
	}

	/**
	 * Ger ev detaljinformation.
	 * @return detaljinformation
	 */
	public String getDetails() {
		return details;
	}
}
