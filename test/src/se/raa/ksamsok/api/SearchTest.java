package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;


public class SearchTest extends AbstractBaseTest{
	ByteArrayOutputStream out;
	int numberOfTotalHits;
	int numberOfHits;
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "search");
		reqParams.put("query","text=yxa");
	}
	
	@Test
	public void testSearchWithRecordSchemaXMLResponse(){
		reqParams.put("recordSchema","xml");
		reqParams.put("fields","itemLabel,itemDescription,thumbnail,url");
		try{
			out = new ByteArrayOutputStream();
			APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
			search.setFormat(Format.XML);
			search.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
			// Travel trough the document
			// Result, version and totalHits
			Node totalHits = assertResultAndVersion(resultDoc.getDocumentElement());
			assertParent(totalHits,"totalHits");
			// total hits value
			Node totalHitsValue = totalHits.getFirstChild();
			if (numberOfTotalHits>0){
				// Compare with the json result if it has been running
				assertEquals(numberOfTotalHits,Integer.parseInt(assertChild(totalHitsValue)));
			} else {
				numberOfTotalHits = Integer.parseInt(assertChild(totalHitsValue));
				assertTrue(numberOfTotalHits>1);
			}
			// Records
			Node records = totalHits.getNextSibling();
			assertParent(records,"records");
			// Echo 
			Node echo = records.getNextSibling();
			assertParent(echo,"echo");
			// StartRecord
			Node startRecord = echo.getFirstChild();
			assertParent(startRecord,"startRecord");
			// Start record value
			Node startRecordValue = startRecord.getFirstChild();
			assertEquals(1,Integer.parseInt(assertChild(startRecordValue)));
			// hitsPerPage
			Node hitsPerPage = startRecord.getNextSibling();
			assertParent(hitsPerPage,"hitsPerPage");
			// hitsPerPage value
			Node hitsPerPageValue = hitsPerPage.getFirstChild();
			if (numberOfHits>0){
				// Compare with the json result if it has been running
				assertEquals(numberOfHits, Integer.parseInt(assertChild(hitsPerPageValue)));
			} else {
				numberOfHits=Integer.parseInt(assertChild(hitsPerPageValue));
				assertTrue(numberOfHits>1);
			}
			// query
			Node query = hitsPerPage.getNextSibling();
			assertParent(query, "query");
			assertTrue(echo.getLastChild().equals(query));
			// query value
			Node queryValue = query.getFirstChild();
			assertTrue(reqParams.get("query").equals(assertChild(queryValue)));
			// The record
			NodeList recordList = records.getChildNodes();
			assertEquals(numberOfHits,recordList.getLength());
			for (int i = 0; i < recordList.getLength(); i++){
				Node record = recordList.item(i);
				NodeList fieldList = record.getChildNodes();
				assertTrue(fieldList.getLength()>1);
				// -1 because rel:score is the last node
				for(int j = 0; j < fieldList.getLength()-1;j++){
					Node field = fieldList.item(j);
					assertField(field);
				}
				Node relScore = record.getLastChild();
				assertRelScore(relScore);
			}
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	/**
	 * This method asserts the <field> tag
	 * @param node - The <field> node
	 */
	private void assertField(Node node) {
		assertTrue(node.getNodeName().equals("field"));
		assertEquals(1,node.getAttributes().getLength());
		assertTrue(node.getAttributes().item(0).getNodeName().equals("name"));
		String fieldName=node.getAttributes().item(0).getNodeValue();
		Node fieldValue = node.getFirstChild();
		switch (fieldName){
		case "itemId" :
		case "thumbnail" :
		case "url" :
			try {
				new URI(assertChild(fieldValue));
			} catch (URISyntaxException e) {
				fail("Non valid url: "+fieldValue);
			}
			break;
		case "itemLabel" :
		case "itemDescription" :
			assertChild(fieldValue);
			break;
		default :
			fail("Found a field value that should not be here: " +fieldName);
			break;
		}
	}

	@Test
	public void testSearchWithRecordSchemaPresentationResponse(){
		reqParams.put("recordSchema","presentation");
		try{
			out = new ByteArrayOutputStream();
			APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
			search.setFormat(Format.XML);
			search.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testSearchWithRecordSchemaRDFResponse(){
		reqParams.put("recordSchema","RDF");
		try{
			out = new ByteArrayOutputStream();
			APIMethod search = apiMethodFactory.getAPIMethod(reqParams, out);
			search.setFormat(Format.XML);
			search.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
