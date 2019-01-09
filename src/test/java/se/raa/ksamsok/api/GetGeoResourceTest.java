package se.raa.ksamsok.api;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GetGeoResourceTest extends AbstractBaseTest {

	
	private ByteArrayOutputStream out;
	private APIMethod getGeoResource;
	private DocumentBuilderFactory docFactory;
	private DocumentBuilder docBuilder = null;
	private Document resultDoc;
	private Node rdf;
	private TransformerFactory transformerFactory;
	private Transformer transform;
	private DOMSource source;
	private ByteArrayOutputStream baos;
	private StreamResult strResult;
	private Model m;
	private JSONObject searchResult;
	private JSONObject context;
	private static final String MUNICIPALITY_URI = "http://kulturarvsdata.se/resurser/aukt/geo/municipality#0117";
	private static final String PARISH_URI = "http://kulturarvsdata.se/resurser/aukt/geo/parish#1";
	
	@Before
	public void setUp() throws MalformedURLException {
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "getGeoResource");
	}
	
	@Test
	public void testGetGeoResourceXMLResponse() {
		try {
			//Test RDF response for Municipality
			reqParams.put("uri", MUNICIPALITY_URI);
			out = new ByteArrayOutputStream();
			getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.XML);
			getGeoResource.performMethod();
			
			docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
			resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			
			rdf = resultDoc.getFirstChild();
			transformerFactory = TransformerFactory.newInstance();
			transform = transformerFactory.newTransformer();
			//Create a DOM object of the RDF
			source = new DOMSource(rdf);
			baos = new ByteArrayOutputStream();
			strResult = new StreamResult(baos);
			//Transforms the DOM object to a stream
			transform.transform(source, strResult);
			m = ModelFactory.createDefaultModel();
			//Creates a JENA RDF model from the stream 
			m.read(new ByteArrayInputStream(baos.toByteArray()),"");
			assertFalse(m.isEmpty());
			
			//Test RDF response for Parish
			reqParams.put("uri", PARISH_URI);
			out = new ByteArrayOutputStream();
			getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.XML);
			getGeoResource.performMethod();
			
			docFactory = DocumentBuilderFactory.newInstance();
			docBuilder = docFactory.newDocumentBuilder();
			resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			
			rdf = resultDoc.getFirstChild();
			transformerFactory = TransformerFactory.newInstance();
			transform = transformerFactory.newTransformer();
			//Create a DOM object of the RDF
			source = new DOMSource(rdf);
			baos = new ByteArrayOutputStream();
			strResult = new StreamResult(baos);
			//Transforms the DOM object to a stream
			transform.transform(source, strResult);
			m = ModelFactory.createDefaultModel();
			//Creates a JENA RDF model from the stream 
			m.read(new ByteArrayInputStream(baos.toByteArray()),"");
			//Check that the response is not empty
			assertFalse(m.isEmpty());
			//Check that coordinates is not empty
			StmtIterator iter = m.listStatements();
			while(iter.hasNext()){
				Statement em = iter.next();
				if ((em.getPredicate().toString().contains("coordinates"))) {
					assertTrue(!em.getObject().toString().isEmpty());
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testGetGeoResourceJSONResponse () {
		try {
			//Test JSON response for Municipality
			reqParams.put("uri", MUNICIPALITY_URI);
			out = new ByteArrayOutputStream();
			getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.JSON_LD);
			getGeoResource.performMethod();
			searchResult = new JSONObject(out.toString("UTF-8"));
			assertTrue(searchResult.has("@context"));
			context  = searchResult.getJSONObject("@context");
			assertTrue(context.has("name"));
			assertFalse(context.has("error"));
			
			//Test JSON response for Parish
			reqParams.put("uri", PARISH_URI);
			out = new ByteArrayOutputStream();
			APIMethod getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.JSON_LD);
			getGeoResource.performMethod();
			searchResult = new JSONObject(out.toString("UTF-8"));
			assertTrue(searchResult.has("@context"));
			//Check that coordinates exist
			assertTrue(searchResult.has("ksamsok:coordinates"));
			//Check that values for coordinates is not empty
			assertFalse(searchResult.get("ksamsok:coordinates").toString().isEmpty());
			context  = searchResult.getJSONObject("@context");
			assertTrue(context.has("name"));
			assertFalse(context.has("error"));
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
}
