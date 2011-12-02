package se.raa.ksamsok.harvest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class OAIPMHHarvestJobTest {

	private static final int PORT = 7654;
	private static final String HOST_URL_BASE = "http://localhost:" + PORT + "/";

	private static Server server = null;

	@BeforeClass
	public static void setup() throws Exception {
		// starta en lokal jetty och servera statiska filer från test/resources
		server = new Server(PORT);
		ResourceHandler handler = new ResourceHandler();
		handler.setResourceBase("test/resources");
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
		OutputStream os = null;
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
		OutputStream os = null;
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
		} finally {
			if (os != null) {
				os.close();
			}
		}
	}

}
