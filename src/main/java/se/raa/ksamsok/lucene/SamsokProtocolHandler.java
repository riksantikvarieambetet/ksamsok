package se.raa.ksamsok.lucene;

import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import se.raa.ksamsok.harvest.HarvestService;

import java.util.Date;
import java.util.List;

public interface SamsokProtocolHandler {

	/**
	 * Skapar ett solr-dokument och fyller det med värden.
	 * 
	 * @param service tjänst
	 * @param added när posten lades till i k-samsök, om känt
	 * @param relations lista att fyllas på med relationer [typ|uri] för specialindexet
	 * @param gmlGeometries lista att fyllas på med gml
	 * @return solr-dokument
	 * @throws Exception vid fel
	 */
	SolrInputDocument handle(HarvestService service, Date added,
			List<String> relations, List<String> gmlGeometries) throws Exception;

	/**
	 * Hämtar klasspecifik logger.
	 * 
	 * @return logger
	 */
	Logger getLogger();
	
	/**
	 * Slår upp ett värde för en uri, tex länsnamn.
	 * 
	 * @param uri uri
	 * @return värde eller null
	 */
	String lookupURIValue(String uri);


	/**
	 * Om requireMediaLicense är satt till true, krävs att objektet har mediaLicense för att skapa luceneDocument
	 * @param requireMediaLicense
	 */
	void setRequireMediaLicense(boolean requireMediaLicense);

}
