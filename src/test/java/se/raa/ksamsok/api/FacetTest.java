package se.raa.ksamsok.api;

import org.junit.Before;
import org.junit.Test;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.fail;

public class FacetTest extends AbstractBaseTest{

	ByteArrayOutputStream out;
	static HashMap<String, HashMap<String, Integer>> indexes;
	static int numberOfTermsVal;

	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "facet");
		reqParams.put("stylesheet", "stylesheet/facet.xsl");
		reqParams.put("index", "countyName|thumbnailExists");
		reqParams.put("query","hus");
		reqParams.put("removeBelow","10");
	}


	//TODO: kommentera tillbaka när det finns data i indexet, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testFacetXMLResponse(){
//		try {
//			out = new ByteArrayOutputStream();
//			APIMethod facet = apiMethodFactory.getAPIMethod(reqParams, out);
//			facet.setFormat(APIMethod.Format.XML);
//			facet.performMethod();
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder=null;
//			docBuilder = docFactory.newDocumentBuilder();
//			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
//			// Travel trough the document
//			// The base doc properties, result, version and echo tag
//			Node numberOfTerms = assertBaseDocProp(resultDoc);
//			// Number of terms
//			assertParent(numberOfTerms,"numberOfTerms");
//			// Number of terms value
//			Node numberOfTermsValue = numberOfTerms.getFirstChild();
//			if (numberOfTermsVal>0){
//				// This can only be tested if testFacetJSONRepsonse has been running
//				assertEquals(numberOfTermsVal,Integer.parseInt(assertChild(numberOfTermsValue)));
//			} else {
//				numberOfTermsVal = Integer.parseInt(assertChild(numberOfTermsValue));
//				assertTrue(numberOfTermsVal>1);
//			}
//			assertTrue(numberOfTerms.getFirstChild().equals(numberOfTerms.getLastChild()));
//			// The term tags
//			Node term=numberOfTerms;
//			boolean fillIndexes = false;
//			if (indexes == null){
//				indexes = new HashMap<String, HashMap<String, Integer>>();
//				fillIndexes=true;
//			}
//			for (int i=0; i < numberOfTermsVal; i++){
//				term=term.getNextSibling();
//				assertParent(term,"term");
//				// The index fields tag
//				Node indexFields = term.getFirstChild();
//				assertParent(indexFields,"indexFields");
//				// The index tag
//				Node index = indexFields.getFirstChild();
//				assertParent(index,"index");
//				//The index value
//				Node indexValue = index.getFirstChild();
//				assertTrue(index.getFirstChild().equals(index.getLastChild()));
//				String indexName = assertChild(indexValue);
//				// The value tag
//				Node value = index.getNextSibling();
//				assertParent(value,"value");
//				assertTrue(indexFields.getLastChild().equals(value));
//				// The value tag's value
//				Node valueValue = value.getFirstChild();
//				assertTrue(value.getFirstChild().equals(value.getLastChild()));
//				String indexNameValue = assertChild(valueValue);
//				// The records tag
//				Node records = indexFields.getNextSibling();
//				assertParent(records,"records");
//				assertTrue(term.getLastChild().equals(records));
//				// The records tags value
//				Node recordsValue = records.getFirstChild();
//				assertTrue(records.getFirstChild().equals(records.getLastChild()));
//				int indexNameRecord = Integer.parseInt(assertChild(recordsValue));
//				assertTrue(indexNameRecord>=Integer.parseInt(reqParams.get("removeBelow")));
//				if (fillIndexes){
//					// Store fetch value to compare with json result
//					HashMap<String, Integer> indexRecords = indexes.get(indexName);
//					if (indexRecords == null){
//						indexRecords = new HashMap<String, Integer>();
//						indexes.put(indexName, indexRecords);
//					}
//					indexRecords.put(indexNameValue, indexNameRecord);
//				} else {
//					// This can only be tested if testFacetJSONRepsonse has been running
//					// Compare with the json result
//					HashMap<String, Integer> indexRecords =indexes.get(indexName);
//					assertNotNull(indexRecords);
//					assertEquals((int) indexRecords.get(indexNameValue),indexNameRecord);
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}

	// TODO: Kommentera tillbaka när det finns data att testa mot i indexet, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testFacetJSONResponse(){
//		try {
//			out = new ByteArrayOutputStream();
//			APIMethod facet = apiMethodFactory.getAPIMethod(reqParams, out);
//			facet.setFormat(APIMethod.Format.JSON_LD);
//			facet.performMethod();
//			JSONObject jsonResult = new JSONObject(out.toString("UTF-8"));
//			// The result object
//			assertEquals(1,jsonResult.length());
//			assertTrue(jsonResult.has("result"));
//			JSONObject result = jsonResult.getJSONObject("result");
//			assertEquals(4, result.length());
//			assertTrue(result.has("term"));
//			assertTrue(result.has("echo"));
//			assertTrue(result.has("version"));
//			assertEquals(Float.parseFloat(APIMethod.API_VERSION),result.getInt("version"),0);
//			assertTrue(result.has("numberOfTerms"));
//			if (numberOfTermsVal>0){
//				// This can only be tested if testFacetXMLRepsonse has been running
//				assertEquals(numberOfTermsVal,result.getInt("numberOfTerms"));
//			} else {
//				numberOfTermsVal=result.getInt("numberOfTerms");
//				assertTrue(numberOfTermsVal>1);
//			}
//			// The terms object
//			JSONArray terms = result.getJSONArray("term");
//			assertEquals(numberOfTermsVal,terms.length());
//			// Check to see if testFacetXMLResponse has been running
//			boolean fillIndexes = false;
//			if (indexes == null){
//				fillIndexes=true;
//				indexes = new HashMap<String, HashMap<String,Integer>>();
//			}
//			for (int i=0; i< numberOfTermsVal; i++){
//				JSONObject term = terms.getJSONObject(i);
//				assertEquals(2, term.length());
//				assertTrue(term.has("indexFields"));
//				assertTrue(term.has("records"));
//				JSONObject indexFields = term.getJSONObject("indexFields");
//				assertEquals(2, indexFields.length());
//				assertTrue(indexFields.has("index"));
//				assertTrue(indexFields.has("value"));
//				if (fillIndexes){
//					// Store fetch value to compare with xml result
//					HashMap<String, Integer> indexRecords = indexes.get(indexFields.getString("index"));
//					if (indexRecords == null){
//						indexRecords = new HashMap<String, Integer>();
//						indexes.put(indexFields.getString("index"), indexRecords);
//					}
//					indexRecords.put(indexFields.getString("value"), term.getInt("records"));
//				} else {
//					// This can only be tested if testFacetXMLRepsonse has been running
//					// Compare with the xml result
//					HashMap<String, Integer> indexRecords =indexes.get(indexFields.getString("index"));
//					assertNotNull(indexRecords);
//					assertEquals((int) indexRecords.get(indexFields.getString("value")),term.getInt("records"));
//				}
//			}
//			// The echo object
//			JSONObject echo = result.getJSONObject("echo");
//			assertEquals(4,echo.length());
//			// The echo index object
//			assertTrue(echo.has("index"));
//			JSONArray index = echo.getJSONArray("index");
//			assertEquals(2,index.length());
//			for (int i=0; i < index.length();i++){
//				assertTrue(reqParams.get("index").contains(index.getString(i)));
//			}
//			// The query property
//			assertTrue(echo.has("query"));
//			assertTrue(reqParams.get("query").equals(echo.getString("query")));
//			// The method property
//			assertTrue(echo.has("method"));
//			assertTrue(reqParams.get("method").equals(echo.getString("method")));
//			// The remove below property
//			assertTrue(echo.has("removeBelow"));
//			assertEquals(Integer.parseInt(reqParams.get("removeBelow")),echo.getInt("removeBelow"));
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}
	
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
			} catch (DiagnosticException | BadParameterException e) {
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
			} catch (DiagnosticException | BadParameterException e) {
				fail("Wrong exception thrown");
			}
	}

}
