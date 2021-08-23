package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Test;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class Protocol_1_11_Test extends AbstractProtocolTest {

/* 1.11 ändrades så att mediaLicense och mediaLicenseUrl inte är obligatoriska trots allt
    vilket gör detta testfall felaktigt, men det får ligga kvar bortkommenterat tillsammans
    med hjalm_1.11_felaktig.rdf tills vidare

	@Test
	public void testNoLicense() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.11_felaktig.rdf");
		Graph graph = RDFUtil.parseGraph(rdf);
		assertNotNull("Ingen graf, fel på rdf:en?", graph);
		GraphElementFactory elementFactory = graph.getElementFactory();
		// grund
		URIReference rdfType = elementFactory.createURIReference(SamsokProtocol.uri_rdfType);
		URIReference samsokEntity = elementFactory.createURIReference(SamsokProtocol.uri_samsokEntity);

		SubjectNode s = null;
		for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rdfType, samsokEntity)) {
			if (s != null) {
				throw new Exception("Ska bara finnas en entity i rdf-grafen");
			}
			s = triple.getSubject();
		}
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_11(graph, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<String>();
		List<String> gmlGeometries = new LinkedList<String>();
		try {
			handler.handle(service, new Date(), relations, gmlGeometries);
			fail("Ett exception borde ha kastats då mediaLicense och mediaLicenseUrl är obligatoriska i 1.11");
		} catch (Exception expected) {
			// inte helt ok att kolla exception strängar så här men...
			assertTrue("Strängen 'mediaLicense' borde vara med i det kastade felet",
					expected.getMessage().contains("mediaLicense"));
		}
	}
 */




	@Test
	public void testNoMediaLicense() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.11_felaktig.rdf");
		Model model = RDFUtil.parseModel(rdf);
		assertNotNull("Ingen graf, fel på rdf:en?", model);

		Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
		Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
		SimpleSelector selector = new SimpleSelector (null, rdfType, samsokEntity);

		Resource s = null;
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			if (s != null) {
				throw new Exception("Ska bara finnas en entity i rdf-grafen");
			}
			s = iter.next().getSubject();
		}
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_11(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		try {
			SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
			Assert.fail("Was expecting an exception due to missing mediaLicense");
		} catch (Exception e) {
			// That's ok, there should be an exception thrown here
		}
	}






	@Test
	public void testParseMedia() throws Exception {
		String rdf = loadTestFileAsString("media.rdf");
		Model model = RDFUtil.parseModel(rdf);
		assertNotNull("Ingen graf, fel på rdf:en?", model);

		Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
		Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
		SimpleSelector selector = new SimpleSelector (null, rdfType, samsokEntity);

		Resource s = null;
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			if (s != null) {
				throw new Exception("Ska bara finnas en entity i rdf-grafen");
			}
			s = iter.next().getSubject();
		}
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_11(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
		assertNotNull("Inget doc tillbaka", doc);
		assertEquals("Fel antal relationer tillbaka", 0, relations.size());
		
//		// kontrollera exists-index
		assertEquals("Felaktigt värde för geodataExists", "j", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
		assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
		assertEquals("Felaktigt värde för timeInfoExists", "n", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		assertEquals("Felaktig objektsupertyp", "Fysiskt ting", doc.getFieldValue(ContentHelper.IX_ITEMSUPERTYPE));
		Collection<Object> contextSuperTypes = doc.getFieldValues(ContentHelper.IX_CONTEXTSUPERTYPE);
		assertNotNull("Kontextsupertyper saknas", contextSuperTypes);
		assertTrue("Kontextsupertypen 'Tillverka' (create) saknas", contextSuperTypes.contains("create"));
		
		// kolla mediaindex
		assertEquals("Felaktigt värde för mediaLicense", "http://kulturarvsdata.se/resurser/License#by", doc.getFieldValue(ContentHelper.IX_MEDIALICENSE));
		assertEquals("Felaktigt värde för mediaMotiveWord", "Ett mediamotiv", doc.getFieldValue(ContentHelper.IX_MEDIAMOTIVEWORD));
		assertEquals("Felaktigt värde för byline", "Arbetets Museum", doc.getFieldValue(ContentHelper.IX_BYLINE));
		assertEquals("Felaktigt värde för copyright", "Arbetets Museum", doc.getFieldValue(ContentHelper.IX_COPYRIGHT));
		assertEquals("Felaktigt värde för thumbnailSource", "http://www.workwithsounds.eu/wp-content/uploads/2014/10/IMG_2867-204x136.jpg", doc.getFieldValue(ContentHelper.IX_THUMBNAIL_SOURCE));

	}
}
