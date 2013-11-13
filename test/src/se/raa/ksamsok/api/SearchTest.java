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

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;


public class SearchTest extends AbstractBaseTest{
	static HashMap<String, HashMap<String, Integer>> indexes;
	static int numberOfTermsVal;
	ByteArrayOutputStream out;

	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "search");
		reqParams.put("query","text=yxa");
		reqParams.put("recordSchema","xml");
		reqParams.put("fields","itemLabel,itemDescription,thumbnail,url");
	}
	
	@Test
	public void testSearchWithRecordSchemaXMLResponse(){
		try{
			out = new ByteArrayOutputStream();
			APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
			search.setFormat(Format.XML);
			search.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		} catch (Exception e){
			fail(e.getMessage());
		}

	}
}
