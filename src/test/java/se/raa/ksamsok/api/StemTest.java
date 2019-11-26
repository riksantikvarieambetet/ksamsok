package se.raa.ksamsok.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.fail;

public class StemTest extends AbstractBaseTest {
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "stem");
		reqParams.put("words","kappe");
	}


	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testStemXMLResponse(){
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		APIMethod stemMethod;
//		try {
//			stemMethod = apiMethodFactory.getAPIMethod(reqParams, out);
//			stemMethod.setFormat(Format.XML);
//			stemMethod.performMethod();
//			//System.out.println(out.toString("UTF-8"));
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder=null;
//			docBuilder = docFactory.newDocumentBuilder();
//			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
//			Node numberOfStems = assertBaseDocProp(resultDoc);
//			assertParent(numberOfStems,"numberOfStems");
//			int numberOfStemsValue=Integer.parseInt(assertChild(numberOfStems.getFirstChild()));
//			assertTrue(numberOfStemsValue>0);
//			Node stems = numberOfStems.getNextSibling();
//			assertParent(stems,"stems");
//			NodeList stemList = stems.getChildNodes();
//			assertEquals(numberOfStemsValue, stemList.getLength());
//			for (int i = 0; i < numberOfStemsValue; i++){
//				Node stem = stemList.item(i);
//				assertParent(stem,"stem");
//				Node stemValue = stem.getFirstChild();
//				assertTrue(reqParams.get("words").contains(assertChild(stemValue)));
//			}
//		} catch (Exception e){
//			fail(e.getMessage());
//		}
//	}

	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testStemJSONResponse(){
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		APIMethod stemMethod;
//		try {
//			stemMethod = apiMethodFactory.getAPIMethod(reqParams, out);
//			stemMethod.setFormat(Format.JSON_LD);
//			stemMethod.performMethod();
//			//System.out.println(out.toString("UTF-8"));
//			JSONObject response = new JSONObject(out.toString("UTF-8"));
//			assertBaseJSONProp(response);
//		} catch (Exception e){
//			fail(e.getMessage());
//		}
//	}


	@Test
	public void testStemMissingReqParam(){
		reqParams.remove("words");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod stemMethod;
		try {
			stemMethod = apiMethodFactory.getAPIMethod(reqParams, out);
			stemMethod.setFormat(Format.JSON_LD);
			stemMethod.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
			fail("No exception was thrown, expected MissingParameterException");
		} catch (MissingParameterException e){
			//Correct exception was thrown
		} catch (Exception e){
			fail("Wrong exception was thrown, expected MissingParameterException: "+e.toString());
		}
	}


}
