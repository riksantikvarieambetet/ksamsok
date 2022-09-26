package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.*;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class Protocol_1_1_Test extends AbstractDocumentTest {

	@Test
	public void testCreateDoc_1_1() throws Exception {
		// specialindexet för relationer
		List<String> relations = new LinkedList<>();
		SolrInputDocument doc = getSolrInputDocument("hjalm_1.1.rdf", relations);

		assertNotNull("Specialindexet för relationer saknas", relations);
		assertEquals("Specialindexet för relationer har fel antal", 2, relations.size());
		assertTrue("Specialindexvärde finns inte med", relations.contains("isRelatedTo|http://kulturarvsdata.se/raa/test/2"));
		assertTrue("Specialindexvärde finns inte med", relations.contains("has_former_or_current_owner|http://libris.kb.se/resource/auth/58087"));

		assertNotNull("Ingen beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION));
		assertTrue("Fel beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION).toString().contains("som beskriver Gustav"));
	}


	@Test
	public void testURILookup() {
		// "null"-graf
		Model model = RDFUtil.parseModel("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"></rdf:RDF>");

		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(model, null);
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/DataQuality#raw");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/EntityType#photo");

		String agent = lookup(handler, "http://kulturarvsdata.se/resurser/EntitySuperType#agent");
		assertNotNull("Agent ska vara med som supertyp i 1.1", agent);
		String person = lookup(handler, "http://kulturarvsdata.se/resurser/EntityType#person");
		assertNotNull("Person ska vara med som typ i 1.1", person);
		String exist = lookup(handler, "http://kulturarvsdata.se/resurser/ContextType#exist");
		assertNull("Exist ska inte vara med som kontexttyp i 1.1", exist);
		String create = lookup(handler, "http://kulturarvsdata.se/resurser/ContextType#create");
		assertNull("Create ska inte vara med som kontexttyp i 1.1", create);
		create = lookup(handler, "http://kulturarvsdata.se/resurser/ContextSuperType#create");
		assertNotNull("Create ska vara med som kontextsupertyp i 1.1", create);
	}

	@Test
	public void testParse1() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.1.rdf");
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
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
		assertNotNull("Inget doc tillbaka", doc);
		assertEquals("Fel antal relationer tillbaka", 2, relations.size());
		// kontrollera specialformatet
		assertTrue("Specialrelation saknas", relations.contains("isRelatedTo|http://kulturarvsdata.se/raa/test/2"));
		assertTrue("Specialrelation saknas", relations.contains("has_former_or_current_owner|http://libris.kb.se/resource/auth/58087"));
		// kontrollera uppslagning
		assertEquals("Felaktigt uppslaget ämne", "Kulturhistoria", doc.getFieldValue(ContentHelper.IX_SUBJECT));
		// kontrollera exists-index
		assertEquals("Felaktigt värde för geodataExists", "n", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
		assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
		assertEquals("Felaktigt värde för timeInfoExists", "j", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		assertEquals("Felaktig objektsupertyp", "Fysiskt ting", doc.getFieldValue(ContentHelper.IX_ITEMSUPERTYPE));
		Collection<Object> contextSuperTypes = doc.getFieldValues(ContentHelper.IX_CONTEXTSUPERTYPE);
		assertNotNull("Kontextsupertyper saknas", contextSuperTypes);
		assertTrue("Kontextsupertypen 'Tillverka' (create) saknas", contextSuperTypes.contains("create"));
		// namn + namn i kontexttypspecifikt index
		assertTrue("Skaparnamn ej extraherat", doc.getFieldValues(ContentHelper.IX_NAME).contains("Kunz Lochner"));
		Collection<Object> contextSuperTypeIndexValues = doc.getFieldValues("create_" + ContentHelper.IX_NAME);
		assertNotNull("Indexet create_name saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexValues);
		Collection<Object> contextTypeIndexValues = doc.getFieldValues("produce_" + ContentHelper.IX_NAME);
		assertNotNull("Indexet produce_name saknar värden (kontexttyp_indexnamn)", contextTypeIndexValues);
		assertTrue("Skaparnamn (kontext-index) ej extraherat ur produce_name", contextTypeIndexValues.contains("Kunz Lochner"));
		// century/decade inkl kontext
		Collection<Object> decadeValues = doc.getFieldValues(ContentHelper.IX_DECADE);
		assertNotNull("Indexet decade saknar värden", decadeValues);
		Collection<Object> contextSuperTypeIndexDecadeValues = doc.getFieldValues("create_" + ContentHelper.IX_DECADE);
		assertNotNull("Indexet create_decade saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexDecadeValues);
		Collection<Object> contextSuperTypeIndexCenturyValues = doc.getFieldValues("create_" + ContentHelper.IX_CENTURY);
		assertNotNull("Indexet create_century saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexCenturyValues);
	}

		
	
	
	@Test
	public void testParse_Agent() throws Exception {
		String rdf = loadTestFileAsString("kung_1.1.rdf");
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

		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
		assertNotNull("Inget doc tillbaka", doc);
		multipleValueIndexAssert(doc, ContentHelper.IX_NAME, new String[] {
				"Gustav Vasa", "Gustaf Vasa", "Gustav I", "Gustaf Eriksson Vasa",
				"Erik Johansson" // från kontextet
		}, 5);
		singleValueIndexAssert(doc, ContentHelper.IX_FIRSTNAME, "Gustav");
		singleValueIndexAssert(doc, ContentHelper.IX_SURNAME, "Vasa");
		singleValueIndexAssert(doc, ContentHelper.IX_GENDER, "male");
		singleValueIndexAssert(doc, ContentHelper.IX_TITLE, "Kung");
		singleValueIndexAssert(doc, ContentHelper.IX_ORGANIZATION, "Kungahuset");

		// kontrollera specialformatet för relationer
		String[] expectedRelations = {
				ContentHelper.IX_SAMEAS + "|" + "http://libris.kb.se/resource/auth/58087",
				ContentHelper.IX_SAMEAS + "|" + "http://viaf.org/viaf/59878606",
				ContentHelper.IX_CHILD + "|" + "http://kulturarvsdata.se/raa/test/2",
				ContentHelper.IX_PARTICIPATEDIN + "|" + "http://kulturarvsdata.se/raa/test/3",
				ContentHelper.IX_WASPRESENTAT + "|" + "http://kulturarvsdata.se/raa/test/4",
				ContentHelper.IX_ISCURRENTORFORMERMEMBEROF + "|" + "http://kulturarvsdata.se/raa/test/5"
		};

		assertEquals("Fel antal relationer tillbaka", expectedRelations.length, relations.size());
		for (String relation: expectedRelations) {
			assertTrue("Specialrelationen " + relation + " saknas", relations.contains(relation));
		}
		// kontrollera uppslagning
		assertEquals("Felaktigt uppslaget ämne", "Kulturhistoria", doc.getFieldValue(ContentHelper.IX_SUBJECT));
		// kontrollera exists-index
		assertEquals("Felaktigt värde för geodataExists", "n", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
		assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
		assertEquals("Felaktigt värde för timeInfoExists", "j", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		assertEquals("Felaktig objektsupertyp", "Agent", doc.getFieldValue(ContentHelper.IX_ITEMSUPERTYPE));
		Collection<Object> contextSuperTypes = doc.getFieldValues(ContentHelper.IX_CONTEXTSUPERTYPE);
		assertNotNull("Kontextsupertyper saknas", contextSuperTypes);
		assertTrue("Kontextsupertypen 'Tillverka' (create) saknas", contextSuperTypes.contains("create"));
		// namn + namn i kontexttypspecifikt index
		singleValueIndexAssert(doc, "create_" + ContentHelper.IX_NAME, "Erik Johansson");
		singleValueIndexAssert(doc, "start_" + ContentHelper.IX_NAME, "Erik Johansson");

		Collection<Object> contextSuperTypeIndexValues = doc.getFieldValues("create_" + ContentHelper.IX_NAME);
		assertNotNull("Indexet create_name saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexValues);
		multipleValueIndexAssert(doc, ContentHelper.IX_FROMTIME, new String[] {
				"1496", "1531"
		}, 2);
		// century/decade inkl kontext
		Collection<Object> decadeValues = doc.getFieldValues(ContentHelper.IX_DECADE);
		assertNotNull("Indexet decade saknar värden", decadeValues);
		Collection<Object> contextSuperTypeIndexDecadeValues = doc.getFieldValues("create_" + ContentHelper.IX_DECADE);
		assertNotNull("Indexet create_decade saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexDecadeValues);
		Collection<Object> contextSuperTypeIndexCenturyValues = doc.getFieldValues("create_" + ContentHelper.IX_CENTURY);
		assertNotNull("Indexet create_decade saknar värden (kontextsupertyp_indexnamn)", contextSuperTypeIndexCenturyValues);
	}

	@Test
	public void testParse_Event() throws Exception {
		String rdf = loadTestFileAsString("lutzen_1.1.rdf");
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
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
		assertNotNull("Inget doc tillbaka", doc);
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMTYPE, "Historisk h\u00e4ndelse");
		singleValueIndexAssert(doc, ContentHelper.IX_HADPARTICIPANT, "http://viaf.org/viaf/10637323");
		singleValueIndexAssert(doc, ContentHelper.IX_OCCUREDINTHEPRESENCEOF, "http://viaf.org/viaf/10637323");
		multipleValueIndexAssert(doc, ContentHelper.I_IX_RELATIONS, new String[] {
				ContentHelper.IX_HADPARTICIPANT + "|" + "http://viaf.org/viaf/10637323",
				ContentHelper.IX_OCCUREDINTHEPRESENCEOF + "|" + "http://viaf.org/viaf/10637323"
		}, relations, 2);
	}

	@Test
	public void testParseBadContextType() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.1_felaktig.rdf");
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

		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(model, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<>();
		List<String> gmlGeometries = new LinkedList<>();
		try {
			handler.handle(service, new Date(), relations, gmlGeometries);
			fail("Ett exception borde ha kastats då transact inte är giltig kontexttyp i 1.1");
		} catch (Exception expected) {
			// inte helt ok att kolla exception strängar så här men...
			assertTrue("Den felaktiga kontext-uri:n borde vara med i det kastade felet",
					expected.getMessage().contains(SamsokProtocol.context_pre));
		}
	}

	private void lookupNotNull(SamsokProtocolHandler handler, String uri) {
		String value = lookup(handler, uri);
		assertNotNull("Hittade inte värde för " + uri, value);
	}

	private String lookup(SamsokProtocolHandler handler, String uri) {
		return handler.lookupURIValue(uri);
	}

	private void singleValueIndexAssert(SolrInputDocument doc, String indexName, String value) {
		singleValueIndexAssert(doc, indexName, value, false);
	}

	private void singleValueIndexAssert(SolrInputDocument doc, String indexName, String value, boolean contains) {
		String docValue = (String) doc.getFieldValue(indexName);
		assertNotNull("Fältet " + indexName + " saknas", docValue);
		Collection<Object> docValues = doc.getFieldValues(indexName);
		assertEquals("Fältet " + indexName + " ska bara ha ett värde, värden är, " +
				docValues, 1, docValues.size());
		if (contains) {
			assertTrue("Fel värde för " + indexName, docValue.contains(value));
		} else {
			assertEquals("Fel värde för " + indexName, value, docValue);
		}
	}

	private void multipleValueIndexAssert(SolrInputDocument doc, String indexName,
			String[] values, int count) {
		Collection<Object> docValues = doc.getFieldValues(indexName);
		assertNotNull("Fältet " + indexName + " saknas", docValues);
		multipleValueIndexAssert(doc, indexName, values, docValues, count);
	}

	private void multipleValueIndexAssert(SolrInputDocument doc, String indexName,
										  String[] values, Collection<?> docValues, int count) {
		if (count > 0) {
			assertEquals("Fältet " + indexName + " innehåller fel antal värden, värden är" +
					docValues, count, docValues.size());
		}
		for (String value: values) {
			assertTrue("Värdet " + value + " saknas för " + indexName +
					", värden är " + docValues, docValues.contains(value));
		}
	}

	@Override
	SamsokProtocolHandler getSamsokProtocolHandler(Model model, Resource s) {
		return new SamsokProtocolHandler_1_1(model, s);
	}
}
