package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;

import static org.junit.Assert.*;

@SuppressWarnings("unused")
public class Protocol_1_11_Test extends AbstractDocumentTest {


    @Test
    public void testNoMediaLicense() {
        LinkedList<String> relations = new LinkedList<>();
        try {
            SolrInputDocument doc = getSolrInputDocument("hjalm_1.11_felaktig.rdf", relations);
            Assert.fail("Was expecting an exception due to missing mediaLicense");
        } catch (Exception e) {
            // That's ok, there should be an exception thrown here
        }
    }


    @Test
    public void testParseMedia() throws Exception {
        LinkedList<String> relations = new LinkedList<>();
        SolrInputDocument doc = getSolrInputDocument("media.rdf", relations);
        assertNotNull("Inget doc tillbaka", doc);
        assertEquals("Fel antal relationer tillbaka", 0, relations.size());

//		// kontrollera exists-index
        assertEquals("Felaktigt värde för geodataExists", "j", doc.getFieldValue(ContentHelper.IX_GEODATAEXISTS));
        assertEquals("Felaktigt värde för thumbnailExists", "j", doc.getFieldValue(ContentHelper.IX_THUMBNAILEXISTS));
        assertEquals("Felaktigt värde för timeInfoExists", "n", doc.getFieldValue(ContentHelper.IX_TIMEINFOEXISTS));
        assertEquals("Felaktig objektsupertyp", "Fysiskt ting", doc.getFieldValue(ContentHelper.IX_ITEMSUPERTYPE));
        Collection<Object> contextSuperTypes = doc.getFieldValues(ContentHelper.IX_CONTEXTSUPERTYPE);
        assertNotNull("Kontextsupertyper saknas", contextSuperTypes);
        assertTrue("Kontextsupertypen 'Tillverka' (create) saknas", contextSuperTypes.contains("create"));

        // kolla mediaindex
        assertEquals("Felaktigt värde för mediaLicense", "http://kulturarvsdata.se/resurser/License#by", doc.getFieldValue(ContentHelper.IX_MEDIALICENSE));
        assertEquals("Felaktigt värde för mediaMotiveWord", "Ett mediamotiv", doc.getFieldValue(ContentHelper.IX_MEDIAMOTIVEWORD));
        assertEquals("Felaktigt värde för byline", "Arbetets Museum", doc.getFieldValue(ContentHelper.IX_BYLINE));
        assertEquals("Felaktigt värde för copyright", "Arbetets Museum", doc.getFieldValue(ContentHelper.IX_COPYRIGHT));
        assertEquals("Felaktigt värde för thumbnailSource", "http://www.workwithsounds.eu/wp-content/uploads/2014/10/IMG_2867-204x136.jpg", doc.getFieldValue(ContentHelper.IX_THUMBNAIL_SOURCE));

    }

    @Override
    SamsokProtocolHandler getSamsokProtocolHandler(Model model, Resource s) {
        return new SamsokProtocolHandler_1_11(model, s);
    }
}
