package se.raa.ksamsok.lucene;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;

public abstract class AbstractDocumentTest {
    static DocumentBuilderFactory xmlFact;
    static TransformerFactory xformerFact;
    static {
        xmlFact = DocumentBuilderFactory.newInstance();
        xmlFact.setNamespaceAware(true);
        xformerFact = TransformerFactory.newInstance();
    }

    String loadTestFileAsString(String fileName) throws Exception {
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

}
