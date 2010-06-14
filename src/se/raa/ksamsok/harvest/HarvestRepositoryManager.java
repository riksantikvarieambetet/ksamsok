package se.raa.ksamsok.harvest;

import java.io.File;
import java.sql.Timestamp;

/**
 * Tjänst som hanterar lagring i repository.
 */
public interface HarvestRepositoryManager {

	/**
	 * Går igenom en hämtad OAI-PMH-skörd och lagrar den i repositoryt.
	 * 
	 * @param service tjänst
	 * @param sm metadata om skördetjänsten
	 * @param xmlFile fil med OAI-PMH-xml
	 * @param ts timestamp
	 * @return sant om något uppdaterades
	 * @throws Exception
	 */
	boolean storeHarvest(HarvestService service, ServiceMetadata sm, File xmlFile, Timestamp ts) throws Exception;

	/**
	 * Uppdaterar lucene-index med data från repositoryt.
	 * 
	 * @param service tjänst
	 * @param ts timestamp att uppdatera från, eller null för allt
	 * @throws Exception
	 */
	void updateLuceneIndex(HarvestService service, Timestamp ts) throws Exception;

	/**
	 * Uppdaterar lucene-index med data från repositoryt. Om enclosingService
	 * är skilt från null kommer dess avbrottsstatus att kontrolleras samtidigt
	 * som tjänstens.
	 * 
	 * @param service tjänst
	 * @param ts timestamp att uppdatera från, eller null för allt
	 * @param enclosingService tjänst som styr körningen
	 * @throws Exception
	 */
	void updateLuceneIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception;

	/**
	 * Tar bort lucene-index för en tjänst (gömmer tjänsten).
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void removeLuceneIndex(HarvestService service) throws Exception;
	
	/**
	 * Tar bort all data i repositoryt för en tjänst.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void deleteData(HarvestService service) throws Exception;

	/**
	 * Hämtar xml (rdf) för en inskickad uri som identifierar en post.
	 * 
	 * @param uri identifierare
	 * @return k-samsöks-xml
	 * @throws Exception
	 */
	String getXMLData(String uri) throws Exception;

	/**
	 * Ger antalet poster i repositoryt för en tjänst.
	 * 
	 * @param service tjänst
	 * @return antal poster
	 * @throws Exception
	 */
	int getCount(HarvestService service) throws Exception;

	/**
	 * Ger spoolfil för en tjänst.
	 * @param service tjänst
	 * @return spoolfil
	 */
	File getSpoolFile(HarvestService service);
	
	/**
	 * Ger spoolfil för en tjänst.
	 * @param service tjänst
	 * @return spoolfil
	 */
	File getZipFile(HarvestService service);
	
	
	/**
	 * Packar upp gzipfil med tidigare skörd till spool-xml-dokument
	 */
	public void extractGZipToSpool(HarvestService service);
}
