package se.raa.ksamsok.api.util;

/**
 * Värdeböna som innehåller term-information.
 */
public class Term {

	final String index;
	final String value;
	final long count;

	/**
	 * Skapa en ny instans för index, term och antal
	 * @param index
	 * @param value
	 * @param count
	 */
	public Term(String index, String value, long count) {
		this.index = index;
		this.value = value;
		this.count = count;
	}

	/**
	 * Ger indexnamn
	 * @return index
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * Ger termen/termtexten
	 * @return text
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Ger antal förekomster
	 * @return antal
	 */
	public long getCount() {
		return count;
	}

}
