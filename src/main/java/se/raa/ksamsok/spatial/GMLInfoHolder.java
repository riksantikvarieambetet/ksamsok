package se.raa.ksamsok.spatial;

import java.util.Collection;

/**
 * Enkel datahållare för spatial-data i form av gml. En typisk post har
 * en identifierar-URI, en namn och noll eller flera geometrier.
 */
public class GMLInfoHolder {

	private String identifier;
	private String name;
	private Collection<String> gmlGeometries;

	/**
	 * Skapar ny tom instans.
	 */
	public GMLInfoHolder() {}

	/**
	 * Ger sant om instansen innehåller geometrier
	 * @return sant mängd med geometrier finns 
	 */
	public boolean hasGeometries() {
		return gmlGeometries != null && gmlGeometries.size() > 0;
	}

	/**
	 * Ger satt identifier.
	 * @return identifierar-URI, eller null
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Sätter identifier.
	 * @param identifier identifierar-URI
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	/**
	 * Ger satt namn för denna identifier.
	 * @return namn, eller null
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sätter namn för denna identifier.
	 * @param name namn
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Ger mängd med geometrier i form av gml-strängar.
	 * @return mängd med geometrier, eller null
	 */
	public Collection<String> getGmlGeometries() {
		return gmlGeometries;
	}

	/**
	 * Sätter mängd med geometrier.
	 * @param gmlGeometries mängd med geometrier i form av gml-strängar
	 */
	public void setGmlGeometries(Collection<String> gmlGeometries) {
		this.gmlGeometries = gmlGeometries;
	}

}
