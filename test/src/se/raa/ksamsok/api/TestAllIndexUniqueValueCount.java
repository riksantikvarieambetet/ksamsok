package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import se.raa.ksamsok.api.exception.MissingParameterException;
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
	public void testAllIndexXMLResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod allIndexUnigueValueCount = apiMethodFactory.getAPIMethod(reqParams, out);
			allIndexUnigueValueCount.setFormat(Format.XML);
			allIndexUnigueValueCount.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			//System.out.println(out.toString("UTF-8"));
			// Travel trough the document
			// The base doc properties, result, version and echo tag
			Node index = assertBaseDocProp(resultDoc);
			while (index.getNodeName().equals("index")){
				assertParent(index, "index");
				assertEquals(2, index.getChildNodes().getLength());
				Node name = index.getFirstChild();
				assertParent(name, "name");
				Node nameValue = name.getFirstChild();
				assertTrue(reqParams.get("index").contains(assertChild(nameValue)));
				Node uniqueValues = name.getNextSibling();
				assertParent(uniqueValues,"uniqueValues");
				Node uniqueValuesValue = uniqueValues.getFirstChild();
				assertTrue(Integer.parseInt(assertChild(uniqueValuesValue))>0);
				index=index.getNextSibling();
			}
		} catch (Exception e){
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testAllIndexJSONResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod allIndexUnigueValueCount = apiMethodFactory.getAPIMethod(reqParams, out);
			allIndexUnigueValueCount.setFormat(Format.JSON_LD);
			allIndexUnigueValueCount.performMethod();
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			JSONObject result = assertBaseJSONProp(response);
			JSONArray indexs = result.getJSONArray("index");
			for (int i = 0; i < indexs.length(); i++){
				JSONObject index = indexs.getJSONObject(i);
				assertTrue(index.has("name"));
				assertTrue(index.has("uniqueValues"));
				assertEquals(2,index.length());
				assertTrue(reqParams.get("index").contains(index.getString("name")));
				assertTrue(index.getInt("uniqueValues")>0);
			}
		} catch (Exception e){
			fail(e.getMessage());
		}
	}
	@Test
	public void testAllIndexNoQueryParam(){
		reqParams.remove("query");
		try {
			out = new ByteArrayOutputStream();
			APIMethod allIndexUnigueValueCount = apiMethodFactory.getAPIMethod(reqParams, out);
			allIndexUnigueValueCount.performMethod();
			System.out.println(out.toString("UTF-8"));
			fail("No exception was thrown, expected MissingParamterException");
		} catch (MissingParameterException e) {
			// Expected exception 
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected MissingParamterException");
		}
	}
	
}
