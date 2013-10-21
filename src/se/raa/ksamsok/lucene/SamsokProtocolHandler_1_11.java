package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMedia;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaMotiveWord;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class SamsokProtocolHandler_1_11 extends SamsokProtocolHandler_1_1
		implements SamsokProtocolHandler {

	private static final Logger classLogger = getClassLogger();

	protected SamsokProtocolHandler_1_11(Model model, Resource subject) {
		super(model, subject);
	}

	/**
	 * Tar medianoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som gällde för protokollversion 1.11, se dok.
	 * Anropar {@linkplain #extractMediaNodeInformation(SubjectNode)} för varje
	 * medianod.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractMediaNodes() throws Exception {
		// läs in värden från medianoder
		Selector selector = new SimpleSelector(subject, getURIRef(uri_rMedia), (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			Statement s = iter.next();
			if (s.getObject().isResource()){
				extractMediaNodeInformation(s.getObject().asResource());
			}
		}
	}

	/**
	 * Extraherar och indexerar information ur en medianod.
	 * Hanterar de index som gällde för protokollversion 1.11, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS medianod
	 * @throws Exception vid fel
	 */
	protected void extractMediaNodeInformation(Resource cS) throws Exception {
		// samma som image-noder i protokoll < 1.11
		ip.setCurrent(IX_MEDIALICENSE, false); // uri, ingen uppslagning fn
		extractValue(model, cS, getURIRef(uri_rMediaLicense), null, ip);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(model, cS, getURIRef(uri_rMediaMotiveWord), null, ip);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}

}
