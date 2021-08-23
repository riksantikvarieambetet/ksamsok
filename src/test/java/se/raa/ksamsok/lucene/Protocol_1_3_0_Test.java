package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.*;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.HarvestServiceImpl;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@SuppressWarnings("unused")
public class Protocol_1_3_0_Test extends AbstractDocumentTest {


    @Test
    public void testTimePeriod() throws Exception {
        String rdf = loadTestFileAsString("hjalm_1.3.0.rdf");
        Model model = RDFUtil.parseModel(rdf);
        assertNotNull("Ingen graf, fel på rdf:en?", model);

        Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
        Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
        SimpleSelector selector = new SimpleSelector(null, rdfType, samsokEntity);

        Resource s = null;
        StmtIterator iter = model.listStatements(selector);
        while (iter.hasNext()) {
            if (s != null) {
                throw new Exception("Ska bara finnas en entity i rdf-grafen");
            }
            s = iter.next().getSubject();
        }
        SamsokProtocolHandler handler = new SamsokProtocolHandler_1_3_0(model, s);
        HarvestService service = new HarvestServiceImpl();
        service.setId("TESTID");
        LinkedList<String> relations = new LinkedList<>();
        List<String> gmlGeometries = new LinkedList<>();
        SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
        assertNotNull("Inget doc tillbaka", doc);
        Collection<Object> toPeriods = doc.getFieldValues(ContentHelper.IX_TOPERIOD);
        assertThat(toPeriods, hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/2"));
        assertThat(toPeriods, not(hasItem((String) null)));
        assertThat(toPeriods, not(hasItems("http://kulturarvsdata.se/raa/test/3", "http://kulturarvsdata.se/raa/test/4")));
        Collection<Object> fromPeriods = doc.getFieldValues(ContentHelper.IX_FROMPERIOD);
        assertThat(fromPeriods, not(hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/2")));
        assertThat(fromPeriods, not(hasItem((String) null)));
        assertThat(fromPeriods, hasItems("http://kulturarvsdata.se/raa/test/3", "http://kulturarvsdata.se/raa/test/4"));

    }

    @Test
    public void testPlaceTerm() throws Exception {

        String rdf = loadTestFileAsString("hjalm_1.3.0.rdf");
        Model model = RDFUtil.parseModel(rdf);
        assertNotNull("Ingen graf, fel på rdf:en?", model);

        Property rdfType = ResourceFactory.createProperty(SamsokProtocol.uri_rdfType.toString());
        Resource samsokEntity = ResourceFactory.createResource(SamsokProtocol.uri_samsokEntity.toString());
        SimpleSelector selector = new SimpleSelector(null, rdfType, samsokEntity);

        Resource s = null;
        StmtIterator iter = model.listStatements(selector);
        while (iter.hasNext()) {
            if (s != null) {
                throw new Exception("Ska bara finnas en entity i rdf-grafen");
            }
            s = iter.next().getSubject();
        }
        SamsokProtocolHandler handler = new SamsokProtocolHandler_1_3_0(model, s);
        HarvestService service = new HarvestServiceImpl();
        service.setId("TESTID");
        LinkedList<String> relations = new LinkedList<>();
        List<String> gmlGeometries = new LinkedList<>();
        SolrInputDocument doc = handler.handle(service, new Date(), relations, gmlGeometries);
        assertNotNull("Inget doc tillbaka", doc);
        Collection<Object> placeTerms = doc.getFieldValues(ContentHelper.IX_PLACETERM);
        assertThat(placeTerms, hasItems("http://kulturarvsdata.se/raa/test/5", "http://kulturarvsdata.se/raa/test/6"));
        assertThat(placeTerms, not(hasItem((String) null)));
        assertThat(placeTerms, not(hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/4")));
    }
}
