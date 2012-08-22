package se.raa.ksamsok.harvest;

/**
 * Klass som hanterar skörd av OAI-PMH-data i k-samsöksformat.
 */
public class SamsokOAIPMHHarvestJob extends OAIPMHHarvestJob {
	public SamsokOAIPMHHarvestJob() {}

	@Override
	protected String getMetadataFormat() {
		// TODO: ska det vara vanilj-rdf eller kulturarvsdata-rdf?
		//       Nils har använt detta i sin nod så det får vara så här tills vidare i alla fall
		return "http://kulturarvsdata.se/schema/ksamsok-rdf#";
		//return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	}
}
