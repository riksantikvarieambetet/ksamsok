package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.*;
import org.apache.solr.common.SolrInputDocument;
import org.w3c.dom.Document;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import javax.swing.text.AbstractDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractDocumentTest {
    static DocumentBuilderFactory xmlFact;
    static TransformerFactory xformerFact;
    static {
        xmlFact = DocumentBuilderFactory.newInstance();
        xmlFact.setNamespaceAware(true);
        xformerFact = TransformerFactory.newInstance();
    }

    final String loadTestFileAsString(String fileName) throws Exception {
        DocumentBuilder builder = xmlFact.newDocumentBuilder();
        StringWriter sw = null;
        try {
            Document doc = builder.parse(new File("src/test/resources/" + fileName));
            final int initialSize = 4096;
            Source source = new DOMSource(doc);
            Transformer xformer = xformerFact.newTransformer();
            xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            sw = new StringWriter(initialSize);
            Result result = new StreamResult(sw);
            xformer.transform(source, result);
            return sw.toString();
        } finally {
            if (sw != null) {
                sw.close();
            }
        }
    }

    final SolrInputDocument getSolrInputDocument(String fileName, List<String> relations) throws Exception {
        String rdf = loadTestFileAsString(fileName);
        Model model = RDFUtil.parseModel(rdf);
        assertNotNull("Ingen graf, fel på rdf:en?", model);

        Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
        Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
        SimpleSelector selector = new SimpleSelector(null, rdfType, samsokEntity);

        Resource s = null;
        StmtIterator iter = model.listStatements(selector);
        while (iter.hasNext()) {
            if (s != null) {
                throw new Exception("Ska bara finnas en entity i rdf-grafen");
            }
            s = iter.next().getSubject();
        }
        SamsokProtocolHandler handler = getSamsokProtocolHandler(model, s);
        HarvestService service = new HarvestServiceImpl();
        service.setId("TESTID");
        List<String> gmlGeometries = new LinkedList<>();

        return handler.handle(service, new Date(), relations, gmlGeometries);

    }

    abstract SamsokProtocolHandler getSamsokProtocolHandler(Model model, Resource s);


}
