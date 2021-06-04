package se.raa.ksamsok.lucene;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static se.raa.ksamsok.lucene.ContentHelper.IX_SAMEAS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISRELATEDTO;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTAINSOBJECT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASBEENUSEDIN;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASCHILD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASPARENT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFIND;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISFOUNDIN;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASIMAGE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASOBJECTEXAMPLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASPART;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISPARTOF;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISDESCRIBEDBY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISVISUALIZEDBY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_VISUALIZES;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTAINSINFORMATIONABOUT;
import static se.raa.ksamsok.lucene.SamsokProtocol.uriPrefixKSamsok;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSameAs;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rIsRelatedTo;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContainsObject;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasBeenUsedIn;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasChild;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasParent;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasFind;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rIsFoundIn;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasImage;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasObjectExample;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHasPart;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rIsPartOf;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rIsDescribedBy;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rIsVisualizedBy;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rVisualizes;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContainsInformationAbout;

public class SamsokProtocolHandler_0_TO_1_0 extends BaseSamsokProtocolHandler {

	private static final Logger classLogger = getClassLogger();

	// map med uri -> värde för indexering
	private static final Map<String,String> uriValues_0_TO_1_0;
	// bassökväg
	static final String PATH = "/";
	// reationsmap
	static final Map<String, URI> relationsMap_0_TO_1_0;

	static {
		Map<String,String> values = new HashMap<>();
		// läs in uri-värden för uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_0_TO_1.0.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "contexttype_0_TO_1.0.rdf", SamsokProtocol.uri_rContextLabel, values);

		uriValues_0_TO_1_0 = Collections.unmodifiableMap(values);

		Map<String, URI> relMap = new HashMap<>();
		// hämta ut sameAs (0n)
		relMap.put(IX_SAMEAS, uri_rSameAs);
		// hämta ut isRelatedTo (0n)
		relMap.put(IX_ISRELATEDTO, uri_rIsRelatedTo);
		// hämta ut containsObject (0n)
		relMap.put(IX_CONTAINSOBJECT, uri_rContainsObject);
		// hämta ut hasBeenUsedIn (0n)
		relMap.put(IX_HASBEENUSEDIN, uri_rHasBeenUsedIn);
		// hämta ut hasChild (0n)
		relMap.put(IX_HASCHILD, uri_rHasChild);
		// hämta ut hasParent (0n)
		relMap.put(IX_HASPARENT, uri_rHasParent);
		// hämta ut hasFind (0n)
		relMap.put(IX_HASFIND, uri_rHasFind);
		// hämta ut isFoundIn (0n)
		relMap.put(IX_ISFOUNDIN, uri_rIsFoundIn);
		// hämta ut hasImage (0n)
		relMap.put(IX_HASIMAGE, uri_rHasImage);
		// hämta ut hasObjectExample (0n)
		relMap.put(IX_HASOBJECTEXAMPLE, uri_rHasObjectExample);
		// hämta ut hasPart (0n)
		relMap.put(IX_HASPART, uri_rHasPart);
		// hämta ut isPartOf (0n)
		relMap.put(IX_ISPARTOF, uri_rIsPartOf);
		// hämta ut isDescribedBy (0n)
		relMap.put(IX_ISDESCRIBEDBY, uri_rIsDescribedBy);
		// hämta ut isVisualizedBy (0n)
		relMap.put(IX_ISVISUALIZEDBY, uri_rIsVisualizedBy);
		// hämta ut visualizes (0n)
		relMap.put(IX_VISUALIZES, uri_rVisualizes);
		// hämta ut containsInformationAbout (0n)
		relMap.put(IX_CONTAINSINFORMATIONABOUT, uri_rContainsInformationAbout);

		relationsMap_0_TO_1_0 = Collections.unmodifiableMap(relMap);
	}

	protected SamsokProtocolHandler_0_TO_1_0(Model model, Resource subject) {
		super(model, subject);
	}

	@Override
	protected Map<String, String> getURIValues() {
		return uriValues_0_TO_1_0;
	}

	@Override
	public String getRelationTypeNameFromURI(String refUri) {
		// specialhantering av relationer
		String relationType;
		if (uri_rSameAs.toString().equals(refUri)) {
			relationType = ContentHelper.IX_SAMEAS;
		} else {
			relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, uriPrefixKSamsok));
		}
		return relationType;
	}

	@Override
	protected Map<String, URI> getTopLevelRelationsMap() {
		return relationsMap_0_TO_1_0;
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}

}
