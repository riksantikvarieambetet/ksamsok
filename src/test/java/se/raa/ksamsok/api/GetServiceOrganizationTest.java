package se.raa.ksamsok.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class GetServiceOrganizationTest extends AbstractBaseTest {
	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "getServiceOrganization");
		reqParams.put("value","all");
	}

	// TODO: Vi har ingen databsakopplin i testerna, det här fallerar
/*
	@Test
	public void testGetServiceOrganizationXMLResponse(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod gerServOrg;
		try {
			gerServOrg = apiMethodFactory.getAPIMethod(reqParams, out);
			gerServOrg.setFormat(Format.XML);
			gerServOrg.performMethod();
			//System.out.println(out.toString("UTF-8"));
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			Node institution = assertBaseDocProp(resultDoc);
			//This is to make sure that we will enter the while-loop
			assertParent(institution,"institution");
			while(institution.getNodeName().equals("institution")){
				assertParent(institution,"institution");
				NodeList instInfoList = institution.getChildNodes();
				assertEquals(14,instInfoList.getLength());
				for (int i = 0; i < instInfoList.getLength(); i++){
					Node instInfo = instInfoList.item(i);
					assertEquals(0,instInfo.getAttributes().getLength());
					assertNull(instInfo.getNodeValue());
					assertEquals(instInfo.getNodeType(), Element.ELEMENT_NODE);
				}
				institution=institution.getNextSibling();
			}
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
*/
	
	@Test
	public void testGetServiceOrganizationJSONResponse(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod gerServOrg;
		try {
			gerServOrg = apiMethodFactory.getAPIMethod(reqParams, out);
			gerServOrg.setFormat(Format.JSON_LD);
			gerServOrg.performMethod();
			//System.out.println(out.toString("UTF-8"));
			JSONObject response = new JSONObject(out.toString("UTF-8"));
			assertBaseJSONProp(response);
		} catch (Exception e){
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
