package se.raa.ksamsok.api;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

public class TestAllIndexUniqueValueCount extends AbstractBaseTest {
	ByteArrayOutputStream out;

	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "allIndexUniqueValueCount");
		reqParams.put("index", "itemType|provinceName|serviceOrganization|thumbnailExists");
		reqParams.put("query","yxa");
	}

	@Test
	public void testFacetXMLResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod allIndexUnigueValueCount = apiMethodFactory.getAPIMethod(reqParams, out);
			allIndexUnigueValueCount.setFormat(Format.XML);
			allIndexUnigueValueCount.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			// Travel trough the document
			// The base doc properties, result, version and echo tag
			Node indexes = assertBaseDocProp(resultDoc);;

		} catch (Exception e){
			fail(e.getMessage());
		}
	}
}
