package se.raa.ksamsok.api.method;

import org.junit.Assert;
import org.junit.Test;
import se.raa.ksamsok.api.AbstractStatisticTest;
import se.raa.ksamsok.api.util.Term;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StatisticTest extends AbstractStatisticTest {
	static int numberOfTermsVal;

	@Test
	public void testGetCartesianCount() {
		HashMap<String, List<Term>> map = new HashMap<>();
		Term term = new Term("foo","bar", 1L );
		// 5
		final ArrayList<Term> terms1 = new ArrayList<>();
		terms1.add(term);
		terms1.add(term);
		terms1.add(term);
		terms1.add(term);
		terms1.add(term);
		//4
		final ArrayList<Term> terms2 = new ArrayList<>();
		terms2.add(term);
		terms2.add(term);
		terms2.add(term);
		terms2.add(term);
		//3
		final ArrayList<Term> terms3 = new ArrayList<>();
		terms3.add(term);
		terms3.add(term);
		terms3.add(term);

		map.put("1", terms1);
		map.put("2", terms2);
		map.put("3", terms3);
		int cc = Statistic.getCartesianCount(map);

		Assert.assertEquals(60, cc);
	}


	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testStatisticXMLResponse(){
//		try {
//			out = new ByteArrayOutputStream();
//			APIMethod statistic = apiMethodFactory.getAPIMethod(reqParams, out);
//			statistic.setFormat(Format.XML);
//			statistic.performMethod();
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder=null;
//			docBuilder = docFactory.newDocumentBuilder();
//			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
//			assertBaseDocProp(resultDoc);
//			// Travel trough the document
//			// The result and version tag
//			Node numberOfTerms = assertResultAndVersion(resultDoc.getDocumentElement());
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
//				assertTrue(reqParams.get("index").contains(assertChild(indexValue)));;
//				// The value tag
//				Node value = index.getNextSibling();
//				assertParent(value,"value");
//				assertTrue(indexFields.getLastChild().equals(value));
//				// The value tag's value
//				Node valueValue = value.getFirstChild();
//				assertTrue(value.getFirstChild().equals(value.getLastChild()));
//				assertChild(valueValue);
//				//*******************************************
//				// The index2 fields tag
//				Node indexFields2 = indexFields.getNextSibling();
//				assertParent(indexFields2,"indexFields");
//				// The index tag
//				Node index2 = indexFields2.getFirstChild();
//				assertParent(index2,"index");
//				//The index value
//				Node indexValue2 = index2.getFirstChild();
//				assertTrue(index2.getFirstChild().equals(index2.getLastChild()));
//				assertTrue(reqParams.get("index").contains(assertChild(indexValue2)));;
//				// The value tag
//				Node value2 = index2.getNextSibling();
//				assertParent(value2,"value");
//				assertTrue(indexFields2.getLastChild().equals(value2));
//				// The value tag's value
//				Node valueValue2 = value.getFirstChild();
//				assertTrue(value2.getFirstChild().equals(value2.getLastChild()));
//				assertChild(valueValue2);
//
//				// The records tag
//				Node records = indexFields2.getNextSibling();
//				assertParent(records,"records");
//				assertTrue(term.getLastChild().equals(records));
//				// The records tags value
//				Node recordsValue = records.getFirstChild();
//				assertTrue(records.getFirstChild().equals(records.getLastChild()));
//				int indexNameRecord = Integer.parseInt(assertChild(recordsValue));
//				assertTrue(indexNameRecord>=Integer.parseInt(reqParams.get("removeBelow")));
//
//				// Assert the echo block
//				assertEcho(resultDoc.getDocumentElement().getLastChild());
//			}
//		}catch (Exception e){
//			fail(e.getMessage());
//		}
//	}

	//TODO: kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
	//	// av om det finns data eller ej genom setup/teardown
//	@Test
//	public void testStatisticJSONResponse(){
//		try {
//			out = new ByteArrayOutputStream();
//			APIMethod statistic = apiMethodFactory.getAPIMethod(reqParams, out);
//			statistic.setFormat(Format.JSON_LD);
//			statistic.performMethod();
//			JSONObject response = new JSONObject(out.toString("UTF-8"));
//			//System.out.println(response.toString(1));
//			JSONObject result = assertBaseJSONProp(response);
//			assertTrue(result.has("term"));
//			JSONArray terms = result.getJSONArray("term");
//			for (int i = 0; i < terms.length(); i++){
//				JSONObject term = terms.getJSONObject(i);
//				assertEquals(2,term.length());
//				assertTrue(term.has("indexFields"));
//				JSONArray indexFields = term.getJSONArray("indexFields");
//				for (int j = 0; j < indexFields.length(); j++){
//					JSONObject indexField = indexFields.getJSONObject(j);
//					assertEquals(2,indexField.length());
//					assertTrue(indexField.has("index"));
//					assertTrue(indexField.has("value"));
//					assertTrue(reqParams.get("index").contains(indexField.getString("index")));
//				}
//				assertTrue(term.has("records"));
//				assertTrue(term.getInt("records") > Integer.parseInt(reqParams.get("removeBelow")));
//			}
//		}catch (Exception e){
//			fail(e.getMessage());
//		}
//	}

}
