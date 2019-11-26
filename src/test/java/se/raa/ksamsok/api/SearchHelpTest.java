package se.raa.ksamsok.api;

import org.junit.Before;
import org.junit.Test;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.fail;

public class SearchHelpTest extends AbstractBaseTest {
	ByteArrayOutputStream out;
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "searchHelp");
		reqParams.put("index","itemMotiveWord|itemKeyWord");
		reqParams.put("prefix","sto*");
		reqParams.put("maxValueCount","5");

	}

	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testSearchHelpXMLResponse(){
//		try{
//			out = new ByteArrayOutputStream();
//			APIMethod serchHelp = apiMethodFactory.getAPIMethod(reqParams, out);
//			serchHelp.setFormat(Format.XML);
//			serchHelp.performMethod();
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder=null;
//			docBuilder = docFactory.newDocumentBuilder();
//			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
//			Node numberOfTerms = assertBaseDocProp(resultDoc);
//			assertParent(numberOfTerms, "numberOfTerms");
//			Node numberOfTermsValue = numberOfTerms.getFirstChild();
//			assertEquals(Integer.parseInt(reqParams.get("maxValueCount")), Integer.parseInt(assertChild(numberOfTermsValue)));
//			Node terms = numberOfTerms.getNextSibling();
//			assertEquals(Integer.parseInt(reqParams.get("maxValueCount")),terms.getChildNodes().getLength());
//			assertParent(terms, "terms");
//			for(int i = 0; i < terms.getChildNodes().getLength(); i++){
//				Node term = terms.getChildNodes().item(i);
//				assertParent(term, "term");
//				Node value = term.getFirstChild();
//				assertParent(value, "value");
//				Node valueValue = value.getFirstChild();
//				assertTrue(assertChild(valueValue).toLowerCase().contains("sto"));
//				Node count = term.getLastChild();
//				assertTrue(count.getPreviousSibling().equals(value));
//				assertParent(count, "count");
//				Node countValue = count.getFirstChild();
//				assertTrue(Integer.parseInt(assertChild(countValue))>1);
//			}
//		} catch (Exception e){
//			fail(e.getMessage());
//		}
//	}
	// TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testSearchHelpJSONResponse(){
//		try{
//			out = new ByteArrayOutputStream();
//			APIMethod serchHelp = apiMethodFactory.getAPIMethod(reqParams, out);
//			serchHelp.setFormat(Format.JSON_LD);
//			serchHelp.performMethod();
//			JSONObject response = new JSONObject(out.toString("UTF-8"));
//			JSONObject result = assertBaseJSONProp(response);
//			assertTrue(result.has("numberOfTerms"));
//			assertEquals(Integer.parseInt(reqParams.get("maxValueCount")), result.getInt("numberOfTerms"));
//			assertTrue(result.has("terms"));
//			JSONObject terms = result.getJSONObject("terms");
//			assertTrue(terms.has("term"));
//			JSONArray termArray = terms.getJSONArray("term");
//			assertEquals(5,termArray.length());
//			for (int i = 0; i < termArray.length(); i++){
//				JSONObject term = termArray.getJSONObject(i);
//				assertTrue(term.has("value"));
//				assertTrue(term.getString("value").toLowerCase().contains("sto"));
//				assertTrue(term.has("count"));
//				assertTrue(term.getInt("count")>1);
//			}
//		} catch (Exception e){
//			fail(e.getMessage());
//		}
//	}

	@Test
	public void testSearchHelpWithoutIndexParam(){
		reqParams.remove("index");
		out = new ByteArrayOutputStream();
		try {
			APIMethod serchHelp = apiMethodFactory.getAPIMethod(reqParams, out);
			serchHelp.setFormat(Format.XML);
			serchHelp.performMethod();
			fail("No exception was thrown, expected MissingParameterException");
		} catch (MissingParameterException e) {
			// Ignore, correct exception was thrown
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected MissingParameterException: "+e.getMessage());
		}
	}
}
