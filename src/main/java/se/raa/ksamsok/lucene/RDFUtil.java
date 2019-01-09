package se.raa.ksamsok.lucene;

import org.apache.commons.lang.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class RDFUtil {

	private static final Logger logger = LogManager.getLogger();

    public static Model parseModel(String rdfXml) throws Exception {
		Model model;
		StringReader r = null;
		try {
			r = new StringReader(rdfXml);
			model = parseModel(r);
		} finally {
			if (r != null) {
				r.close();
			}
		}
		return model;
	}

	public static Model parseModel(Reader r) throws Exception {
		Model m = ModelFactory.createDefaultModel();
		m.read(r, "");
		return m;
	}

	// läser ut ett värde ur subjektnoden eller subjektnodens objektnod om denna är en subjektnod
	// och lägger till värdet mha indexprocessorn
	static String extractValue(Model model, Resource subject, Property ref, Property refRef, IndexProcessor ip) throws Exception {
		return extractValue(model, subject, ref, refRef, ip, null);
	}

	// läser ut ett värde ur subjektnoden eller subjektnodens objektnod om denna är en subjektnod
	// och lägger till värdet mha indexprocessorn och ev specialhanterar relationer
	static String extractValue(Model model, Resource subject, Property ref, Property refRef, IndexProcessor ip, List<String> relations) throws Exception {
		final String sep = " ";
		StringBuilder buf = new StringBuilder();
		String value = null;
		Selector selector = new SimpleSelector(subject, ref, (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			Statement s = iter.next();
			if (s.getObject().isLiteral()){
				if (buf.length() > 0) {
					buf.append(sep);
				}
				value = s.getObject().asLiteral().getString();
				buf.append(value);
				if (ip != null) {
					ip.addToDoc(value);
					// specialfall för att hantera relationer som nog egentligen felaktigt(?) är vanliga värden, de borde vara rdf-resurser och då URIReferences
					// jmfr <isPartOf>http...</isPartOf> och <isPartOf rdf:resource="http..." />
					if (relations != null) {
						ip.lookupAndHandleURIValue(value, relations, ref != null ? ref.getURI() : null);
					}
				}
			} else if (s.getObject().isURIResource()){
				value = getReferenceValue(s.getObject().asResource(), ip, relations, ref);
				// lägg till i buffer bara om detta är en uri vi ska slå upp värde för
				if (value != null && ip != null && ip.translateURI()) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
				if (ip != null) {
					ip.addToDoc(value);
				}
			} else if (refRef != null && s.getObject().isResource()){
				value = extractSingleValue(model, s.getObject().asResource(), refRef, ip);
				if (value != null) {
					if (buf.length() > 0) {
						buf.append(sep);
					}
					buf.append(value);
				}
			}
			value = null;
		}
		return buf.length() > 0 ? StringUtils.trimToNull(buf.toString()) : null;
	}

	// läser ut ett enkelt värde ur subjektoden där objektnoden måste vara en literal eller en uri-referens
	// och lägger till det mha indexprocessorn
	static String extractSingleValue(Model model, Resource subject, Property p, IndexProcessor ip) throws Exception {
		String value = null;
		Selector selector = new SimpleSelector(subject, p, (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			if (value != null) {
				throw new Exception("Fler värden än ett för s: " + subject + " p: " + p);
			}
			Statement s = iter.next();
			if (s.getObject().isLiteral()){
				value = StringUtils.trimToNull(s.getObject().asLiteral().getString());
			} else if (s.getObject().isURIResource()){
				value = getReferenceValue(s.getObject().asResource(), ip, null, null);
			}else {
				throw new Exception("Måste vara literal/urireference o.class: " + s.getObject().getClass().getSimpleName() + " för s: " + subject + " p: " + p);
			}
			if (ip != null) {
				ip.addToDoc(value);
			}
		}
		return value;
	}

	// försöker översätta ett uri-värde till ett förinläst värde
	private static String getReferenceValue(Resource object, IndexProcessor ip, List<String> relations, Property ref) throws Exception {
		String value = null;
		String uri = object.getURI();
		String refUri = ref != null ? ref.getURI() : null;
		// se om vi ska försöka ersätta uri:n med en uppslagen text
		// och lägga in den i relations
		if (ip != null) { // && ip.translateURI()) {
			value = ip.lookupAndHandleURIValue(uri, relations, refUri);
		} else {
			value = StringUtils.trimToNull(uri);
		}
		return value;
/*
private String getReferenceValue(URIReference ref, IndexProcessor ip) {
String value = null;
		String uri = ref.getURI().toString();
		String lookedUpValue;
		// se om vi ska försöka ersätta uri:n med en uppslagen text
		if (ip != null && ip.translateURI() && (lookedUpValue = lookupURIValue(uri)) != null) {
			value = lookedUpValue;
		} else {
			value = StringUtils.trimToNull(uri);
		}
		return value;
 */
	}

	// läser in en rdf-resurs och lagrar uri-värden och översättningsvärden för uppslagning
	// alla resurser förutsätts vara kodade i utf-8 och att värdena är Literals
	static void readURIValueResource(String fileName, URI predicateURI, Map<String, String> uriValues) {
		Reader r = null;
		Model model = null;
		try {
			r = new InputStreamReader(RDFUtil.class.getResourceAsStream(fileName), "UTF-8");
			model = parseModel(r);
			Property rName = ResourceFactory.createProperty(predicateURI.toString());
			Selector selector = new SimpleSelector((Resource) null, rName, (RDFNode) null);
			StmtIterator iter = model.listStatements(selector);
			int statementCnt=0;
			while (iter.hasNext()){
				Statement s = iter.next();
				uriValues.put(s.getSubject().getURI(), StringUtils.trimToNull(s.getObject().asLiteral().getString()));
				statementCnt++;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Läste in " + statementCnt + " uris/värden från " + fileName);
			}
		} catch (Exception e) {
			throw new RuntimeException("Problem att läsa in uri-översättningsfil " +
					fileName, e);
		} finally {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignore) {
				}
			}
			if (model != null) {
				model.close();
			}
		}
	}

}
