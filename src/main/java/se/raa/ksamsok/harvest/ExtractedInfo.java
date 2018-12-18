package se.raa.ksamsok.harvest;

/**
 * Värdeböna som innehåller information extraherad från rdf.
 */
public class ExtractedInfo {

	private String identifier;
	private String nativeURL;

	public ExtractedInfo() {
	}

	/**
	 * @return identifierare
	 */
	public String getIdentifier() {
		return identifier;
	}
	/**
	 * Sätter identifierare
	 * @param identifier identifierare
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	/**
	 * Ger url till html-representation
	 * @return html-url
	 */
	public String getNativeURL() {
		return nativeURL;
	}
	/**
	 * Sätter html-url.
	 * @param nativeURL html-url
	 */
	public void setNativeURL(String nativeURL) {
		this.nativeURL = nativeURL;
	}
}
