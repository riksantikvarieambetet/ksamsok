package se.raa.ksamsok.lucene;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

@SuppressWarnings("unused")
public class Protocol_1_11_Test {

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
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
