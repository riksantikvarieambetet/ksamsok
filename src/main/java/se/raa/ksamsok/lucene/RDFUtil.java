package se.raa.ksamsok.lucene;

import org.apache.commons.lang3.StringUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class RDFUtil {

	private static final Logger logger = LogManager.getLogger(RDFUtil.class);

    public static Model parseModel(String rdfXml) {
		Model model;
		try (StringReader r = new StringReader(rdfXml)) {
			model = parseModel(r);
		}
		return model;
	}

	public static Model parseModel(Reader r) {
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
		StringJoiner stringJoiner = new StringJoiner(" ");
		String value;
		Selector selector = new SimpleSelector(subject, ref, (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			Statement s = iter.next();
			if (s.getObject().isLiteral()) {

				value = s.getObject().asLiteral().getString();
				stringJoiner.add(value);
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
				if (value != null) {
					stringJoiner.add(value);
				}
				if (ip != null) {
					ip.addToDoc(value);
				}
			} else if (refRef != null && s.getObject().isResource()){
				value = extractValue(model, s.getObject().asResource(), refRef, ip);
				if (value != null) {
					stringJoiner.add(value);
				}
			}
		}
		return stringJoiner.length() > 0 ? StringUtils.trimToNull(stringJoiner.toString()) : null;
	}

	// läser ut ett eller flera värden ur subjektoden där objektnoden måste vara en literal eller en uri-referens
	// och lägger till det mha indexprocessorn. forceSingle kastar exception om mer än ett värde finns
	private static String extractValue(Model model, Resource subject, Property p, IndexProcessor ip, boolean forceSingle) throws Exception {
		StringJoiner stringJoiner = new StringJoiner(" ");
    	String value = null;
		Selector selector = new SimpleSelector(subject, p, (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		try {
			while (iter.hasNext()) {
				if (forceSingle && value != null) {
					throw new Exception("Fler värden än ett för s: " + subject + " p: " + p);
				}
				Statement s = iter.next();
				if (s.getObject().isLiteral()) {
					value = StringUtils.trimToNull(s.getObject().asLiteral().getString());
				} else if (s.getObject().isURIResource()) {
					value = getReferenceValue(s.getObject().asResource(), ip, null, null);
				} else {
					throw new Exception("Måste vara literal/urireference o.class: " + s.getObject().getClass().getSimpleName() + " för s: " + subject + " p: " + p);
				}
				stringJoiner.add(value);
				if (ip != null) {
					ip.addToDoc(value);
				}
			}
		} finally {
			if (iter != null) {
				iter.close();
			}
		}
		if (stringJoiner.length() > 0) {
			return StringUtils.trimToNull(stringJoiner.toString());
		} else {
			return null;
		}
	}

	// läser ut ett enkelt värde ur subjektoden där objektnoden måste vara en literal eller en uri-referens
	// och lägger till det mha indexprocessorn
	static String extractSingleValue(Model model, Resource subject, Property p, IndexProcessor ip) throws Exception {
    	return extractValue(model, subject, p, ip, true);

	}

	// läser ut ett eller flera värden ur subjektoden där objektnoden måste vara en literal eller en uri-referens
	// och lägger till det mha indexprocessorn
	static String extractValue(Model model, Resource subject, Property p, IndexProcessor ip) throws Exception {
		return extractValue(model, subject, p, ip, false);

	}

	// försöker översätta ett uri-värde till ett förinläst värde
	private static String getReferenceValue(Resource object, IndexProcessor ip, List<String> relations, Property ref) throws Exception {
		String value;
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
		Model model = null;
		try (Reader r = new InputStreamReader(RDFUtil.class.getResourceAsStream(fileName), StandardCharsets.UTF_8)) {
			model = parseModel(r);
			Property rName = ResourceFactory.createProperty(predicateURI.toString());
			Selector selector = new SimpleSelector(null, rName, (RDFNode) null);
			StmtIterator iter = model.listStatements(selector);
			int statementCnt = 0;
			while (iter.hasNext()) {
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
			if (model != null) {
				model.close();
			}
		}
	}

}
