package se.raa.ksamsok.api.util;

/**
 * Enkel bönklass för att hålla info om relationer.
 * Obs att informationskälla (source) ej är källan i relationen utan var relationen kom ifrån,
 * tex null om den hämtades direkt från källobjektet eller "deduced" om den härleddes via
 * relationsinvers (mha originalRelationType). "Källan" i relationen förutsätts vara känd av
 * användaren av denna böna.
 */
public class Relation {

	private String relationType;
	private String targetUri;
	private String source;
	private String originalRelationType;

	/**
	 * Skapa ny instans.
	 * @param relationType typ av relation
	 * @param targetUri uri som relationen går till
	 * @param source relationens informationskälla eller null
	 * @param originalRelationType
	 */
	public Relation(String relationType, String targetUri, String source, String originalRelationType) {
		this.relationType = relationType;
		this.targetUri = targetUri;
		this.source = source;
		this.originalRelationType = originalRelationType;
	}

	/**
	 * Ger relationstypens namn.
	 * @return relationstyp
	 */
	public String getRelationType() {
		return relationType;
	}

	/**
	 * Ger uri för objekt på andra sidan av relationen.
	 * @return objekt-uri
	 */
	public String getTargetUri() {
		return targetUri;
	}

	/**
	 * Ger informationskälla.
	 * @return informationskälla
	 */
	public String getSource() {
		return source;
	}

	/**
	 * Ger ev orginalrelationstyp tex om relationen har härletts eller hämtats från manuellt inmatad info.
	 * @return orginalrelationstyp
	 */
	public String getOriginalRelationType() {
		return originalRelationType;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		// genererad av eclipse från uri och relation
		// OBS bara för uri och relation, övriga är ointressanta vid denna jämförelse
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((relationType == null) ? 0 : relationType.hashCode());
		result = prime * result
				+ ((targetUri == null) ? 0 : targetUri.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		// genererad av eclipse från uri och relation
		// OBS bara för uri och relation, övriga är ointressanta vid denna jämförelse
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Relation)) {
			return false;
		}
		Relation other = (Relation) obj;
		if (relationType == null) {
			if (other.relationType != null) {
				return false;
			}
		} else if (!relationType.equals(other.relationType)) {
			return false;
		}
		if (targetUri == null) {
			return other.targetUri == null;
		} else return targetUri.equals(other.targetUri);
	}

	@Override
	public String toString() {
		return "relation of type " + relationType + " to " + targetUri +
			" (source " + source + ", org type " + originalRelationType + ")";
	}
}
