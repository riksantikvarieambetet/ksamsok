package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.util.List;

import static se.raa.ksamsok.lucene.ContentHelper.*;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODNAME;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.*;

public class SamsokProtocolHandler_1_3_0 extends SamsokProtocolHandler_1_2_0 {

    protected SamsokProtocolHandler_1_3_0(Model model, Resource subject) {
        super(model, subject);
    }

    @Override
    protected void extractContextTimeInformation(Resource cS, String[] contextTypes) throws Exception {
        super.extractContextTimeInformation(cS, contextTypes);

        ip.setCurrent(IX_FROMPERIOD, contextTypes);
        extractValue(model, cS, getURIRef(uri_rFromPeriod), ip);

        ip.setCurrent(IX_TOPERIOD, contextTypes);
        extractValue(model, cS, getURIRef(uri_rToPeriod), ip);
    }

    protected void extractContextPlaceInformation(Resource cS, String[] contextTypes, List<String> gmlGeometries) throws Exception {
        super.extractContextPlaceInformation(cS, contextTypes, gmlGeometries);

        ip.setCurrent(IX_PLACETERM, contextTypes);
        extractValue(model, cS, getURIRef(uri_rPlaceTerm), ip);

    }

    protected void extractContextActorInformation(Resource cS, String[] contextTypes, List<String> relations) throws Exception {
        super.extractContextActorInformation(cS, contextTypes, relations);

        ip.setCurrent(IX_AGENT, contextTypes);
        extractValue(model, cS, getURIRef(uri_rAgent), ip);
    }

}
