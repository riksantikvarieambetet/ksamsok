package se.raa.ksamsok.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GetRelationsTest extends AbstractBaseTest {

	@Before
	public void setUp() throws MalformedURLException {
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "getRelations");
		reqParams.put("relation", "all");
		reqParams.put("objectId", "raa/fmi/10028201230001");
	}

	@Test
	public void testGetRelationsXMLResponse() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelations;
		try {
			getRelations = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelations.setFormat(Format.XML);
			getRelations.performMethod();
//			System.out.println(out.toString("UTF-8"));
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			Node relations = assertBaseDocProp(resultDoc);
			assertEquals("relations", relations.getNodeName());
			int numberOfRelations = Integer.parseInt(relations.getAttributes().getNamedItem("count").getTextContent());
			NodeList relationList = relations.getChildNodes();
			assertEquals(numberOfRelations, relationList.getLength());
			for (int i = 0; i < numberOfRelations; i++) {
				assertRelation(relationList.item(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void assertRelation(Node relation) throws URISyntaxException {
		assertEquals("relation", relation.getNodeName());
		NamedNodeMap relAttrList = relation.getAttributes();
		assertTrue(relAttrList.getLength() > 0);
		for (int i = 0; i < relAttrList.getLength(); i++) {
			Node relAttr = relAttrList.item(i);
			if (relAttr.getNodeName().equals("type")) {
				assertChild(relAttr.getFirstChild());
			} else if (relAttr.getNodeName().equals("source")) {
				assertEquals("deduced", assertChild(relAttr.getFirstChild()));
			} else {
				fail("Unknown attribute was found in relation tag: " + relAttr.getNodeName());
			}
		}
		// Check if it is an valid uri
		new URI(assertChild(relation.getFirstChild()));
	}

	@Test
	public void testGetRelationsJSONResponse() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelations;
		try {
			getRelations = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelations.setFormat(Format.JSON_LD);
			getRelations.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testGetRelationsMissingReqParam() {
		reqParams.remove("relation");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelations;
		try {
			getRelations = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelations.setFormat(Format.JSON_LD);
			getRelations.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
			fail("No exception was thrown, expected MissingParameterException");
		} catch (MissingParameterException e) {
			//Correct exception was thrown
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected MissingParameterException: " + e.getCause().toString());
		}
	}

	@Test
	public void testGetRelationsInvalidReqParam() {
		reqParams.remove("relation");
		reqParams.put("relation", "asklödfjlö");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod getRelations;
		try {
			getRelations = apiMethodFactory.getAPIMethod(reqParams, out);
			getRelations.setFormat(Format.JSON_LD);
			getRelations.performMethod();
			System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
			fail("No exception was thrown, expected BadParameterException");
		} catch (BadParameterException e) {
			//Correct exception was thrown
		} catch (Exception e) {
			fail("Wrong exception was thrown, expected BadParameterException: " + e.getCause().toString());
		}
	}

}
