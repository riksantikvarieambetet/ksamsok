package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMedia;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaMotiveWord;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

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
		for (Triple triple: model.find(subject, getURIRef(elementFactory, uri_rMedia), AnyObjectNode.ANY_OBJECT_NODE)) {
			if (triple.getObject() instanceof SubjectNode) {
				SubjectNode cS = (SubjectNode) triple.getObject();

				extractMediaNodeInformation(cS);
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
	protected void extractMediaNodeInformation(SubjectNode cS) throws Exception {
		// samma som image-noder i protokoll < 1.11
		ip.setCurrent(IX_MEDIALICENSE, false); // uri, ingen uppslagning fn
		extractValue(model, cS, getURIRef(elementFactory, uri_rMediaLicense), null, ip);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(model, cS, getURIRef(elementFactory, uri_rMediaMotiveWord), null, ip);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}

}
