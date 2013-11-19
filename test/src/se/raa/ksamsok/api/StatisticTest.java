package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

public class StatisticTest extends AbstractBaseTest {
	ByteArrayOutputStream out;
	static int numberOfTermsVal;
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "statistic");
		reqParams.put("index", "mediaType=*|itemName=yxa*");
		reqParams.put("removeBelow","10");
	}

	@Test
	public void testFacetXMLResponse(){
		try {
			out = new ByteArrayOutputStream();
			APIMethod statistic = apiMethodFactory.getAPIMethod(reqParams, out);
			statistic.setFormat(Format.XML);
			statistic.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
			// Travel trough the document
			// The result and version tag
			Node numberOfTerms = assertResultAndVersion(resultDoc.getDocumentElement());
			// Number of terms
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
				assertTrue(reqParams.get("index").contains(assertChild(indexValue)));;
				// The value tag
				Node value = index.getNextSibling();
				assertParent(value,"value");
				assertTrue(indexFields.getLastChild().equals(value));
				// The value tag's value
				Node valueValue = value.getFirstChild();
				assertTrue(value.getFirstChild().equals(value.getLastChild()));
				assertChild(valueValue);
				//*******************************************
				// The index2 fields tag
				Node indexFields2 = indexFields.getNextSibling();
				assertParent(indexFields2,"indexFields");
				// The index tag
				Node index2 = indexFields2.getFirstChild();
				assertParent(index2,"index");
				//The index value
				Node indexValue2 = index2.getFirstChild();
				assertTrue(index2.getFirstChild().equals(index2.getLastChild()));
				assertTrue(reqParams.get("index").contains(assertChild(indexValue2)));;
				// The value tag
				Node value2 = index2.getNextSibling();
				assertParent(value2,"value");
				assertTrue(indexFields2.getLastChild().equals(value2));
				// The value tag's value
				Node valueValue2 = value.getFirstChild();
				assertTrue(value2.getFirstChild().equals(value2.getLastChild()));
				assertChild(valueValue2);

				// The records tag
				Node records = indexFields2.getNextSibling();
				assertParent(records,"records");
				assertTrue(term.getLastChild().equals(records));
				// The records tags value
				Node recordsValue = records.getFirstChild();
				assertTrue(records.getFirstChild().equals(records.getLastChild()));
				int indexNameRecord = Integer.parseInt(assertChild(recordsValue));
				assertTrue(indexNameRecord>=Integer.parseInt(reqParams.get("removeBelow")));
				
				// Assert the echo block
				assertEcho(resultDoc.getDocumentElement().getLastChild());
			} 
		}catch (Exception e){
			fail(e.getMessage());
		}
	}
}