package se.raa.ksamsok.harvest;

/**
 * Enkel värdeklass för att hålla reda på metadataprefix, namespace och schema
 * för OAI-PMH-noder.
 */
public class ServiceFormat {

	final String prefix;
	final String schema;
	final String namespace;

	ServiceFormat(String prefix, String namespace, String schema) {
		this.prefix = prefix;
		this.namespace = namespace;
		this.schema = schema;
	}

	/**
	 * Hämtar prefix.
	 * 
	 * @return prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Hämtar schema-uri.
	 * 
	 * @return schema-uri
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Hämtar namespace-uri.
	 * 
	 * @return namespace-uri
	 */
	public String getNamespace() {
		return namespace;
	}

}
