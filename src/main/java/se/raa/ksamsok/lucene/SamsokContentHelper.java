package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.raa.ksamsok.harvest.ExtractedInfo;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.spatial.GMLUtil;

import javax.vecmath.Point2d;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;

import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uriPrefix;

public class SamsokContentHelper extends ContentHelper {

	private static final Logger logger = LogManager.getLogger(SamsokContentHelper.class);

	private static DocumentBuilderFactory xmlFact;
	private static TransformerFactory xformerFact;

	static {
		xmlFact = DocumentBuilderFactory.newInstance();
	    xmlFact.setNamespaceAware(true);
	    xformerFact = TransformerFactory.newInstance();
	}

	@Override
	public SolrInputDocument createSolrDocument(HarvestService service,
			String xmlContent, Date added) throws Exception {
		Model model = null;
		String identifier = null;
		SolrInputDocument luceneDoc = null;
		try {
			model = RDFUtil.parseModel(xmlContent);

			// grund
			Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
			Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
			Property rPres = ResourceFactory.createProperty(SamsokProtocol.uri_rPres.toString());
			// url
			Property rURL = ResourceFactory.createProperty(SamsokProtocol.uri_rURL.toString());
			Property rMuseumdatURL = ResourceFactory.createProperty(SamsokProtocol.uri_rMuseumdatURL.toString());
			// special
			Property rItemForIndexing = ResourceFactory.createProperty(SamsokProtocol.uri_rItemForIndexing.toString());
			Property rProtocolVersion = ResourceFactory.createProperty(SamsokProtocol.uri_rKsamsokVersion.toString());

			Resource subject = null;
			Selector selector = new SimpleSelector((Resource) null, rdfType, samsokEntity);
			StmtIterator iter = model.listStatements(selector);
			while (iter.hasNext()){
				if (subject != null) {
					throw new Exception("Ska bara finnas en entity i rdf-grafen");
				}
				subject = iter.next().getSubject();
			}
			if (subject == null) {
				logger.error("Hittade ingen entity i rdf-grafen:\n" + model);
				throw new Exception("Hittade ingen entity i rdf-grafen");
			}
			identifier = subject.toString();
			// kolla om denna post inte ska indexeras och returnera i så fall null
			// notera att detta gör att inte posten indexeras alls vilket kräver ett
			// specialfall i resolverservleten då den främst jobbar mot lucene-indexet
			String itemForIndexing = RDFUtil.extractSingleValue(model, subject, rItemForIndexing, null);
			if ("n".equals(itemForIndexing)) {
				return null;
			}
			String protocolVersion = RDFUtil.extractSingleValue(model, subject, rProtocolVersion, null);
			if (protocolVersion == null) {
				logger.error("Hittade ingen protokollversion i rdf-grafen:\n" + model);
				throw new Exception("Hittade ingen protokollversion i rdf-grafen");
			}

			LinkedList<String> gmlGeometries = new LinkedList<String>();
			LinkedList<String> relations = new LinkedList<String>();
			SamsokProtocolHandler sph = getProtocolHandlerForVersion(protocolVersion, model, subject);
			luceneDoc = sph.handle(service, added, relations, gmlGeometries);

			// den unika identifieraren och protokollversionen
			luceneDoc.addField(IX_ITEMID, identifier);
			luceneDoc.addField(IX_PROTOCOLVERSION, protocolVersion);

			// interna system-index

			// tjänst-id
			luceneDoc.addField(I_IX_SERVICE, service.getId());

			// html-url
			String url = extractSingleValue(model, subject, rURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("HTML URL starts with " + uriPrefix);
				}
				luceneDoc.addField(I_IX_HTML_URL, url);
			}
			// museumdat-url
			url = extractSingleValue(model, subject, rMuseumdatURL, null);
			if (url != null) {
				if (url.toLowerCase().startsWith(uriPrefix)) {
					addProblemMessage("Museumdat URL starts with " + uriPrefix);
				}
				luceneDoc.addField(I_IX_MUSEUMDAT_URL, url);
			}

			// lagra den första geometrins centroid
			if (gmlGeometries.size() > 0) {
				String gml = gmlGeometries.getFirst();
				if (gmlGeometries.size() > 1 && logger.isDebugEnabled()) {
					logger.debug("Hämtade " + gmlGeometries.size() +
							" geometrier för " + identifier + ", kommer bara " +
							"att använda den första");
				}
				try {
					Point2d p = GMLUtil.getLonLatCentroid(gml);
					luceneDoc.addField(I_IX_LON, p.x);
					luceneDoc.addField(I_IX_LAT, p.y);
				} catch (Exception e) {
					addProblemMessage("Error when indexing geometries for " + identifier +
							": " + e.getMessage());
				}
			}

			// lägg in relationer i specialstruktur/index (typ|uri)
			if (relations.size() > 0) {
				for (String value: relations) {
					luceneDoc.addField(I_IX_RELATIONS, value);
				}
			}

			// hämta ut presentationsblocket
			String pres = extractSingleValue(model, subject, rPres, null);
			if (pres != null && pres.length() > 0) {
				// verifiera att det är xml
				// TODO: kontrollera korrekt schema också
				Document doc = parseDocument(pres);
				// serialisera som ett xml-fragment, dvs utan xml-deklaration
				pres = serializeDocumentAsFragment(doc);
				// lagra binärt, kodat i UTF-8
				byte[] presBytes = pres.getBytes("UTF-8");
				luceneDoc.addField(I_IX_PRES, Base64.byteArrayToBase64(presBytes, 0, presBytes.length));
			}

			// lagra rdf:en 
			byte[] rdfBytes = xmlContent.getBytes("UTF-8");
			luceneDoc.addField(I_IX_RDF, Base64.byteArrayToBase64(rdfBytes, 0, rdfBytes.length));

		} catch (Exception e) {
			// TODO: kasta exception/räkna felen/annat?
			logger.error("Fel vid skapande av lucenedokument för " + identifier + ": " + e.getMessage());
			//throw e;
		} finally {
			if (model != null) {
				model.close();
			}
		}
		return luceneDoc;
	}

	private SamsokProtocolHandler getProtocolHandlerForVersion(String protocolVersion,
			Model model, Resource subject)  throws Exception {
		Double protocol;
		SamsokProtocolHandler handler = null;
		try {
			protocol = Double.parseDouble(protocolVersion);
		} catch (Exception e) {
			logger.error("Ej numeriskt protokollversionsnummer: " + protocolVersion);
			throw new Exception("Ej numeriskt protokollversionsnummer: " + protocolVersion);
		}
		final double latest = 1.11;
		if (protocol == latest) {
			handler = new SamsokProtocolHandler_1_11(model, subject);
		} else if (protocol >= 1.1 && protocol < latest) {
			handler = new SamsokProtocolHandler_1_1(model, subject);
		} else if (protocol > 0 && protocol <= 1.0) {
			handler = new SamsokProtocolHandler_0_TO_1_0(model, subject);
		} else {
			logger.error("Ej hanterat versionsnummer: " + protocolVersion);
			throw new Exception("Ej hanterat versionsnummer: " + protocolVersion);
		}
		return handler;
	}

	@Override
	public ExtractedInfo extractInfo(String xmlContent) throws Exception {
		String identifier = null;
		String htmlURL = null;
		Model model = null;
		ExtractedInfo info = new ExtractedInfo();
		try {
			model = RDFUtil.parseModel(xmlContent);
	
			Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
			Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
			Property rURL = ResourceFactory.createProperty(SamsokProtocol.uri_rURL.toString());
			Resource subject = null;
			Selector selector = new SimpleSelector((Resource) null, rdfType, samsokEntity);
			StmtIterator iter = model.listStatements(selector);
			while (iter.hasNext()){
				if (identifier != null) {
					throw new Exception("Ska bara finnas en entity");
				}
				Statement s = iter.next();
				subject = s.getSubject();
				identifier=subject.toString();
				logger.debug("Identifier: " + identifier);
				htmlURL=RDFUtil.extractSingleValue(model, subject, rURL, null);
			}
			if (identifier == null) {
				logger.error("Kunde inte extrahera identifierare ur rdf-grafen:\n" + xmlContent);
				throw new Exception("Kunde inte extrahera identifierare ur rdf-grafen");
			}
			info.setIdentifier(identifier);
			info.setNativeURL(htmlURL);

		} finally {
			if (model != null) {
				model.close();
			}
		}
		return info;
	}

	// hjälpmetod som parsar ett xml-dokument och ger en dom tillbaka
	static Document parseDocument(String xmlContent) throws ParserConfigurationException, SAXException, IOException{
        // records måste matcha schema
        //xmlFact.setSchema(schema);
        DocumentBuilder builder;
		try {
			builder = xmlFact.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			logger.error("Det är problem att konfigurerar xml-parser");
			throw e1;
		}
        StringReader sr = null;
        Document doc;
        try {
        	sr = new StringReader(xmlContent);
        	doc = builder.parse(new InputSource(sr));
        } catch (SAXException e) {
        	logger.error("Det är problem att parse följande sträng: "+xmlContent);
        	throw e;
		} catch (IOException e) {
			logger.error("Det är problem med att läsa från in-strömmen");
			throw e;
		} finally {
        	if (sr != null) {
        		try {
        			sr.close();
        		} catch (Exception ignore) {
				}
        	}
        }
        return doc;
	}

	// hjälpmetod som serialiserar ett dom-träd som xml utan xml-deklaration
	static String serializeDocumentAsFragment(Document doc) throws Exception {
		// TODO: använd samma Transformer för en hel serie, kräver refaktorering
		//       av hur ContentHelpers används map deras livscykel
		final int initialSize = 4096;
		Source source = new DOMSource(doc);
		Transformer xformer = xformerFact.newTransformer();
		// ingen xml-deklaration då vi vill använda den som ett xml-fragment
		xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		StringWriter sw = new StringWriter(initialSize);
		Result result = new StreamResult(sw);
        xformer.transform(source, result);
        sw.close();
		return sw.toString();
	}

}
