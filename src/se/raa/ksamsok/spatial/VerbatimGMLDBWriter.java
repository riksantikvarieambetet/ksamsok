package se.raa.ksamsok.spatial;

/**
 * Enkel klass som inte gör nån konvertering av gml:en utan returnerar den.
 * Används främst för debug och databaskolumnen geometry måste vara anpassad till
 * detta.
 */
public class VerbatimGMLDBWriter extends AbstractGMLDBWriter {

	public VerbatimGMLDBWriter() {}

	@Override
	protected Object convertToNative(String gml) throws Exception {
		return gml;
	}

}
