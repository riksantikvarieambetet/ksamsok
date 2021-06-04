//package se.raa.ksamsok.api;
//
//import org.apache.commons.lang3.StringUtils;
//import org.junit.Before;
//import org.junit.Test;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;
//import org.w3c.dom.ProcessingInstruction;
//import se.raa.ksamsok.api.method.APIMethod;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.net.MalformedURLException;
//import java.net.URI;
//import java.util.HashMap;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//
//public class RSSTest extends AbstractBaseTest {
//
//	@Before
//	public void setUp() throws MalformedURLException{
//		super.setUp();
//		reqParams = new HashMap<>();
//		reqParams.put("method", "rss");
//		reqParams.put("query","item=yxa");
//		reqParams.put("place","gotland");
//		reqParams.put("startRecord", "10");
//		reqParams.put("hitsPerPage","25");
//		reqParams.put("sort","itemName");
//		reqParams.put("sortConfig","asc");
//	}
//
//	//TODO: Kommentera tillbaka när vi har data i indexet igen, eller ännu hellre göra teset oberoende
//	//	// av om det finns data eller ej genom setup/teardown
//	// Kan inte köras just nu pga instabilt testdata
//	@Test
//	public void testRSSSearch(){
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		APIMethod rss;
//		try {
//			rss = apiMethodFactory.getAPIMethod(reqParams, out);
//			rss.performMethod();
//			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
//			DocumentBuilder docBuilder=null;
//			docBuilder = docFactory.newDocumentBuilder();
//			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
//			// Check encoding
//			assertTrue(resultDoc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
//			// Check version
//			assertTrue(resultDoc.getXmlVersion().equalsIgnoreCase("1.0"));
//			// Check stylesheet
//			if (reqParams.containsKey("stylesheet")){
//				ProcessingInstruction styleElement = (ProcessingInstruction) resultDoc.getDocumentElement().getPreviousSibling();
//				assertNotNull(styleElement);
//				assertTrue(styleElement.getData().contains(reqParams.get("stylesheet")));
//			} else {
//				assertNull(resultDoc.getDocumentElement().getPreviousSibling());
//			}
//			// Travel through the document
//			// rss
//			Element rssEl = resultDoc.getDocumentElement();
//			assertTrue(rssEl.getNodeName().equals("rss"));
//			// channel
//			Node channel = rssEl.getFirstChild();
//			assertParent(channel,"channel");
//			assertTrue(channel.equals(rssEl.getLastChild()));
//			// channel childs
//			NodeList channelList = channel.getChildNodes();
//			assertEquals(Integer.parseInt(reqParams.get("hitsPerPage"))+3,channelList.getLength());
//			// title
//			assertParent(channelList.item(0),"title");
//			Node titleValue = channelList.item(0).getFirstChild();
//			assertTrue("titleValue var: " + assertChild(titleValue), "K-samsök sökresultat".equals(assertChild(titleValue)));
//			// link
//			assertParent(channelList.item(1),"link");
//			Node linkValue = channelList.item(1).getFirstChild();
//			assertTrue("http://www.kulturarvsdata.se".equals(assertChild(linkValue)));
//			// Check if it is a valid uri
//			new URI(linkValue.getTextContent());
//			// description
//			assertParent(channelList.item(2),"description");
//			Node descriptionValue = channelList.item(2).getFirstChild();
//			assertTrue("Sökresultat av en sökning mot K-samsök API".equals(assertChild(descriptionValue)));
//			// item
//			for (int i = 3; i < channelList.getLength(); i++){
//				Node item = channelList.item(i);
//				assertParent(item,"item");
//				Node title = item.getFirstChild();
//				assertParent(title,"title");
//				Node titleValueEl = title.getFirstChild();
//				String queryValue = reqParams.get("query").split("=")[1];
//				String assertedChild = assertChild(titleValueEl);
//
//				// ett av sökresultaten är "Yxa", till skillnad från "yxa"...
//				assertedChild = StringUtils.lowerCase(assertedChild);
//				assertTrue("Förväntade mig värde " + queryValue + ", men fick " + assertedChild + ", 	i = " + i, (assertedChild.contains(queryValue)));
//				Node link = title.getNextSibling();
//				assertParent(link,"link");
//				Node linkValueEl = link.getFirstChild();
//				new URI(assertChild(linkValueEl));
//				// The rest nodes are not asserted
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}
//
//}
