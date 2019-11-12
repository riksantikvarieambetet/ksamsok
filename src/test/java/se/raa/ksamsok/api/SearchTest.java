package se.raa.ksamsok.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class SearchTest extends AbstractBaseTest{
	ByteArrayOutputStream out;
	int numberOfTotalHits;
	int numberOfHits;
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "search");
		reqParams.put("query","text=yxa");
	}

	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
	// Kan inte köras just nu pga instabilt testdata
//	@Test
//	public void testSearchWithRecordSchemaXMLResponse(){
//		reqParams.put("fields","itemId,itemLabel,itemDescription,thumbnail,url");
//		reqParams.put("recordSchema","xml");
//		try{
//			// Assert the base search document structure
//			NodeList recordList = assertBaseSearchDocument(Format.XML);
//			for (int i = 0; i < recordList.getLength(); i++){
//				Node record = recordList.item(i);
//				NodeList fieldList = record.getChildNodes();
//				assertTrue(fieldList.getLength()>1);
//				// -1 because rel:score is the last node
//				for(int j = 0; j < fieldList.getLength()-1;j++){
//					Node field = fieldList.item(j);
//					assertField(field);
//				}
//				Node relScore = record.getLastChild();
//				assertRelScore(relScore);
//			}
//		} catch (Exception e){
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//
//	}

	@Test
	public void testSearchWithRecordSchemaXMLJSONResponse(){
		reqParams.put("fields","itemId,itemLabel,itemDescription,thumbnail,url");
		reqParams.put("recordSchema","xml");
		try{
			assertBaseSearchJSON();
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchWithJSONResponse(){
		reqParams.remove("recordSchema");
		try{
			assertBaseSearchJSON();
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
	@Test
	public void testSearchWithRecordSchemaPresResponse(){
		reqParams.put("recordSchema","presentation");
		try{
			// Assert the base search document structure
			NodeList recordList = assertBaseSearchDocument(Format.XML);
			for (int i = 0; i < recordList.getLength(); i++){
				assertEquals(2, recordList.item(i).getChildNodes().getLength());
				Node pres = recordList.item(i).getFirstChild();
				assertEquals("pres:item", pres.getNodeName());
				assertNotNull(pres.getFirstChild());
				//rel:score
				Node relScore = recordList.item(i).getLastChild();
				assertRelScore(relScore);
			}
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchWithRecordSchemaPresJSONResponse(){
		reqParams.put("recordSchema","presentation");
		try{
			assertBaseSearchJSON();
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

   // TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
   //	// av om det finns data eller ej genom setup/teardown
	// kan inte köras just nu pga instabilt testdata
//	@Test
//	public void testSearchWithRecordSchemaRDFResponse(){
//		reqParams.put("recordSchema","rdf");
//		try{
//			NodeList recordList= assertBaseSearchDocument(Format.RDF);
//			for (int i = 0; i < recordList.getLength(); i++){
//				assertEquals(2, recordList.item(i).getChildNodes().getLength());
//				//Try to creat an model from the embedded rdf
//				Node rdf = recordList.item(i).getFirstChild();
//				TransformerFactory transformerFactory = TransformerFactory.newInstance();
//				Transformer transform = transformerFactory.newTransformer();
//				DOMSource source = new DOMSource(rdf);
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				StreamResult strResult = new StreamResult(baos);
//				transform.transform(source, strResult);
//				Model m = ModelFactory.createDefaultModel();
//				m.read(new ByteArrayInputStream(baos.toByteArray()),"");
//				// Assert that the model is not empty
//				assertFalse(m.isEmpty());
//				//rel:score
//				Node relScore = recordList.item(i).getLastChild();
//				assertRelScore(relScore);
//			}
//		} catch (Exception e){
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}


	@Test
	public void testSearchWithUnknownRecordSchema(){
		reqParams.put("recordSchema","");
		try {
			assertBaseSearchDocument(Format.XML);
			fail("No exception thrown, expected BadParameterException");
		} catch (BadParameterException e) {
			//Ignore. This exception is expected
		} catch (Exception e) {
			fail("Wrong exception thrown, expected BadParameterException");
		}
	}

	/**
	 * This method asserts the <field> tag
	 * @param node - The <field> node
	 */
	private void assertField(Node node) {
		assertEquals("field", node.getNodeName());
		assertEquals(1,node.getAttributes().getLength());
		assertEquals("name", node.getAttributes().item(0).getNodeName());
		String fieldName=node.getAttributes().item(0).getNodeValue();
		Node fieldValue = node.getFirstChild();
		if (fieldName.equals("itemId") || fieldName.equals("thumbnail") || fieldName.equals("url")){
			try {
				new URI(assertChild(fieldValue));
			} catch (URISyntaxException e) {
				fail("Non valid url: "+fieldValue);
			}
		} else if (fieldName.equals("itemLabel") || fieldName.equals("itemDescription")){
			assertChild(fieldValue);
		} else {
			fail("Found a field value that should not be here: " +fieldName);
		}
	}

	/**
	 * This method makes the search request and assert the base document structure of the search result
	 * @param format
	 * @return
	 * @throws MissingParameterException
	 * @throws DiagnosticException
	 * @throws BadParameterException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	private NodeList assertBaseSearchDocument(Format format) throws MissingParameterException, DiagnosticException, BadParameterException, ParserConfigurationException, SAXException, IOException{
		out = new ByteArrayOutputStream();
		APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
		search.setFormat(format);
		search.performMethod();
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = docFactory.newDocumentBuilder();
		Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		//System.out.println(out.toString("UTF-8"));
		// Travel trough the document
		// Result, version and totalHits
		Node totalHits = assertBaseDocProp(resultDoc);
		assertParent(totalHits,"totalHits");
		// total hits value
		Node totalHitsValue = totalHits.getFirstChild();
		if (numberOfTotalHits>0){
			// Compare with the previous result if other test has been running
			assertEquals(numberOfTotalHits,Integer.parseInt(assertChild(totalHitsValue)));
		} else {
			numberOfTotalHits = Integer.parseInt(assertChild(totalHitsValue));
			assertTrue(numberOfTotalHits>1);
		}
		// Records
		Node records = totalHits.getNextSibling();
		assertParent(records,"records");
		// The record
		return records.getChildNodes();
	}
	
	/**
	 * This method makes the search request and assert the base json structure of the search result
	 * @throws UnsupportedEncodingException
	 * @throws JSONException
	 * @throws MissingParameterException
	 * @throws DiagnosticException
	 * @throws BadParameterException
	 */
	private void assertBaseSearchJSON() throws UnsupportedEncodingException, JSONException, MissingParameterException, DiagnosticException, BadParameterException{
		out = new ByteArrayOutputStream();
		APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
		search.setFormat(Format.JSON_LD);
		search.performMethod();
		JSONObject searchResult = new JSONObject(out.toString("UTF-8"));
		assertTrue(searchResult.has("result"));
		JSONObject result  = searchResult.getJSONObject("result");
		assertFalse(result.has("error"));
	}
}
