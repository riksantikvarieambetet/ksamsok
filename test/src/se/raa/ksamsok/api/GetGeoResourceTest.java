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

public class GetGeoResourceTest extends AbstractBaseTest {

	
	private ByteArrayOutputStream out;
	private static HashMap<String, HashMap<String, Integer>> indexes;
	
	@Before
	public void setUp() throws MalformedURLException {
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "getGeoResource");
		reqParams.put("uri", "http://kulturarvsdata.se/resurser/aukt/geo/municipality#0117");
	}
	
	@Test
	public void testGetGeoResourceXMLResponse() {
		try {
			out = new ByteArrayOutputStream();
			APIMethod getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.XML);
			getGeoResource.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
