package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import com.github.jsonldjava.jena.JenaJSONLD;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;
import se.raa.ksamsok.solr.SearchServiceImpl;

public class FacetTest {

	HashMap<String, String> reqParams;
	APIMethodFactory apiMethodFactory;
	ByteArrayOutputStream out;
	static HashMap<String, HashMap<String, Integer>> indexes;
	static int numberOfTermsVal;

	@Before

	public void setUp() throws DiagnosticException, MalformedURLException{
		SolrServer solr = new CommonsHttpSolrServer("http://lx-ra-ksamtest1:8080/solr");
		SearchServiceImpl searchService = new SearchServiceImpl();
		// The solr is @Autowired in the project. It is necessary to set up it by hand in the test cases
		ReflectionTestUtils.setField(searchService,"solr", solr);
		apiMethodFactory = new APIMethodFactory();
		// The searchService is @Autowired in the project. It is necessary to set up it by hand in the test cases
		ReflectionTestUtils.setField(apiMethodFactory,"searchService", searchService);
		JenaJSONLD.init();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "facet");
		reqParams.put("stylesheet", "stylesheet/facet.xsl");
		reqParams.put("index", "countyName|thumbnailExists");
		reqParams.put("query","hus");
		reqParams.put("removeBelow","10");
	}

	@Test
	public void testFacetXMLResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod facet = apiMethodFactory.getAPIMethod(reqParams, out);
			facet.setFormat(Format.XML);
			facet.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
			// Travel trough the document
			// The result tag
			Element result = resultDoc.getDocumentElement();
			assertParent(result,"result");
			// The version tag
			Node version = result.getFirstChild();
			assertParent(version,"version");
			// The version value
			Node versionValue = version.getFirstChild();
			assertEquals(Float.parseFloat(APIMethod.API_VERSION),Float.parseFloat(assertChild(versionValue)),0);
			assertTrue(version.getFirstChild().equals(version.getLastChild()));
			// Number of terms
			Node numberOfTerms = version.getNextSibling();
			assertParent(numberOfTerms,"numberOfTerms");
			// Number of terms value
			Node numberOfTermsValue = numberOfTerms.getFirstChild();
			if (numberOfTermsVal>0){
				// This can only be tested if testFacetJSONRepsonse has been running
				assertEquals(numberOfTermsVal,Integer.parseInt(assertChild(numberOfTermsValue)));
			} else {
				numberOfTermsVal = Integer.parseInt(assertChild(numberOfTermsValue));
				assertTrue(numberOfTermsVal>1);
			}
			assertTrue(numberOfTerms.getFirstChild().equals(numberOfTerms.getLastChild()));
			// The term tags
			Node term=numberOfTerms;
			boolean fillIndexes = false;
			if (indexes == null){
				indexes = new HashMap<String, HashMap<String, Integer>>(); 
				fillIndexes=true;
			}
			for (int i=0; i < numberOfTermsVal; i++){
				term=term.getNextSibling();
				assertParent(term,"term");
				// The index fields tag
				Node indexFields = term.getFirstChild();
				assertParent(indexFields,"indexFields");
				// The index tag
				Node index = indexFields.getFirstChild();
				assertParent(index,"index");
				//The index value
				Node indexValue = index.getFirstChild();
				assertTrue(index.getFirstChild().equals(index.getLastChild()));
				String indexName = assertChild(indexValue);;
				// The value tag
				Node value = index.getNextSibling();
				assertParent(value,"value");
				assertTrue(indexFields.getLastChild().equals(value));
				// The value tag's value
				Node valueValue = value.getFirstChild();
				assertTrue(value.getFirstChild().equals(value.getLastChild()));
				String indexNameValue = assertChild(valueValue);
				// The records tag
				Node records = indexFields.getNextSibling();
				assertParent(records,"records");
				assertTrue(term.getLastChild().equals(records));
				// The records tags value
				Node recordsValue = records.getFirstChild();
				assertTrue(records.getFirstChild().equals(records.getLastChild()));
				int indexNameRecord = Integer.parseInt(assertChild(recordsValue));
				assertTrue(indexNameRecord>=Integer.parseInt(reqParams.get("removeBelow")));
				if (fillIndexes){
					// Store fetch value to compare with json result 
					HashMap<String, Integer> indexRecords = indexes.get(indexName);
					if (indexRecords == null){
						indexRecords = new HashMap<String, Integer>();
						indexes.put(indexName, indexRecords);
					}
					indexRecords.put(indexNameValue, indexNameRecord);
				} else {
					// This can only be tested if testFacetJSONRepsonse has been running
					// Compare with the json result
					HashMap<String, Integer> indexRecords =indexes.get(indexName);
					assertNotNull(indexRecords);
					assertEquals((int) indexRecords.get(indexNameValue),indexNameRecord);
				}
			}
			// The echo element
			Node echo = term.getNextSibling();
			assertParent(echo,"echo");
			assertTrue(result.getLastChild().equals(echo));
			// The method element
			Node method = echo.getFirstChild();
			assertParent(method,"method");
			// The method value
			Node methodValue = method.getFirstChild();
			assertTrue(reqParams.get("method").equals(assertChild(methodValue)));
			// The first index
			Node index1 = method.getNextSibling();
			assertParent(index1,"index");				// This can only be tested if testFacetJSONRepsonse has been running

			// The first index's value
			Node index1Value = index1.getFirstChild();
			assertTrue(reqParams.get("index").contains(assertChild(index1Value)));
			// The second index
			Node index2 = index1.getNextSibling();
			assertParent(index2,"index");
			// The second index's value
			Node index2Value = index2.getFirstChild();
			assertTrue(reqParams.get("index").contains(assertChild(index2Value)));
			// The removeBelow element
			Node removeBelow = index2.getNextSibling();
			assertParent(removeBelow,"removeBelow");
			// The removeBelow value
			Node removeBelowValue = removeBelow.getFirstChild();
			assertEquals(Integer.parseInt(reqParams.get("removeBelow")),Integer.parseInt(assertChild(removeBelowValue)));
			// The query element
			Node query = removeBelow.getNextSibling();
			assertParent(query,"query");
			assertTrue(echo.getLastChild().equals(query));
			// The query value
			Node queryValue = query.getFirstChild();
			assertTrue(reqParams.get("query").equals(assertChild(queryValue)));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	/**
	 * This method assert the base properties of the xml document like verions, encoding and stylesheet
	 * @param doc - The document to assert
	 */
	private void assertBaseDocProp(Document doc){
		// Check encoding
		assertTrue(doc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
		// Check version
		assertTrue(doc.getXmlVersion().equalsIgnoreCase("1.0"));
		// Check stylesheet
		if (reqParams.containsKey("stylesheet")){

			ProcessingInstruction styleElement = (ProcessingInstruction) doc.getDocumentElement().getPreviousSibling();
			assertNotNull(styleElement);
			assertTrue(styleElement.getData().contains(reqParams.get("stylesheet")));
		} else {
			assertNull(doc.getDocumentElement().getPreviousSibling());
		}
	}
	/**
	 * This method asserts a child node, i.e. a node without any childs, and returns the node's value
	 * @param node - The node to assert
	 * @return - A string with the node's value
	 */
	private String assertChild(Node node) {
		assertTrue(node.getNodeType()==Node.TEXT_NODE);
		assertNull(node.getFirstChild());		
		return node.getNodeValue();
	}
	/**
	 * This method asserts a parent node, i.e. a node without value but with at least one child.
	 * @param node - The node to assert
	 * @param nodeName - The name it should have
	 */
	private void assertParent(Node node, String nodeName){
		assertTrue(node.getNodeName().equals(nodeName));
		assertEquals(0,node.getAttributes().getLength());
		assertNull(node.getNodeValue());
		assertTrue(node.getNodeType()==Element.ELEMENT_NODE);
		assertTrue(node.getChildNodes().getLength()>0);
	}
	
	@Test
	public void testFacetJSONResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod facet = apiMethodFactory.getAPIMethod(reqParams, out);
			facet.setFormat(Format.JSON_LD);
			facet.performMethod();
			JSONObject jsonResult = new JSONObject(out.toString("UTF-8"));
			// The result object
			assertEquals(1,jsonResult.length());
			assertTrue(jsonResult.has("result"));
			JSONObject result = jsonResult.getJSONObject("result");
			assertEquals(4, result.length());
			assertTrue(result.has("term"));
			assertTrue(result.has("echo"));
			assertTrue(result.has("version"));
			assertEquals(Float.parseFloat(APIMethod.API_VERSION),result.getInt("version"),0);
			assertTrue(result.has("numberOfTerms"));
			if (numberOfTermsVal>0){
				// This can only be tested if testFacetXMLRepsonse has been running
				assertEquals(numberOfTermsVal,result.getInt("numberOfTerms"));
			} else {
				numberOfTermsVal=result.getInt("numberOfTerms");
				assertTrue(numberOfTermsVal>1);
			}
			// The terms object
			JSONArray terms = result.getJSONArray("term");
			assertEquals(numberOfTermsVal,terms.length());
			// Check to see if testFacetXMLResponse has been running
			boolean fillIndexes = false;
			if (indexes == null){
				fillIndexes=true;
				indexes = new HashMap<String, HashMap<String,Integer>>(); 
			}
			for (int i=0; i< numberOfTermsVal; i++){
				JSONObject term = terms.getJSONObject(i);
				assertEquals(2, term.length());
				assertTrue(term.has("indexFields"));
				assertTrue(term.has("records"));
				JSONObject indexFields = term.getJSONObject("indexFields");
				assertEquals(2, indexFields.length());
				assertTrue(indexFields.has("index"));
				assertTrue(indexFields.has("value"));
				if (fillIndexes){
					// Store fetch value to compare with xml result 
					HashMap<String, Integer> indexRecords = indexes.get(indexFields.getString("index"));
					if (indexRecords == null){
						indexRecords = new HashMap<String, Integer>();
						indexes.put(indexFields.getString("index"), indexRecords);
					}
					indexRecords.put(indexFields.getString("value"), term.getInt("records"));
				} else {
					// This can only be tested if testFacetXMLRepsonse has been running
					// Compare with the xml result
					HashMap<String, Integer> indexRecords =indexes.get(indexFields.getString("index"));
					assertNotNull(indexRecords);
					assertEquals((int) indexRecords.get(indexFields.getString("value")),term.getInt("records"));
				}
			}
			// The echo object
			JSONObject echo = result.getJSONObject("echo");
			assertEquals(4,echo.length());
			// The echo index object
			assertTrue(echo.has("index"));
			JSONArray index = echo.getJSONArray("index");
			assertEquals(2,index.length());
			for (int i=0; i < index.length();i++){
				assertTrue(reqParams.get("index").contains(index.getString(i)));
			}
			// The query property
			assertTrue(echo.has("query"));
			assertTrue(reqParams.get("query").equals(echo.getString("query")));
			// The method property
			assertTrue(echo.has("method"));
			assertTrue(reqParams.get("method").equals(echo.getString("method")));
			// The remove below property
			assertTrue(echo.has("removeBelow"));
			assertEquals(Integer.parseInt(reqParams.get("removeBelow")),echo.getInt("removeBelow"));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testFacetMissingIndexParam(){
			out = new ByteArrayOutputStream();
			reqParams.remove("index");
			APIMethod facet;
			try {
				facet = apiMethodFactory.getAPIMethod(reqParams, out);
				facet.performMethod();
				fail("No exception was thrown, expected MissingParameterException");
			} catch (MissingParameterException e) {
				// Ignore, correct exception was trown
			} catch (DiagnosticException e) {
				fail("Wrong exception thrown");
			} catch (BadParameterException e) {
				fail("Wrong exception thrown");
			}
	}

	@Test
	public void testFacetMissingQueryParam(){
			out = new ByteArrayOutputStream();
			reqParams.remove("query");
			APIMethod facet;
			try {
				facet = apiMethodFactory.getAPIMethod(reqParams, out);
				facet.performMethod();
				fail("No exception was thrown, expected MissingParameterException");
			} catch (MissingParameterException e) {
				// Ignore, correct exception was trown
			} catch (DiagnosticException e) {
				fail("Wrong exception thrown");
			} catch (BadParameterException e) {
				fail("Wrong exception thrown");
			}
	}

}
