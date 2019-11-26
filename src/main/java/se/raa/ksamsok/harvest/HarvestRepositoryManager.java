package se.raa.ksamsok.harvest;

import java.io.File;
import java.sql.Timestamp;
import java.util.Map;

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
	 * Uppdaterar index med data från repositoryt.
	 * 
	 * @param service tjänst
	 * @param ts timestamp att uppdatera från, eller null för allt
	 * @throws Exception
	 */
	void updateIndex(HarvestService service, Timestamp ts) throws Exception;

	/**
	 * Uppdaterar index med data från repositoryt. Om enclosingService
	 * är skilt från null kommer dess avbrottsstatus att kontrolleras samtidigt
	 * som tjänstens.
	 * 
	 * @param service tjänst
	 * @param ts timestamp att uppdatera från, eller null för allt
	 * @param enclosingService tjänst som styr körningen
	 * @throws Exception
	 */
	void updateIndex(HarvestService service, Timestamp ts, HarvestService enclosingService) throws Exception;

	/**
	 * Tar bort index-data för en tjänst (gömmer tjänsten).
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void deleteIndexData(HarvestService service) throws Exception;
	
	/**
	 * Tar bort all data i repositoryt för en tjänst.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void deleteData(HarvestService service) throws Exception;

	/** Kollar om efterfrågad uri finns i databasen
	 *
	 * @param uri identifierare
	 * @return true om den finns, false annars
	 * @throws Exception
	 */
	public boolean existsInDatabase(String uri) throws Exception;

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
	 * Ger antal poster i repositoryt per tjänst.
	 * 
	 * @return antal poster per tjänst nycklade på tjänste-id
	 * @throws Exception
	 */
	Map<String, Integer> getCounts() throws Exception;

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
    void extractGZipToSpool(HarvestService service);

	/**
	 *  Kör optimering av indexet.
	 * @throws Exception vid fel
	 */
	void optimizeIndex() throws Exception;

	/**
	 * Rensar indexet - OBS mycket bättre att stoppa tomcat och rensa indexkatalogen.
	 * 
	 * @throws Exception
	 */
	void clearIndex() throws Exception;

	/**
	 * Ger spoolkatalogen.
	 * @return spoolkatalogen
	 */
	File getSpoolDir();
}
