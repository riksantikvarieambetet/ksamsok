package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.List;

import static se.raa.ksamsok.lucene.ContentHelper.*;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.*;

public class SamsokProtocolHandler_1_3_0 extends SamsokProtocolHandler_1_2_0 {

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

}
