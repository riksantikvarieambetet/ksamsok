package se.raa.ksamsok.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.APIMethod.Format;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class GetGeoResourceTest extends AbstractBaseTest {

	
	private ByteArrayOutputStream out;
	private static final String MUNICIPALITY_URI = "http://kulturarvsdata.se/resurser/aukt/geo/municipality#0117";
	
	@Before
	public void setUp() throws MalformedURLException {
		super.setUp();
		reqParams = new HashMap<String,String>();
		reqParams.put("method", "getGeoResource");
	}
	
	@Test
	public void testGetGeoResourceXMLResponse() {
		try {
			reqParams.put("uri", MUNICIPALITY_URI);
			out = new ByteArrayOutputStream();
			APIMethod getGeoResource = apiMethodFactory.getAPIMethod(reqParams, out);
			getGeoResource.setFormat(Format.XML);
			getGeoResource.performMethod();
			
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = null;
			docBuilder = docFactory.newDocumentBuilder();
			Document resultDoc = docBuilder.parse(new ByteArrayInputStream(out.toByteArray()));
			
			Node rdf = resultDoc.getFirstChild();
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transform = transformerFactory.newTransformer();
			//Create a DOM object of the RDF
			DOMSource source = new DOMSource(rdf);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamResult strResult = new StreamResult(baos);
			//Transforms the DOM object to a stream
			transform.transform(source, strResult);
			Model m = ModelFactory.createDefaultModel();
			//Creates a JENA RDF model from the stream 
			m.read(new ByteArrayInputStream(baos.toByteArray()),"");
			// Assert that the model is not empty
			assertFalse(m.isEmpty());
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
