package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.*;

public class Protocol_0_TO_1_0_Test extends AbstractDocumentTest {


	@Test
	public void testURILookup()  {
		// "null"-graf
		Model m = RDFUtil.parseModel("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"></rdf:RDF>");

		SamsokProtocolHandler handler = getSamsokProtocolHandler(m, null);
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/DataQuality#raw");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/ContextType#exist");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/EntityType#photo");

		String agent = lookup(handler, "http://kulturarvsdata.se/resurser/EntityType#agent");
		assertNull("Agent ska inte vara med i <= 1.0", agent);
	}

	@Test
	public void testParse1() throws Exception {
		LinkedList<String> relations = new LinkedList<>();
		SolrInputDocument doc = getSolrInputDocument("hjalm_0.99.rdf", relations);
		assertEquals("Fel antal relationer tillbaka", 1, relations.size());
		// kontrollera specialformatet
		assertEquals("Felaktig relation", "isRelatedTo|http://kulturarvsdata.se/raa/test/2", relations.getFirst());
		// kontrollera uppslagning
		assertEquals("Felaktigt uppslaget ämne", "Kulturhistoria", doc.getFieldValue(ContentHelper.IX_SUBJECT));
		// kontrollera exists-index
		assertEquals("Felaktigt värde för geodataExists", "n", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
		assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
		assertEquals("Felaktigt värde för timeInfoExists", "n", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		// namn + namn i kontexttypspecifikt index
		assertTrue("Skaparnamn ej extraherat", doc.getFieldValues(ContentHelper.IX_NAME).contains("Kunz Lochner"));
		assertTrue("Skaparnamn (kontext-index) ej extraherat", doc.getFieldValues("create_" + ContentHelper.IX_NAME).contains("Kunz Lochner"));
	}


	private void lookupNotNull(SamsokProtocolHandler handler, String uri) {
		String value = lookup(handler, uri);
		assertNotNull("Hittade inte värde för " + uri, value);
	}

	private String lookup(SamsokProtocolHandler handler, String uri) {
		return handler.lookupURIValue(uri);
	}



	@Test(expected = Exception.class)
	public void testNoMediaLicense() throws Exception {
		SolrInputDocument doc = getSolrInputDocument("alla_index_0.99_felaktig.rdf", new LinkedList<>());
	}

	@Override
	SamsokProtocolHandler getSamsokProtocolHandler(Model model, Resource s) {
		return new SamsokProtocolHandler_0_TO_1_0(model, s);
	}
}
