package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMINSCRIPTION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMMARK;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemInscription;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemMark;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Form;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Text;

public class SamsokProtocolHandler_1_2_0 extends SamsokProtocolHandler_1_11 {

	protected SamsokProtocolHandler_1_2_0(Model model, Resource subject) {
		super(model, subject);
	}

	@Override
	protected void extractItemInformation() throws Exception {
		super.extractItemInformation();

		// hämta ut itemMark, resursnod (0m)
		ip.setCurrent(IX_ITEMMARK); // fritext
		extractValue(model, subject, getURIRef(uri_rItemMark), getURIRef(uri_r__Form), ip);
		// hämta ut itemInscription, resursnod (0m)
		ip.setCurrent(IX_ITEMINSCRIPTION); // fritext
		extractValue(model, subject, getURIRef(uri_rItemInscription), getURIRef(uri_r__Text), ip);

	}
}
