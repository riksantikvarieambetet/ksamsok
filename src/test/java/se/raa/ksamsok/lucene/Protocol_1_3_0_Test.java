package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;


@SuppressWarnings("unused")
public class Protocol_1_3_0_Test extends AbstractDocumentTest {


    String getRdfFileName() {
        return "hjalm_1.3.0.rdf";
    }

    SamsokProtocolHandler getSamsokProtocolHandler(Model model, Resource s) {
        return new SamsokProtocolHandler_1_3_0(model, s);
    }

    @Test
    public void testTimePeriod() throws Exception {
        SolrInputDocument doc = getSolrInputDocument("hjalm_1.3.0.rdf", new LinkedList<>());
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
        SolrInputDocument doc = getSolrInputDocument("hjalm_1.3.0.rdf", new LinkedList<>());
        assertNotNull("Inget doc tillbaka", doc);
        Collection<Object> placeTerms = doc.getFieldValues(ContentHelper.IX_PLACETERM);
        assertThat(placeTerms, hasItems("http://kulturarvsdata.se/raa/test/5", "http://kulturarvsdata.se/raa/test/6"));
        assertThat(placeTerms, not(hasItem((String) null)));
        assertThat(placeTerms, not(hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/4")));
    }

    @Test
    public void testAgent() throws Exception {
        SolrInputDocument doc = getSolrInputDocument("hjalm_1.3.0.rdf", new LinkedList<>());
        assertNotNull("Inget doc tillbaka", doc);
        Collection<Object> agents = doc.getFieldValues(ContentHelper.IX_AGENT);
        assertThat(agents, hasItems("http://kulturarvsdata.se/raa/test/8", "http://kulturarvsdata.se/raa/test/9", "http://kulturarvsdata.se/raa/test/10"));
        assertThat(agents, not(hasItem((String) null)));
        assertThat(agents, not(hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/4")));
    }

    @Test
    public void testEvent() throws Exception {
        SolrInputDocument doc = getSolrInputDocument("hjalm_1.3.0.rdf", new LinkedList<>());
        assertNotNull("Inget doc tillbaka", doc);
        Collection<Object> events = doc.getFieldValues(ContentHelper.IX_EVENT);
        assertThat(events, hasItems("http://kulturarvsdata.se/raa/test/11", "http://kulturarvsdata.se/raa/test/12", "http://kulturarvsdata.se/raa/test/13"));
        assertThat(events, not(hasItem((String) null)));
        assertThat(events, not(hasItems("http://kulturarvsdata.se/raa/test/1", "http://kulturarvsdata.se/raa/test/4")));
    }

}
