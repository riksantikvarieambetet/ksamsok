package se.raa.ksamsok.lucene;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.w3c.dom.Document;
import se.raa.ksamsok.harvest.ExtractedInfo;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import javax.swing.text.AbstractDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;

import static org.junit.Assert.*;

public class ContentHelperTest {

	@Test
	public void testIndexExists() {
		assertTrue(ContentHelper.indexExists(ContentHelper.IX_CONTINENTNAME));

		// test the regexp for splitting on underscore
		assertFalse(ContentHelper.indexExists("foo_bar"));
		assertTrue(ContentHelper.indexExists("foo_" + ContentHelper.IX_CONTINENTNAME));
	}
}
