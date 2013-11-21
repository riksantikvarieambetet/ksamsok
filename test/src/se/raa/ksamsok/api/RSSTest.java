package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

import se.raa.ksamsok.api.method.APIMethod;

public class RSSTest extends AbstractBaseTest {

	@Before
	public void setUp() throws MalformedURLException{
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "rss");
		reqParams.put("query","item=yxa");
		reqParams.put("place","gotland");
		reqParams.put("startRecord", "10");
		reqParams.put("hitsPerPage","25");
		reqParams.put("sort","itemName");
		reqParams.put("sortConfig","asc");
		// Setting pretty print will make the junit test fail because /n will be extra nodes in the DOM
		//reqParams.put("prettyPrint","true");
	}

	@Test
	public void testRSSSearch(){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		APIMethod rss;
		try {
			rss = apiMethodFactory.getAPIMethod(reqParams, out);
			rss.performMethod();
			//System.out.println(out.toString("UTF-8"));
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			// Check encoding
			assertTrue(resultDoc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
			// Check version
			assertTrue(resultDoc.getXmlVersion().equalsIgnoreCase("1.0"));
			// Check stylesheet
			if (reqParams.containsKey("stylesheet")){
				ProcessingInstruction styleElement = (ProcessingInstruction) resultDoc.getDocumentElement().getPreviousSibling();
				assertNotNull(styleElement);
				assertTrue(styleElement.getData().contains(reqParams.get("stylesheet")));
			} else {
				assertNull(resultDoc.getDocumentElement().getPreviousSibling());
			}
			// Travel through the document
			// rss
			Element rssEl = resultDoc.getDocumentElement();
			assertTrue(rssEl.getNodeName().equals("rss"));
			// channel
			Node channel = rssEl.getFirstChild();
			assertParent(channel,"channel");
			assertTrue(channel.equals(rssEl.getLastChild()));
			// channel childs
			NodeList channelList = channel.getChildNodes();
			assertEquals(Integer.parseInt(reqParams.get("hitsPerPage"))+3,channelList.getLength());
			// title
			assertParent(channelList.item(0),"title");
			Node titleValue = channelList.item(0).getFirstChild();
			assertTrue("K-samsök sökresultat".equals(assertChild(titleValue)));
			// link
			assertParent(channelList.item(1),"link");
			Node linkValue = channelList.item(1).getFirstChild();
			assertTrue("http://www.kulturarvsdata.se".equals(assertChild(linkValue)));
			// Check if it is a valid uri
			new URI(linkValue.getTextContent());
			// description
			assertParent(channelList.item(2),"description");
			Node descriptionValue = channelList.item(2).getFirstChild();
			assertTrue("Sökresultat av en sökning mot K-samsök API".equals(assertChild(descriptionValue)));
			// item
			for (int i = 3; i < channelList.getLength(); i++){
				Node item = channelList.item(i);
				assertParent(item,"item");
				Node title = item.getFirstChild();
				assertParent(title,"title");
				Node titleValueEl = title.getFirstChild();
				assertTrue(reqParams.get("query").contains(assertChild(titleValueEl)));
				Node link = title.getNextSibling();
				assertParent(link,"link");
				Node linkValueEl = link.getFirstChild();
				new URI(assertChild(linkValueEl));
				// The rest nodes are not asserted
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
}
