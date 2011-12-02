package se.raa.ksamsok.lucene;

import static junit.framework.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.solr.common.SolrInputDocument;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.junit.Test;
import org.w3c.dom.Document;

import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

public class Protocol_1_1_Test {

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}


	@Test
	public void testURILookup() throws Exception {
		// "null"-graf
		Graph graph = RDFUtil.parseGraph("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"></rdf:RDF>");

		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(graph, null);
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/DataQuality#raw");
		lookupNotNull(handler, "http://kulturarvsdata.se/resurser/EntityType#photo");

		String agent = lookup(handler, "http://kulturarvsdata.se/resurser/EntityType#agent");
		assertNotNull("Agent ska vara med i 1.1", agent);
		String exist = lookup(handler, "http://kulturarvsdata.se/resurser/ContextType#exist");
		assertNull("Exist ska inte vara med i 1.1", exist);
	}

	@Test
	public void testParse1() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.1.rdf");
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
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(graph, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<String>();
		List<String> gmlGeometries = new LinkedList<String>();
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
		assertEquals("Felaktigt värde för timeInfoExists", "n", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
		// namn + namn i kontexttypspecifikt index
		assertTrue("Skaparnamn ej extraherat", doc.getFieldValues(ContentHelper.IX_NAME).contains("Kunz Lochner"));
		assertTrue("Skaparnamn (kontext-index) ej extraherat", doc.getFieldValues("create_" + ContentHelper.IX_NAME).contains("Kunz Lochner"));
	}

	@Test
	public void testParseBadContextType() throws Exception {
		String rdf = loadTestFileAsString("hjalm_1.1_felaktig.rdf");
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
		SamsokProtocolHandler handler = new SamsokProtocolHandler_1_1(graph, s);
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		LinkedList<String> relations = new LinkedList<String>();
		List<String> gmlGeometries = new LinkedList<String>();
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

	private String loadTestFileAsString(String fileName) throws Exception {
		DocumentBuilder builder = xmlFact.newDocumentBuilder();
		InputStream is = null;
		StringWriter sw = null;
		try {
			Document doc = builder.parse(new File("test/resources/" + fileName));
			final int initialSize = 4096;
			Source source = new DOMSource(doc);
			Transformer xformer = xformerFact.newTransformer();
			xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			sw = new StringWriter(initialSize);
			Result result = new StreamResult(sw);
	        xformer.transform(source, result);
		} finally {
			if (sw != null) {
				sw.close();
			}
			if (is != null) {
				is.close();
			}
		}
		return sw != null ? sw.toString() : null;
	}

}
