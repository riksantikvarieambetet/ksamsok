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

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

public class SearchHelpTest extends AbstractBaseTest {
	ByteArrayOutputStream out;
	int numberOfTotalHits;
	int numberOfHits;
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "searchHelp");
		reqParams.put("index","itemMotiveWord|itemKeyWord");
		reqParams.put("prefix","sto*");
		reqParams.put("maxValueCount","5");

	}
	@Test
	public void testSearchHelpXMLResponse(){
		try{
			out = new ByteArrayOutputStream();
			APIMethod serchHelp = apiMethodFactory.getAPIMethod(reqParams, out);
			serchHelp.setFormat(Format.XML);
			serchHelp.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			assertBaseDocProp(resultDoc);
			Element result = resultDoc.getDocumentElement();
			Node numberOfTerms = assertResultAndVersion(result);
			assertParent(numberOfTerms, "numberOfTerms");
			Node numberOfTermsValue = numberOfTerms.getFirstChild();
			assertEquals(Integer.parseInt(reqParams.get("maxValueCount")), Integer.parseInt(assertChild(numberOfTermsValue)));
			Node terms = numberOfTerms.getNextSibling();
			assertEquals(Integer.parseInt(reqParams.get("maxValueCount")),terms.getChildNodes().getLength());
			assertParent(terms, "terms");
			for(int i = 0; i < terms.getChildNodes().getLength(); i++){
				Node term = terms.getChildNodes().item(i);
				assertParent(term, "term");
				Node value = term.getFirstChild();
				assertParent(value, "value");
				Node valueValue = value.getFirstChild();
				assertTrue(assertChild(valueValue).toLowerCase().contains("sto"));
				Node count = term.getLastChild();
				assertTrue(count.getPreviousSibling().equals(value));
				assertParent(count, "count");
				Node countValue = count.getFirstChild();
				assertTrue(Integer.parseInt(assertChild(countValue))>1);
			}
			Node echo = terms.getNextSibling();
			assertTrue(echo.equals(result.getLastChild()));
			assertEcho(echo);
		} catch (Exception e){
			fail(e.getMessage());
		}
		
	}

}
