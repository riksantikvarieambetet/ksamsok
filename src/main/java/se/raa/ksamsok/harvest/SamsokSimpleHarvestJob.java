package se.raa.ksamsok.harvest;

import java.util.ArrayList;
import java.util.List;

/**
 * Klass som hanterar skörd av data ifrån en fil i k-samsöksformat.
 */
public class SamsokSimpleHarvestJob extends SimpleHarvestJob {
	public SamsokSimpleHarvestJob() {}

	@Override
	protected String getMetadataFormat() {
		return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	}
	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service) {
		final List<ServiceFormat> list = new ArrayList<>(1);
		list.add(new ServiceFormat("rdf",
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
				"http://www.w3.org/2000/07/rdf.xsd")); // TODO: rätt schemaplats
		return list;
	}


}
