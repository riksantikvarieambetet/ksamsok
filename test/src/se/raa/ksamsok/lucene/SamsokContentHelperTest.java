package se.raa.ksamsok.lucene;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;

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

import se.raa.ksamsok.harvest.ExtractedInfo;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;
import se.raa.ksamsok.spatial.GMLInfoHolder;

public class SamsokContentHelperTest {

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;
	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	@Test
	public void testExtractInfo__0_TO_1_0() throws Exception {
		SamsokContentHelper helper = new SamsokContentHelper();
		GMLInfoHolder gmlInfoHolder = new GMLInfoHolder();
		String xmlContent = loadTestFileAsString("hjalm_0.99.rdf");
		ExtractedInfo extractedInfo = helper.extractInfo(xmlContent, gmlInfoHolder);
		assertNotNull("Ingen extractedInfo", extractedInfo);
		assertNotNull("Ingen idenfierare", extractedInfo.getIdentifier());
		assertNotNull("Ingen url", extractedInfo.getNativeURL());
		assertEquals("Fel identfierare", "http://kulturarvsdata.se/raa/test/1", extractedInfo.getIdentifier());
		assertFalse("Gml-info ska inte finnas", gmlInfoHolder.hasGeometries());
	}

	@Test
	public void testExtractInfo_1_1() throws Exception {
		SamsokContentHelper helper = new SamsokContentHelper();
		GMLInfoHolder gmlInfoHolder = new GMLInfoHolder();
		String xmlContent = loadTestFileAsString("hjalm_1.1.rdf");
		ExtractedInfo extractedInfo = helper.extractInfo(xmlContent, gmlInfoHolder);
		assertNotNull("Ingen extractedInfo", extractedInfo);
		assertNotNull("Ingen idenfierare", extractedInfo.getIdentifier());
		assertNotNull("Ingen url", extractedInfo.getNativeURL());
		assertEquals("Fel identfierare", "http://kulturarvsdata.se/raa/test/1", extractedInfo.getIdentifier());
		assertFalse("Gml-info ska inte finnas", gmlInfoHolder.hasGeometries());
	}

	@Test
	public void testCreateDoc_0_TO_1_0() throws Exception {
		SamsokContentHelper helper = new SamsokContentHelper();
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		String xmlContent = loadTestFileAsString("hjalm_0.99.rdf");
		SolrInputDocument doc = helper.createSolrDocument(service, xmlContent, new Date());
		assertNotNull("Inget solr-dokument", doc);
		// kolla system-index
		assertEquals("Felaktigt service-id", "TESTID", doc.getFieldValue(ContentHelper.I_IX_SERVICE));
		assertEquals("Fel identifierare", "http://kulturarvsdata.se/raa/test/1", doc.getFieldValue(ContentHelper.IX_ITEMID));
		assertNotNull("Ingen RDF", doc.getFieldValue(ContentHelper.I_IX_RDF));
		assertNotNull("Inget pres-block", doc.getFieldValue(ContentHelper.I_IX_PRES));
		// specialindexet för relationer
		Collection<Object> relations = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
		assertNotNull("Specialindexet för relationer saknas", relations);
		assertEquals("Specialindexet för relationer har fel antal", 1, relations.size());
		assertEquals("Felaktig relation", "isRelatedTo|http://kulturarvsdata.se/raa/test/2", relations.iterator().next());

		assertNotNull("Ingen beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION));
		assertTrue("Fel beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION).toString().contains("som beskriver Gustav"));
	}

	@Test
	public void testCreateDoc_0_TO_1_0_All() throws Exception {
		// test av (nästan) allt

		SamsokContentHelper helper = new SamsokContentHelper();
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		String xmlContent = loadTestFileAsString("alla_index_0.99.rdf");
		Date addedDate = new Date();
		SolrInputDocument doc = helper.createSolrDocument(service, xmlContent, addedDate);
		assertNotNull("Inget solr-dokument", doc);
		// kolla system-index
		
		singleValueIndexAssert(doc, ContentHelper.I_IX_SERVICE, "TESTID");
		singleValueIndexAssert(doc, ContentHelper.IX_SERVICENAME, "test");
		singleValueIndexAssert(doc, ContentHelper.IX_SERVICEORGANISATION, "TEST");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMID, "http://kulturarvsdata.se/raa/test/1");
		assertNotNull("Ingen RDF", doc.getFieldValue(ContentHelper.I_IX_RDF));
		assertNotNull("Inget pres-block", doc.getFieldValue(ContentHelper.I_IX_PRES));
		assertNotNull("Ingen latitud", doc.getFieldValue(ContentHelper.I_IX_LAT));
		assertNotNull("Ingen longitud", doc.getFieldValue(ContentHelper.I_IX_LON));

		singleValueIndexAssert(doc, ContentHelper.IX_ISRELATEDTO, "http://kulturarvsdata.se/raa/test/2");
		singleValueIndexAssert(doc, ContentHelper.IX_CONTAINSINFORMATIONABOUT, "http://kulturarvsdata.se/raa/test/3");
		singleValueIndexAssert(doc, ContentHelper.IX_CONTAINSOBJECT, "http://kulturarvsdata.se/raa/test/4");
		singleValueIndexAssert(doc, ContentHelper.IX_HASBEENUSEDIN, "http://kulturarvsdata.se/raa/test/5");
		singleValueIndexAssert(doc, ContentHelper.IX_HASCHILD, "http://kulturarvsdata.se/raa/test/6");
		singleValueIndexAssert(doc, ContentHelper.IX_HASFIND, "http://kulturarvsdata.se/raa/test/7");
		singleValueIndexAssert(doc, ContentHelper.IX_HASIMAGE, "http://kulturarvsdata.se/raa/test/8");
		multipleValueIndexAssert(doc, ContentHelper.IX_HASOBJECTEXAMPLE, new String[] {
				"http://kulturarvsdata.se/raa/test/9", "http://kulturarvsdata.se/raa/test/10"	
		}, 2);
		singleValueIndexAssert(doc, ContentHelper.IX_HASPARENT, "http://kulturarvsdata.se/raa/test/11");
		singleValueIndexAssert(doc, ContentHelper.IX_HASPART, "http://kulturarvsdata.se/raa/test/12");
		singleValueIndexAssert(doc, ContentHelper.IX_ISDESCRIBEDBY, "http://kulturarvsdata.se/raa/test/13");
		singleValueIndexAssert(doc, ContentHelper.IX_ISFOUNDIN, "http://kulturarvsdata.se/raa/test/14");
		singleValueIndexAssert(doc, ContentHelper.IX_ISPARTOF, "http://kulturarvsdata.se/raa/test/15");
		singleValueIndexAssert(doc, ContentHelper.IX_ISVISUALIZEDBY, "http://kulturarvsdata.se/raa/test/16");
		singleValueIndexAssert(doc, ContentHelper.IX_SAMEAS, "http://kulturarvsdata.se/raa/test/17");
		singleValueIndexAssert(doc, ContentHelper.IX_VISUALIZES, "http://kulturarvsdata.se/raa/test/18");

		// specialindexet för relationer
		multipleValueIndexAssert(doc, ContentHelper.I_IX_RELATIONS, new String[] {
				ContentHelper.IX_ISRELATEDTO + "|" + "http://kulturarvsdata.se/raa/test/2",
				ContentHelper.IX_CONTAINSINFORMATIONABOUT + "|" + "http://kulturarvsdata.se/raa/test/3",
				ContentHelper.IX_CONTAINSOBJECT + "|" + "http://kulturarvsdata.se/raa/test/4",
				ContentHelper.IX_HASBEENUSEDIN + "|" + "http://kulturarvsdata.se/raa/test/5",
				ContentHelper.IX_HASCHILD + "|" + "http://kulturarvsdata.se/raa/test/6",
				ContentHelper.IX_HASFIND + "|" + "http://kulturarvsdata.se/raa/test/7",
				ContentHelper.IX_HASIMAGE + "|" + "http://kulturarvsdata.se/raa/test/8",
				ContentHelper.IX_HASOBJECTEXAMPLE + "|" + "http://kulturarvsdata.se/raa/test/9",
				ContentHelper.IX_HASOBJECTEXAMPLE + "|" + "http://kulturarvsdata.se/raa/test/10",
				ContentHelper.IX_HASPARENT + "|" + "http://kulturarvsdata.se/raa/test/11",
				ContentHelper.IX_HASPART + "|" + "http://kulturarvsdata.se/raa/test/12",
				ContentHelper.IX_ISDESCRIBEDBY + "|" + "http://kulturarvsdata.se/raa/test/13",
				ContentHelper.IX_ISFOUNDIN + "|" + "http://kulturarvsdata.se/raa/test/14",
				ContentHelper.IX_ISPARTOF + "|" + "http://kulturarvsdata.se/raa/test/15",
				ContentHelper.IX_ISVISUALIZEDBY + "|" + "http://kulturarvsdata.se/raa/test/16",
				ContentHelper.IX_SAMEAS + "|" + "http://kulturarvsdata.se/raa/test/17",
				ContentHelper.IX_VISUALIZES + "|" + "http://kulturarvsdata.se/raa/test/18"
		}, 17);

		// \u00e5 å, \u00e4 ä,  \u00f6 ö
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMTITLE, "Gustav Vasas hj\u00e4lm");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMLABEL, "Hj\u00e4lm");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMTYPE, "Objekt/f\u00f6rem\u00e5l");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMCLASS, "http://raa.se/test/itemClass#test");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMCLASSNAME, "Test");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMNAME, "F\u00f6rem\u00e5l");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMSPECIFICATION, "Huvudsak");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMKEYWORD, "Hj\u00e4lm");
		multipleValueIndexAssert(doc, ContentHelper.IX_ITEMMOTIVEWORD, new String[] {
				"Hj\u00e4lm", "Huvudbonad"
		}, 2);
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMMATERIAL, "metall");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMTECHNIQUE, "gjuten");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMSTYLE, "tuff");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMCOLOR, "metall");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMNUMBER, "4711");
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMDESCRIPTION, "som beskriver Gustav", true);
		singleValueIndexAssert(doc, ContentHelper.IX_ITEMLICENSE, "http://kulturarvsdata.se/resurser/license#public");

		// klassificering
		singleValueIndexAssert(doc, ContentHelper.IX_SUBJECT, "Kulturhistoria");
		singleValueIndexAssert(doc, ContentHelper.IX_COLLECTION, "En samling");
		singleValueIndexAssert(doc, ContentHelper.IX_DATAQUALITY, "Bearbetad");
		singleValueIndexAssert(doc, ContentHelper.IX_MEDIATYPE, "text/html");
		singleValueIndexAssert(doc, ContentHelper.IX_THEME, "http://kulturarvsdata.se/resurser/Theme#test");
		// dates
		singleValueIndexAssert(doc, ContentHelper.IX_CREATEDDATE, "2006-01-01");
		singleValueIndexAssert(doc, ContentHelper.IX_ADDEDTOINDEXDATE, ContentHelper.formatDate(addedDate, false));
		singleValueIndexAssert(doc, ContentHelper.IX_LASTCHANGEDDATE, "2009-03-06");
		// specialindex
		singleValueIndexAssert(doc, ContentHelper.IX_GEODATAEXISTS, "j");
		singleValueIndexAssert(doc, ContentHelper.IX_TIMEINFOEXISTS, "j");
		singleValueIndexAssert(doc, ContentHelper.IX_THUMBNAILEXISTS, "j");

		// bilder
		singleValueIndexAssert(doc, ContentHelper.IX_MEDIALICENSE, "http://kulturarvsdata.se/resurser/License#test");
		singleValueIndexAssert(doc, ContentHelper.IX_MEDIAMOTIVEWORD, "Stockholms slott");

		// kontext
		multipleValueIndexAssert(doc, ContentHelper.IX_CONTEXTTYPE, new String[] {
				"create", "use"
		}, 2);
		multipleValueIndexAssert(doc, ContentHelper.IX_CONTEXTLABEL, new String[] {
				"Tillverkad", "Brukad"
		}, 2);

		// kontext, plats
		singleValueIndexAssert(doc, ContentHelper.IX_PLACENAME, "Stockholm");
		singleValueIndexAssert(doc, ContentHelper.IX_CADASTRALUNIT, "Stockholm 1:1");
		singleValueIndexAssert(doc, ContentHelper.IX_PLACETERMID, "4711");
		singleValueIndexAssert(doc, ContentHelper.IX_PLACETERMAUTH, "Vaktm\u00e4staren");
		singleValueIndexAssert(doc, ContentHelper.IX_CONTINENTNAME, "Europa");
		singleValueIndexAssert(doc, ContentHelper.IX_COUNTRYNAME, "Sverige");
		singleValueIndexAssert(doc, ContentHelper.IX_COUNTYNAME, "Stockholm");
		singleValueIndexAssert(doc, ContentHelper.IX_MUNICIPALITYNAME, "Stockholm");
		singleValueIndexAssert(doc, ContentHelper.IX_PROVINCENAME, "Uppland");
		singleValueIndexAssert(doc, ContentHelper.IX_PARISHNAME, "Stockholm");
		singleValueIndexAssert(doc, ContentHelper.IX_COUNTRY, "se");
		singleValueIndexAssert(doc, ContentHelper.IX_COUNTY, "1");
		singleValueIndexAssert(doc, ContentHelper.IX_MUNICIPALITY, "180");
		singleValueIndexAssert(doc, ContentHelper.IX_PROVINCE, "Up");
		singleValueIndexAssert(doc, ContentHelper.IX_PARISH, "91");

		// kontext, agent
		singleValueIndexAssert(doc, ContentHelper.IX_FIRSTNAME, "Gustav");
		singleValueIndexAssert(doc, ContentHelper.IX_SURNAME, "Vasa");
		singleValueIndexAssert(doc, ContentHelper.IX_FULLNAME, "Gustav Vasa");
		multipleValueIndexAssert(doc, ContentHelper.IX_NAME, new String[] {
				"Gustav Vasa", "Kunz Lochner"	
		}, 2);
		singleValueIndexAssert(doc, ContentHelper.IX_GENDER, "male");
		singleValueIndexAssert(doc, ContentHelper.IX_ORGANIZATION, "Kungarna");
		multipleValueIndexAssert(doc, ContentHelper.IX_TITLE, new String[] {
				"Kung", "Smed"	
		}, 2);
		singleValueIndexAssert(doc, ContentHelper.IX_FULLNAME, "Gustav Vasa");
		singleValueIndexAssert(doc, ContentHelper.IX_NAMEID, "59878606");
		singleValueIndexAssert(doc, ContentHelper.IX_NAMEAUTH, "VIAF");

		// kontext, tid
		multipleValueIndexAssert(doc, ContentHelper.IX_FROMTIME, new String[] {
				"1540", "1542"	
		}, 2);
		singleValueIndexAssert(doc, "create_" + ContentHelper.IX_FROMTIME, "1540");
		singleValueIndexAssert(doc, "use_" + ContentHelper.IX_FROMTIME, "1542");
		multipleValueIndexAssert(doc, ContentHelper.IX_TOTIME, new String[] {
				"1541", "1560"	
		}, 2);
		singleValueIndexAssert(doc, "create_" + ContentHelper.IX_TOTIME, "1541");
		singleValueIndexAssert(doc, "use_" + ContentHelper.IX_TOTIME, "1560");
		multipleValueIndexAssert(doc, ContentHelper.IX_DECADE, new String[] {
				"1540", "1550", "1560"
		}, -1); // 4 värden, 1540 är med 2 ggr, fixa i TimeUtil-metoden?
		singleValueIndexAssert(doc, "create_" + ContentHelper.IX_DECADE, "1540");
		multipleValueIndexAssert(doc, "use_" + ContentHelper.IX_DECADE, new String[] {
				"1540", "1550", "1560"
		}, 3);
		multipleValueIndexAssert(doc, ContentHelper.IX_CENTURY, new String[] {
				"1500"	
		}, -1); // 2 värden, 1500 är med 2 ggr, fixa i TimeUtil-metoden?
		singleValueIndexAssert(doc, "create_" + ContentHelper.IX_CENTURY, "1500");
		singleValueIndexAssert(doc, "use_" + ContentHelper.IX_CENTURY, "1500");

		singleValueIndexAssert(doc, ContentHelper.IX_FROMPERIODNAME, "Omodern tid");
		singleValueIndexAssert(doc, ContentHelper.IX_TOPERIODNAME, "Modern tid");

		singleValueIndexAssert(doc, ContentHelper.IX_FROMPERIODID, "1234");
		singleValueIndexAssert(doc, ContentHelper.IX_TOPERIODID, "5678");

		singleValueIndexAssert(doc, ContentHelper.IX_PERIODAUTH, "Tidsoptimisterna");

		singleValueIndexAssert(doc, ContentHelper.IX_EVENTNAME, "Hj\u00e4lmhamrande");
		singleValueIndexAssert(doc, ContentHelper.IX_EVENTAUTH, "Hj\u00e4lmhamrarf\u00f6rbundet");

	}

	@Test
	public void testCreateDoc_1_1() throws Exception {
		SamsokContentHelper helper = new SamsokContentHelper();
		HarvestService service = new HarvestServiceImpl();
		service.setId("TESTID");
		String xmlContent = loadTestFileAsString("hjalm_1.1.rdf");
		SolrInputDocument doc = helper.createSolrDocument(service, xmlContent, new Date());
		assertNotNull("Inget solr-dokument", doc);
		// kolla system-index
		assertEquals("Felaktigt service-id", "TESTID", doc.getFieldValue(ContentHelper.I_IX_SERVICE));
		assertEquals("Fel identifierare", "http://kulturarvsdata.se/raa/test/1", doc.getFieldValue(ContentHelper.IX_ITEMID));
		assertNotNull("Ingen RDF", doc.getFieldValue(ContentHelper.I_IX_RDF));
		assertNotNull("Inget pres-block", doc.getFieldValue(ContentHelper.I_IX_PRES));
		// specialindexet för relationer
		Collection<Object> relations = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
		assertNotNull("Specialindexet för relationer saknas", relations);
		assertEquals("Specialindexet för relationer har fel antal", 2, relations.size());
		assertTrue("Specialindexvärde finns inte med", relations.contains("isRelatedTo|http://kulturarvsdata.se/raa/test/2"));
		assertTrue("Specialindexvärde finns inte med", relations.contains("has_former_or_current_owner|http://libris.kb.se/resource/auth/58087"));

		assertNotNull("Ingen beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION));
		assertTrue("Fel beskrivning", doc.getFieldValue(ContentHelper.IX_ITEMDESCRIPTION).toString().contains("som beskriver Gustav"));
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

	private void multipleValueIndexAssert(SolrInputDocument doc, String indexName, String[] values, int count) {
		Collection<Object> docValues = doc.getFieldValues(indexName);
		assertNotNull("Fältet " + indexName + " saknas", docValues);
		if (count > 0) {
			assertEquals("Fältet " + indexName + " innehåller fel antal värden, värden är" +
					docValues, count, docValues.size());
		}
		for (String value: values) {
			assertTrue("Värdet " + value + " saknas för " + indexName +
					", värden är " + docValues, docValues.contains(value));
		}
	}

	private String loadTestFileAsString(String fileName) throws Exception {
		DocumentBuilder builder = xmlFact.newDocumentBuilder();
		InputStream is = null;
		StringWriter sw = null;
		try {
			// förutsätter att testfallen körs med projektkatalogen som cwd
			// vilket normalt är fallet både från ant och i eclipse
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
