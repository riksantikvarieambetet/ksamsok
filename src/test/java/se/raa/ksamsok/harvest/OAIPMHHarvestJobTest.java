package se.raa.ksamsok.harvest;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.raa.ksamsok.lucene.SamsokContentHelper;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OAIPMHHarvestJobTest {

	private static final int PORT = 7654;
	private static final String HOST_URL_BASE = "http://localhost:" + PORT + "/";

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	private static Server server = null;

	@BeforeClass
	public static void setup() throws Exception {
		// starta en lokal jetty och servera statiska filer från src/test/resources
		server = new Server(PORT);
		ResourceHandler handler = new ResourceHandler();
		handler.setResourceBase("src/test/resources");
		//handler.setDirectoriesListed(true); // bra för debug, ger dirlistning
		HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { handler, new DefaultHandler() });
        server.setHandler(handlers);
		server.start();

	}

	@AfterClass
	public static void teardown() throws Exception {
		if (server != null) {
			server.stop();
			server.destroy();
		}
	}

	// kan "avkommenteras" och användas för funktionstester av tjänster

//	public void testPerformGetRecords() throws Exception {
//		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
//		
//		HarvestService service = new HarvestServiceImpl();
//		String deletedRecord = "";
//		String granularity = "";
//		ServiceMetadata sm = new ServiceMetadata(deletedRecord, granularity);
//		String prefix = "";
//		String namespace = "";
//		String schema = "";
//		ServiceFormat f = new ServiceFormat(prefix, namespace, schema);
//		File storeTo = new File("file");
//		StatusService ss = new StatusServiceImpl(null);
//		oaipmhHarvesterJob.performGetRecords(service, sm, f, storeTo, ss);
//		fail("Not yet implemented");
//	}
//	
//	public void testGetRecords_harvestChanges() throws Exception {
//		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
//		
//		String url = "http://130.242.42.136/OAICat/tekm/media";
//		String fromDate = "2011-08-01";
//		String toDate = null;
//		String metadataPrefix = "ksamsok-rdf";
//		String setSpec = "";
//		File storeTo = new File("storeTo.txt");
//		OutputStream os = new BufferedOutputStream(new FileOutputStream(storeTo));
//		Logger logger = null;
//		StatusService ss = null;
//		HarvestService service = null;
//		int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, ss, service);
//		
//		System.out.println(records);
//		
//		
//	}
//	
//	public void testGetRecords_harvestEverything() throws Exception {
//		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
//		
//		String url = "http://130.242.42.136/OAICat/tekm/media";
//		//String fromDate = "2011-08-01";
//		String fromDate = null;
//		String toDate = null;
//		String metadataPrefix = "ksamsok-rdf";
//		String setSpec = "";
//		File storeTo = new File("storeTo.txt");
//		OutputStream os = new BufferedOutputStream(new FileOutputStream(storeTo));
//		Logger logger = null;
//		StatusService ss = null;
//		HarvestService service = null;
//		int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, ss, service);
//		
//		System.out.println(records);
//	}

	@Test
	public void testGetRecords_harvestFile_0_TO_0_99() throws Exception {
		// TODO: detta testfall kanske inte säger så mycket då ingen behandling av det som hämtas görs
		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob(1, 1);
		ByteArrayOutputStream os = null;
		try {
			String url = HOST_URL_BASE + "hjalmar_0.99.xml";
			//String fromDate = "2011-08-01";
			String fromDate = null;
			String toDate = null;
			String metadataPrefix = "ksamsok-rdf";
			String setSpec = "";
			os = new ByteArrayOutputStream(); 
			Logger logger = null;
			StatusService ss = null;
			HarvestService service = null;
			int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate,
					metadataPrefix, setSpec, os, logger, ss, service);
			assertEquals("Fel antal poster", 2, records);
			DocumentBuilder builder = xmlFact.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(os.toByteArray()));
			NodeList graphs = doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#","RDF");
			assertNotNull("Inga rdf-grafer", graphs);
			assertEquals("Fel antal rdf-grafer", records, graphs.getLength());
			SamsokContentHelper samsokContentHelper = new SamsokContentHelper(true);
			Date added = new Date();
			service = new HarvestServiceImpl();
			service.setId("TEST");
			String rdf;
			for (int i = 0; i < graphs.getLength(); ++i) {
				Node rdfNode = graphs.item(i);
				rdf = serializeNode(rdfNode);
				ExtractedInfo info = samsokContentHelper.extractInfo(rdf);
				assertNotNull("Ingen info extraherad ur rdf", info);
				SolrInputDocument solrDoc = samsokContentHelper.createSolrDocument(service, rdf, added);
				assertNotNull("Inget solr-dokument från rdf", solrDoc);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

	@Test
	public void testGetRecords_harvestFile_1_1() throws Exception {
		// TODO: detta testfall kanske inte säger så mycket då ingen behandling av det som hämtas görs
		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob(1, 1);
		ByteArrayOutputStream os = null;
		try {
			String url = HOST_URL_BASE + "agenter_hjalm_1.1.xml";
			//String fromDate = "2011-08-01";
			String fromDate = null;
			String toDate = null;
			String metadataPrefix = "ksamsok-rdf";
			String setSpec = "";
			os = new ByteArrayOutputStream(); 
			Logger logger = null;
			StatusService ss = null;
			HarvestService service = null;
			int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate,
					metadataPrefix, setSpec, os, logger, ss, service);
			assertEquals("Fel antal poster", 4, records);

			DocumentBuilder builder = xmlFact.newDocumentBuilder();
			Document doc = builder.parse(new ByteArrayInputStream(os.toByteArray()));
			NodeList graphs = doc.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#","RDF");
			assertNotNull("Inga rdf-grafer", graphs);
			assertEquals("Fel antal rdf-grafer", records, graphs.getLength());
			SamsokContentHelper samsokContentHelper = new SamsokContentHelper(true);
			Date added = new Date();
			service = new HarvestServiceImpl();
			service.setId("TEST");
			String rdf;
			for (int i = 0; i < graphs.getLength(); ++i) {
				Node rdfNode = graphs.item(i);
				rdf = serializeNode(rdfNode);
				ExtractedInfo info = samsokContentHelper.extractInfo(rdf);
				assertNotNull("Ingen info extraherad ur rdf", info);
				SolrInputDocument solrDoc = samsokContentHelper.createSolrDocument(service, rdf, added);
				assertNotNull("Inget solr-dokument från rdf", solrDoc);
			}

		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

	// hjälpmetod som serialiserar en dom-nod som xml utan xml-deklaration
	static String serializeNode(Node node) throws Exception {
		// TODO: använd samma Transformer för en hel serie, kräver refaktorering
		//       av hur ContentHelpers används map deras livscykel
		final int initialSize = 4096;
		Source source = new DOMSource(node);
		Transformer xformer = xformerFact.newTransformer();
		// ingen xml-deklaration då vi vill använda den som ett xml-fragment
		xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		StringWriter sw = new StringWriter(initialSize);
		Result result = new StreamResult(sw);
        xformer.transform(source, result);
        sw.close();
		return sw.toString();
	}

}
