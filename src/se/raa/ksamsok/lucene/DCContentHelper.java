package se.raa.ksamsok.lucene;

import java.io.StringReader;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xml.sax.InputSource;

import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.spatial.GMLInfoHolder;

/**
 * Klass främst för test som hanterar dublin core-data.
 */
public class DCContentHelper extends ContentHelper {

	private static final Logger logger = Logger.getLogger(DCContentHelper.class);

	@Override
	public Document createLuceneDocument(HarvestService service, String xmlContent) {
		Document luceneDoc = null;
		try {
	        org.w3c.dom.Document doc = parseDocument(xmlContent);
	        XPath xp = createXPath(doc);
	
			luceneDoc = new Document();
			
			luceneDoc.add(new Field(I_IX_SERVICE, service.getId(), Field.Store.YES, Field.Index.NOT_ANALYZED));
	
			// identifierare enligt http://www.loc.gov/standards/sru/march06-meeting/record-id.html
			String identifier = extractIdentifier(xp, doc);
			if (identifier == null) {
				throw new Exception("Kunde inte hitta identifier");
			}
			luceneDoc.add(new Field(CONTEXT_SET_REC + "." + IX_REC_IDENTIFIER, identifier, Field.Store.YES, Field.Index.NOT_ANALYZED));
			luceneDoc.add(new Field(IX_ITEMID, identifier, Field.Store.YES, Field.Index.NOT_ANALYZED));

			StringBuffer allText = new StringBuffer();
			StringBuffer pres = new StringBuffer();
			pres.append("<pres:item xmlns:pres=\"http://kulturarvsdata.se/presentation#\"><pres:id>");
			pres.append(xmlEscape(identifier)).append("</pres:id>");
			String creator = xp.evaluate("oai_dc:dc/dc:creator", doc);
			if (creator != null && creator.length() > 0) {
				allText.append(creator).append(" ");
			}
			String title = xp.evaluate("oai_dc:dc/dc:title", doc);
			if (title != null && title.length() > 0) {
				luceneDoc.add(new Field(IX_ITEMTITLE, title, Field.Store.YES, Field.Index.NOT_ANALYZED));
				pres.append("<pres:idLabel>").append(xmlEscape(title)).append("</pres:idLabel>");
				allText.append(title).append(" ");
			}
			String desc = xp.evaluate("oai_dc:dc/dc:description", doc);
			if (desc != null && desc.length() > 0) {
				pres.append("<pres:description>").append(xmlEscape(desc)).append("</pres:description>");
				luceneDoc.add(new Field(IX_ITEMDESCRIPTION, desc, Field.Store.NO, Field.Index.ANALYZED));
				allText.append(desc).append(" ");
			}
			luceneDoc.add(new Field(IX_TEXT, allText.toString(), Field.Store.NO, Field.Index.ANALYZED));
			luceneDoc.add(new Field(IX_STRICT, allText.toString(), Field.Store.NO, Field.Index.NOT_ANALYZED));

			pres.append("</pres:item>");
			// lagra ihopplockad presentation för detta format, inte rätt men...
			// kodat i UTF-8
			luceneDoc.add(new Field(I_IX_PRES, pres.toString().getBytes("UTF-8"), Field.Store.COMPRESS));
			// TODO: ska detta lagras av lucene, vi kan annars hämta det från db
			//luceneDoc.add(new Field("xmlContent", xmlContent, Field.Store.YES, Field.Index.UN_TOKENIZED));
		} catch (Exception e) {
			logger.error("Fel vid skapande av lucenedokument", e);
		}
		return luceneDoc;
	}

	@Override
	public String extractIdentifierAndGML(String xmlContent, GMLInfoHolder gmlInfoHolder) {
		String identifier = null;
		try {
	        org.w3c.dom.Document doc = parseDocument(xmlContent);
	        XPath xp = createXPath(doc);
	        identifier = extractIdentifier(xp, doc);
		} catch (Exception e) {
			logger.error("Fel vid extrahering av identifierare", e);
		}
		return identifier;
	}

	private org.w3c.dom.Document parseDocument(String xmlContent) throws Exception {
		DocumentBuilderFactory xmlFact = DocumentBuilderFactory.newInstance();
        xmlFact.setNamespaceAware(true);
        // records måste matcha schema
        //xmlFact.setSchema(schema);
        DocumentBuilder builder = xmlFact.newDocumentBuilder();
        //Document doc = builder.parse(new ByteArrayInputStream(xmlContent.getBytes("UTF-8")));
        org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
        return doc;
	}
	private XPath createXPath(org.w3c.dom.Document doc) {
		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		NamespaceContext nsContext = new NamespaceContextImpl(doc);
		xp.setNamespaceContext(nsContext);
		return xp;
	}
	private String extractIdentifier(XPath xp, org.w3c.dom.Document doc) throws Exception {
		return xp.evaluate("oai_dc:dc/dc:identifier", doc);
	}
	
	private static String xmlEscape(String s) {
		// TODO: inte helt 100
		return s.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
	}

}
