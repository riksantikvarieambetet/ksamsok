package se.raa.ksamsok.lucene;

import java.util.HashMap;

/**
 * Enum to keep track of valid uri prefixes. It also helps correcting faulty uri prefixes in data
 * from local nodes.
 * 
 * @author stefan
 * 
 */
public enum SamsokUriPrefix {

	FOAF(SamsokProtocol.uriPrefixFoaf, "http://xmlns.com/foaf"),
	MINDSWAP(SamsokProtocol.uriPrefixMindswap, "http://www.mindswap.org/2003/owl/geo"),
	KSAMSOK(SamsokProtocol.uriPrefixKSamsok, "http://kulturarvsdata.se/ksamsok"),
	CIDOC(SamsokProtocol.uriPrefix_cidoc_crm, "http://www.cidoc-crm.org/rdfs/cidoc-crm"),
	BIO(SamsokProtocol.uriPrefix_bio, "http://purl.org/vocab/bio/0.1");

	private final String correctPrefix;
	private final String shortVersion;

	SamsokUriPrefix(String correctPrefix, String shortVersion) {
		this.correctPrefix = correctPrefix;
		this.shortVersion = shortVersion;
	}

	private static HashMap<String, String> cache = new HashMap<>();

	/**
	 * Given a uri prefix, looks up the correct one. This is to correct for example uri:s from
	 * Tekniska muséet, where the foaf uri:s come with an extra "#" in the end.
	 * 
	 * @param uri a uri to check for correctness
	 * @return the correct version of the uri
	 */
	public static String lookupPrefix(String uri) {
		String retValue = cache.get(uri);
		if (retValue == null) {
			// not found in cache

			// Default, return same uri
			retValue = uri;
			for (SamsokUriPrefix p : SamsokUriPrefix.values()) {
				if (uri.startsWith(p.shortVersion)) {
					retValue = p.correctPrefix;
					break;
				}
			}
			cache.put(uri, retValue);
		}
		return retValue;
	}
}
