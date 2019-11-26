package se.raa.ksamsok.harvest;

import org.json.JSONObject;
import se.raa.ksamsok.harvest.StatusService.Step;

import java.util.Date;
import java.util.List;

/**
 * Tjänst som hanterar skördetjänster.
 */
public interface HarvestServiceManager {

	// nyckelvärden för att nå managers i skördejobb
    String HSM_KEY = "hsm";
	String HRM_KEY = "hrm";
	String SS_KEY = "ss";

	// namn på lucenespecifika interna tjänster (eg bara cronjobb)
	// TODO: värdena kanske bör ändras då det är solr nu, men de ligger i db också
    String SERVICE_INDEX_OPTIMIZE = "_lucene_opt";
	String SERVICE_INDEX_REINDEX = "_lucene_reindex";

	/**
	 * Ger lista med alla användarskapade tjänster.
	 * 
	 * @return lista med tjänster, eller null vid databasproblem
	 */
	List<HarvestService>getServices();

	/**
	 * Hämtar böna för tjänst med inskickad id.
	 * 
	 * @param serviceId id
	 * @return tjänst eller null
	 */
	HarvestService getService(String serviceId);
	
	/**
	 * Hämtar böna i json-format för tjänst med inskickad id.
	 * 
	 * @param serviceId id
	 * @return json-objekt eller null
	 */
	JSONObject getServiceAsJSON(String serviceId);

	/**
	 * Uppdaterar tjänst i databasen.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void updateService(HarvestService service) throws Exception;
	
	/**
	 * Uppdaterar pausat tillstånd för tjänster i databasen.
	 * Denna metod kan bara köras då ksamsök initieras.
	 * 
	 * @param  paused
	 * @throws Exception
	 */
	void togglePausedForServices(boolean paused) throws Exception;

	/**
	 * Uppdaterar endast datumfältet (senaste lyckade skörd) för tjänsten i databasen.
	 * 
	 * @param service tjänst
	 * @param date datum
	 * @throws Exception
	 */
	void updateServiceDate(HarvestService service, Date date) throws Exception;

	/**
	 * Lagrar första gången tjänsten indexerades ok om inget värde finns.
	 * @param service tjänst
	 */
	void storeFirstIndexDateIfNotSet(HarvestService service) throws Exception;

	/**
	 * Tar bort en tjänst ur databasen. Tar även bort data ur repositoryt och ifrån
	 * indexet.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void deleteService(HarvestService service) throws Exception;

	/**
	 * Skapar en ny tjänst i databasen.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void createService(HarvestService service) throws Exception;

	/**
	 * Skapar en ny instans av en tjänsteböna.
	 * 
	 * @return ny tom instans
	 */
	HarvestService newServiceInstance();

	/**
	 * Triggar igång en skörd, dvs en full körning av skördejobbet för tjänsten.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void triggerHarvest(HarvestService service) throws Exception;

	/**
	 * Triggar igång en omindexering, dvs en delkörning av skördejobbet för tjänsten.
	 * Omindexeringen görs utifrån data i repositoryt.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void triggerReindex(HarvestService service) throws Exception;

	/**
	 * Triggar igång en avindexering, dvs gömmer tjänsten utan att tömma repositoryt.
	 * 
	 * @param service tjänst
	 * @throws Exception
	 */
	void triggerRemoveindex(HarvestService service) throws Exception;

	/**
	 * Triggar igång omindexering av alla tjänster.
	 * 
	 * @throws Exception
	 */
    void triggerReindexAll() throws Exception;

	/**
	 * Begär att en pågående skörd ska avbrytas.
	 * 
	 * @param service tjänst
	 * @return sant om cronscheduleraren tyckte att jobbet kunde avbrytas
	 * @throws Exception
	 */
	boolean interruptHarvest(HarvestService service) throws Exception;

	/**
	 * Begär att en pågående omindexering av alla tjänster ska avbrytas.
	 * 
	 * @return sant om cronscheduleraren tyckte att jobbet kunde avbrytas
	 * @throws Exception
	 */
	boolean interruptReindexAll() throws Exception;

	/**
	 * Hämtar senast kända jobbstatus.
	 * 
	 * @param service tjänst
	 * @return senaste jobbstatus
	 */
	String getJobStatus(HarvestService service);

	/**
	 * Ger om tjänstens jobb kör fn.
	 * 
	 * @param service tjänst
	 * @return sant om tjänsten skördas eller omindexeras
	 */
	boolean isRunning(HarvestService service);

	/**
	 * Hämtar senast kända jobbsteg.
	 * 
	 * @param service tjänst
	 * @return senaste jobbsteg
	 */
	Step getJobStep(HarvestService service);

	/**
	 * Hämtar jobblogg för senaste körning efter omstart (bara från minne).
	 * 
	 * @param service tjänst
	 * @return lista med meddelanden
	 */
	List<String> getJobLog(HarvestService service);

	/**
	 * Hämtar jobblogg från databas.
	 * 
	 * @param service tjänst
	 * @return lista med meddelanden
	 */
	List<String> getJobLogHistory(HarvestService service);

	/**
	 * Ger om quartz-scheduleraren har startats. I princip är detta samma sak som
	 * att kontrollera att init har lyckats.
	 * @return sant om scheduleraren körs
	 */
	boolean isSchedulerStarted();
}
