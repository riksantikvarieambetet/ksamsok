package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.Logger;

import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIATYPE;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMedia;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaMotiveWord;

public class SamsokProtocolHandler_1_11 extends SamsokProtocolHandler_1_1
		implements SamsokProtocolHandler {

	private static final Logger classLogger = getClassLogger();

	protected SamsokProtocolHandler_1_11(Model model, Resource subject) {
		super(model, subject);
	}

	/**
	 * Tar medianoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som gällde för protokollversion 1.11, se dok.
	 * Anropar {@linkplain #extractMediaNodeInformation(Resource)} för varje
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
		extractMediaLicense(cS);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(model, cS, getURIRef(uri_rMediaMotiveWord), null, ip);
		extractMediaAndImageNodeInformation(cS);
	}

	/**
	 * Extraherar och indexerar information ur en bildnod.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS bildnod
	 * @throws Exception vid fel
	 */
	@Override
	protected void extractImageNodeInformation(Resource cS) throws Exception {
		super.extractImageNodeInformation(cS);
		extractMediaAndImageNodeInformation(cS);
	}

	private void extractMediaAndImageNodeInformation(Resource cS) throws Exception {
		// imagenoder och medianoder ska få samma saker indexerat
		// vi bör fundera på att slå ihop dem i framtida protokoll
		ip.setCurrent(IX_MEDIATYPE, false); // uri, ingen uppslagning fn
		extractValue(model, cS, getURIRef(SamsokProtocol.uri_rMediaType), null, ip);
		ip.setCurrent(ContentHelper.IX_BYLINE, false); 
		extractValue(model, cS, getURIRef(SamsokProtocol.uri_rByline), null, ip);
		ip.setCurrent(ContentHelper.IX_COPYRIGHT, false); 
		extractValue(model, cS, getURIRef(SamsokProtocol.uri_rCopyright), null, ip);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}

}
