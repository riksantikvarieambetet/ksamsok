package se.raa.ksamsok.api;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.organization.OrganizationManager;
import se.raa.ksamsok.solr.SearchServiceImpl;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("file:src/test/resources/testContext.xml")
abstract public class AbstractBaseTest {

	@Resource
	private DataSource dataSource;
	static APIMethodFactory apiMethodFactory;
	HashMap<String, String> reqParams;
	@Autowired
	private String COMMON_SOLR_SERVER;
	
	public void setUp() throws MalformedURLException{
		if (apiMethodFactory == null){
			SolrClient solr = new Http2SolrClient.Builder(COMMON_SOLR_SERVER).build();
			SearchServiceImpl searchService = new SearchServiceImpl();
			// The solr is @Autowired in the project. It is necessary to set up it by hand in the test cases
			ReflectionTestUtils.setField(searchService,"solr", solr);
			apiMethodFactory = new APIMethodFactory();
			// The searchService is @Autowired in the project. It is necessary to set up it by hand in the test cases
			ReflectionTestUtils.setField(apiMethodFactory,"searchService", searchService);
			// The organizationManager is @Autowired in the project.It is necessary to set up it by hand in the test cases
			OrganizationManager organizationManager = new OrganizationManager(dataSource);
			ReflectionTestUtils.setField(apiMethodFactory,"organizationManager", organizationManager);
			//Wire a database connection right here, made available for use in classes extending the AbstractBaseTest.
			ReflectionTestUtils.setField(apiMethodFactory,"dataSource", dataSource);
		}
	}


	/**
	 * This method assert the base properties of the xml document like verions, encoding, stylesheet and the result, version and echo tag
	 * @param doc - The document to assert
	 * @return - Returns the result tag
	 */
	protected Node assertBaseDocProp(Document doc) {
		// Check encoding
		assertTrue(doc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
		// Check version
		assertTrue(doc.getXmlVersion().equalsIgnoreCase("1.0"));
		// Check stylesheet
		if (reqParams.containsKey("stylesheet")){
			ProcessingInstruction styleElement = (ProcessingInstruction) doc.getDocumentElement().getPreviousSibling();
			assertNotNull(styleElement);
			assertTrue(styleElement.getData().contains(reqParams.get("stylesheet")));
		} else {
			assertNull(doc.getDocumentElement().getPreviousSibling());
		}
		assertEcho(doc.getDocumentElement().getLastChild());
		return assertResultAndVersion(doc.getDocumentElement());
	}

	/**
	 * This method asserts a child node, i.e. a node without any childs, and returns the node's value
	 * @param node - The node to assert
	 * @return - A string with the node's value
	 */
	protected String assertChild(Node node) {
		assertEquals(node.getNodeType(), Node.TEXT_NODE);
		assertNull(node.getFirstChild());		
		return node.getNodeValue();
	}

	/**
	 * This method asserts a parent node, i.e. a node without value but with at least one child.
	 * @param node - The node to assert
	 * @param nodeName - The name it should have
	 */
	protected void assertParent(Node node, String nodeName) {
		assertEquals(node.getNodeName(), nodeName);
		assertEquals(0,node.getAttributes().getLength());
		assertNull(node.getNodeValue());
		assertEquals(node.getNodeType(), Element.ELEMENT_NODE);
		assertTrue(node.getChildNodes().getLength()>0);
	}

	/**
	 * This method assert the result and version tag in the response and returns the sibling to the version tag
	 * @param result - the document element
	 * @return - the first sibling to the version tag
	 */
	protected Node assertResultAndVersion(Element result) {
		// The result tag
		assertParent(result,"result");
		// The version tag
		Node version = result.getFirstChild();
		assertParent(version,"version");
		// The version value
		Node versionValue = version.getFirstChild();
		assertEquals(Float.parseFloat(APIMethod.API_VERSION),Float.parseFloat(assertChild(versionValue)),0);
		assertEquals(version.getFirstChild(), version.getLastChild());
		return version.getNextSibling();
	}

	/**
	 * This method asserts the <rel:score> tag
	 * @param node - The <rel:score> node
	 */
	protected void assertRelScore(Node node) {
		assertEquals("rel:score", node.getNodeName());
		// Check rel:score namespace
		assertEquals(1,node.getAttributes().getLength());
		assertEquals("xmlns:rel", node.getAttributes().item(0).getNodeName());
		assertEquals(node.getAttributes().item(0).getNodeType(), Node.ATTRIBUTE_NODE);
		String nameSpace = assertChild(node.getAttributes().item(0).getFirstChild());
		assertEquals("info:srw/extension/2/relevancy-1.0", nameSpace);
		// The first child is the attribute node
		Node relScoreValue = node.getLastChild();
		assertTrue(Float.parseFloat(assertChild(relScoreValue))>0);
	}
	
	/**
	 * This method asserts the echo block
	 * @param echo - The echo node
	 */
	protected void assertEcho(Node echo){
		// Check that the echo block equals that input parameters
		NodeList echoNodes = echo.getChildNodes();
		for (int i = 0; i < echoNodes.getLength(); i++){
			Node echoChildNode = echoNodes.item(i);
			if(reqParams.containsKey(echoChildNode.getNodeName())){
				assertParent(echoChildNode, echoChildNode.getNodeName());
				Node echoChildNodeValue = echoChildNode.getFirstChild();
				assertTrue(reqParams.get(echoChildNode.getNodeName()).contains(assertChild(echoChildNodeValue)));
			}
		}
		// Check that all parameters are in the echo block
		for (String nodeName : reqParams.keySet()) {
			if (!nodeName.equals("stylesheet")) {
				boolean found = false;
				for (int i = 0; i < echoNodes.getLength(); i++) {
					Node echoChildNode = echoNodes.item(i);
					if (echoChildNode.getNodeName().equals(nodeName)) {
						found = true;
						break;
					}
				}
				if (!found) {
					fail(nodeName + " not found in echo node");
				}
			}
		}
	}

	/**
	 * This method asserts the echo block
	 * @param echo
	 * @throws JSONException
	 */
	protected void assertEcho(JSONObject echo){
		// Check that the echo block equals that input parameters
		@SuppressWarnings("rawtypes")
		Iterator keys = echo.keys();
		while (keys.hasNext()){
			String key = (String) keys.next();
			if (reqParams.containsKey(key)) {
				try {
					assertTrue(reqParams.get(key).contains(echo.getString(key)));
				} catch (JSONException e) {
					//The key is not a string it can also be an array
					JSONArray echoArray;
					try {
						echoArray = echo.getJSONArray(key);
						for (int i = 0; i < echoArray.length(); i++){
							assertTrue(reqParams.get(key).contains(echoArray.getString(i)));
						}
					} catch (JSONException e1) {
						// The key can also be a integer
						try {
							assertTrue(reqParams.get(key).contains(Integer.toString(echo.getInt(key))));
						} catch (JSONException e2) {
							fail("Unhandled json type: "+e2.getMessage());
						}
					}
				}	
			}
		}
		// Check that all parameters are in the echo block
		for (String keyName : reqParams.keySet()) {
			assertTrue(echo.has(keyName));
		}
	}
	/**
	 * This method assert the base json response properties like result, version and echo
	 * @param response - The response JSONObject
	 * @return result - The result JSONObject
	 * @throws JSONException
	 */
	protected JSONObject assertBaseJSONProp(JSONObject response) throws JSONException {
		assertEquals(1,response.length());
		// The result 
		assertTrue(response.has("result"));
		JSONObject result = response.getJSONObject("result");
		// Version
		assertTrue(result.has("version"));
		assertEquals(Double.parseDouble(APIMethod.API_VERSION),result.getDouble("version"),0);
		// Echo
		assertTrue(result.has("echo"));
		assertEcho(result.getJSONObject("echo"));
		return result;
	}
}
