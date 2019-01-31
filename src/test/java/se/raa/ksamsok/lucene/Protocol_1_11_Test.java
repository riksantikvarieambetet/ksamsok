package se.raa.ksamsok.lucene;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
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
import org.junit.Test;
import org.w3c.dom.Document;

import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.StmtIterator;

@SuppressWarnings("unused")
public class Protocol_1_11_Test {

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	@Test
	public void dummy(){
		//This is a dummy test to be able to run junit in ant-task
	}

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
	public void testParseMedia() throws Exception {
		String rdf = loadTestFileAsString("media.rdf");
		Model model = RDFUtil.parseModel(rdf);
		assertNotNull("Ingen graf, fel på rdf:en?", model);

		Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
		Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
		SimpleSelector selector = new SimpleSelector ((Resource) null, rdfType, samsokEntity); 
				
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
		LinkedList<String> relations = new LinkedList<String>();
		List<String> gmlGeometries = new LinkedList<String>();
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
		
	}


	
	private String loadTestFileAsString(String fileName) throws Exception {
		DocumentBuilder builder = xmlFact.newDocumentBuilder();
		InputStream is = null;
		StringWriter sw = null;
		try {
			Document doc = builder.parse(new File("src/test/resources/" + fileName));
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
