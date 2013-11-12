package se.raa.ksamsok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;


import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.eclipse.jetty.util.resource.FileResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import com.github.jsonldjava.jena.JenaJSONLD;

import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.Facet;
import se.raa.ksamsok.api.method.APIMethod.Format;
import se.raa.ksamsok.api.method.Search;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.solr.SearchServiceImpl;

public class FacetTest {
	
	HashMap<String, String> reqParams;
	APIMethodFactory apiMethodFactory;
	ByteArrayOutputStream out;
	@Before

	public void setUp() throws DiagnosticException, MalformedURLException{
		SolrServer solr = new CommonsHttpSolrServer("http://lx-ra-ksamtest1:8080/solr");
		SearchServiceImpl searchService = new SearchServiceImpl();
		ReflectionTestUtils.setField(searchService,"solr", solr);
		apiMethodFactory = new APIMethodFactory();
		ReflectionTestUtils.setField(apiMethodFactory,"searchService", searchService);
		JenaJSONLD.init();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "facet");
		reqParams.put("stylesheet", "stylesheet/facet.xsl");
		reqParams.put("index", "countyName|thumbnailExists");
		reqParams.put("query","hus");
		reqParams.put("removeBelow","1");
	}
	
	@Test
	public void testFacet(){
		
		
		try {
			out = new ByteArrayOutputStream();
			APIMethod facet = apiMethodFactory.getAPIMethod(reqParams, out);
			facet.setFormat(Format.XML);
			facet.performMethod();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder=null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			// Check encoding
			assertTrue(resultDoc.getXmlEncoding().equalsIgnoreCase("UTF-8"));
			// Check version
			assertTrue(resultDoc.getXmlVersion().equalsIgnoreCase("1.0"));
			// Check stylesheet
			ProcessingInstruction styleElement = (ProcessingInstruction) resultDoc.getDocumentElement().getPreviousSibling();
			assertNotNull(styleElement);
			assertTrue(styleElement.getData().contains("stylesheet/facet.xsl"));
			// Travel trough the document
			// The result tag
			Element result = resultDoc.getDocumentElement();
			assertTrue(result.getNodeName().equals("result"));
			assertEquals(0,result.getAttributes().getLength());
			assertNull(result.getNodeValue());
			assertTrue(result.getNodeType()==Node.ELEMENT_NODE);
			// The version tag
			Node version = result.getFirstChild();
			assertTrue(version.getNodeName().equals("version"));
			assertEquals(0,version.getAttributes().getLength());
			assertNull(version.getNodeValue());
			assertTrue(version.getNodeType()==Node.ELEMENT_NODE);
			// The version value
			Node versionValue = version.getFirstChild();
			assertTrue(versionValue.getNodeType()==Node.TEXT_NODE);
			assertEquals(Float.parseFloat(APIMethod.API_VERSION),Float.parseFloat(versionValue.getNodeValue()),0);
			assertTrue(version.getFirstChild().equals(version.getLastChild()));
			assertNull(versionValue.getFirstChild());
			// Number of terms
			Node numberOfTerms = version.getNextSibling();
			assertTrue(numberOfTerms.getNodeName().equals("numberOfTerms"));
			assertTrue(numberOfTerms.getNodeType()==Node.ELEMENT_NODE);
			assertNull(numberOfTerms.getNodeValue());
			assertEquals(0,numberOfTerms.getAttributes().getLength());
			// Number of terms value
			Node numberOfTermsValue = numberOfTerms.getFirstChild();
			int numberOfTermsVal = Integer.parseInt(numberOfTermsValue.getNodeValue(), 10);
			assertTrue(numberOfTermsVal>1);
			assertTrue(numberOfTermsValue.getNodeType()==Node.TEXT_NODE);
			assertTrue(numberOfTerms.getFirstChild().equals(numberOfTerms.getLastChild()));
			assertNull(numberOfTermsValue.getFirstChild());
			// The term tags
			Node term=numberOfTerms;
			for (int i=0; i < numberOfTermsVal; i++){
				term=term.getNextSibling();
				assertTrue(term.getNodeName().equals("term"));
				assertEquals(0,term.getAttributes().getLength());
				assertNull(term.getNodeValue());
				assertTrue(term.getNodeType()==Node.ELEMENT_NODE);
				// The index fields tag
				Node indexFields = term.getFirstChild();
				assertTrue(indexFields.getNodeName().equals("indexFields"));
				assertEquals(0,indexFields.getAttributes().getLength());
				assertNull(indexFields.getNodeValue());
				assertTrue(indexFields.getNodeType()==Node.ELEMENT_NODE);
				// The index tag
				Node index = indexFields.getFirstChild();
				assertTrue(index.getNodeName().equals("index"));
				assertEquals(0,index.getAttributes().getLength());
				assertNull(index.getNodeValue());
				assertTrue(index.getNodeType()==Element.ELEMENT_NODE);
				//The index value
				Node indexValue = index.getFirstChild();
				assertTrue(indexValue.getNodeType()==Node.TEXT_NODE);
				assertTrue(index.getFirstChild().equals(index.getLastChild()));
				assertNull(indexValue.getFirstChild());
				String indexName = indexValue.getNodeValue();
				// The value tag
				Node value = index.getNextSibling();
				assertTrue(value.getNodeName().equals("value"));
				assertEquals(0, value.getAttributes().getLength());
				assertTrue(value.getNodeType()==Node.ELEMENT_NODE);
				assertTrue(indexFields.getLastChild().equals(value));
				// The value tag's value
				Node valueValue = value.getFirstChild();
				assertTrue(valueValue.getNodeType()==Node.TEXT_NODE);
				assertTrue(value.getFirstChild().equals(value.getLastChild()));
				assertNull(valueValue.getFirstChild());
				String indexNameValue = valueValue.getNodeValue();
				// The records tag
				Node records = indexFields.getNextSibling();
				assertTrue(records.getNodeName().equals("records"));
				assertEquals(0, records.getAttributes().getLength());
				assertNull(records.getNodeValue());
				assertTrue(records.getNodeType()==Node.ELEMENT_NODE);
				assertTrue(term.getLastChild().equals(records));
				
				
				
				}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
