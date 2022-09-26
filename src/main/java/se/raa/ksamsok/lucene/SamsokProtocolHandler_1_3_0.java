package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static se.raa.ksamsok.lucene.ContentHelper.*;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromPeriod;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriod;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rEvent;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceTerm;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rAgent;

public class SamsokProtocolHandler_1_3_0 extends SamsokProtocolHandler_1_2_0 {

    protected static final Map<String, URI> contextRelationsMap_1_3_TO;

    static {
        /*
		* Utgå från dem i SamsokProtocolHandler_1_1 (det finns inga egna i SamsokProtocolHandler_1_11 eller SamsokProtocolHandler 1_2)
		*/
        Map<String, URI> contextRelMap = new HashMap<>(contextRelationsMap_1_1_TO);
        contextRelMap.put(IX_FROMPERIOD, uri_rFromPeriod);
        contextRelMap.put(IX_TOPERIOD, uri_rToPeriod);
        contextRelMap.put(IX_EVENT, uri_rEvent);
        contextRelMap.put(IX_PLACETERM, uri_rPlaceTerm);
        contextRelMap.put(IX_AGENT, uri_rAgent);
        contextRelationsMap_1_3_TO = Collections.unmodifiableMap(contextRelMap);
    }

    
    protected SamsokProtocolHandler_1_3_0(Model model, Resource subject) {
        super(model, subject);
    }

    @Override
    protected void extractContextTimeInformation(Resource cS, String[] contextTypes) throws Exception {
        super.extractContextTimeInformation(cS, contextTypes);

        ip.setCurrent(IX_FROMPERIOD, contextTypes, false, null);
        extractValue(model, cS, getURIRef(uri_rFromPeriod), ip);

        ip.setCurrent(IX_TOPERIOD, contextTypes, false, null);
        extractValue(model, cS, getURIRef(uri_rToPeriod), ip);

        ip.setCurrent(IX_EVENT, contextTypes, false, null);
        extractValue(model, cS, getURIRef(uri_rEvent), ip);
    }

    protected void extractContextPlaceInformation(Resource cS, String[] contextTypes, List<String> gmlGeometries) throws Exception {
        super.extractContextPlaceInformation(cS, contextTypes, gmlGeometries);

        ip.setCurrent(IX_PLACETERM, contextTypes, false, null);
        extractValue(model, cS, getURIRef(uri_rPlaceTerm), ip);

    }

    protected void extractContextActorInformation(Resource cS, String[] contextTypes, List<String> relations) throws Exception {
        super.extractContextActorInformation(cS, contextTypes, relations);

        // Vi ska även ta hand om agent här
        ip.setCurrent(IX_AGENT, contextTypes, false, null);
        extractValue(model, cS, getURIRef(uri_rAgent), ip);
    }

    
	/**
	 * Extraherar och indexerar kontextnivårelationer som hämtas via
	 * {@linkplain #getContextLevelRelationsMap()}.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param relations lista med relationer för specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractContextLevelRelations(Resource cS, List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getContextLevelRelationsMap();
		extractRelationsFromNode(cS, relationsMap, relations);
	}

    /**
	 * Ger map med giltiga toppnivårelationer nycklat på indexnamn.
	 * 
	 * Överlagra i subklasser vid behov.
	 * @return map med toppnivårelationer
	 */
    @Override
	protected Map<String, URI> getContextLevelRelationsMap() {
		return contextRelationsMap_1_3_TO;
	}

}
