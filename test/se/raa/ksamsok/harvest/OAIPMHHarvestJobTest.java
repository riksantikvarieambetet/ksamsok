package se.raa.ksamsok.harvest;

import java.io.BufferedOutputStream;
import java.io.File;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.OAIPMHHarvestJob;


import junit.framework.TestCase;

public class OAIPMHHarvestJobTest extends TestCase {

	public void testPerformGetRecords() throws Exception {
		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
		
		HarvestService service = new HarvestServiceImpl();
		String deletedRecord = "";
		String granularity = "";
		ServiceMetadata sm = new ServiceMetadata(deletedRecord, granularity);
		String prefix = "";
		String namespace = "";
		String schema = "";
		ServiceFormat f = new ServiceFormat(prefix, namespace, schema);
		File storeTo = new File("file");
		StatusService ss = new StatusServiceImpl(null);
		oaipmhHarvesterJob.performGetRecords(service, sm, f, storeTo, ss);
		fail("Not yet implemented");
	}
	
	public void testGetRecords_harvestChanges() throws Exception {
		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
		
		String url = "http://130.242.42.136/OAICat/tekm/media";
		String fromDate = "2011-08-01";
		String toDate = null;
		String metadataPrefix = "ksamsok-rdf";
		String setSpec = "";
		File storeTo = new File("storeTo.txt");
		OutputStream os = new BufferedOutputStream(new FileOutputStream(storeTo));
		Logger logger = null;
		StatusService ss = null;
		HarvestService service = null;
		int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, ss, service);
		
		System.out.println(records);
		
		
	}
	
	public void testGetRecords_harvestEverything() throws Exception {
		OAIPMHHarvestJob oaipmhHarvesterJob = new OAIPMHHarvestJob();
		
		String url = "http://130.242.42.136/OAICat/tekm/media";
		//String fromDate = "2011-08-01";
		String fromDate = null;
		String toDate = null;
		String metadataPrefix = "ksamsok-rdf";
		String setSpec = "";
		File storeTo = new File("storeTo.txt");
		OutputStream os = new BufferedOutputStream(new FileOutputStream(storeTo));
		Logger logger = null;
		StatusService ss = null;
		HarvestService service = null;
		int records = oaipmhHarvesterJob.getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, ss, service);
		
		System.out.println(records);
	}

}
