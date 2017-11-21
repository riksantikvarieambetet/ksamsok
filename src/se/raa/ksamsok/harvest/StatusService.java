package se.raa.ksamsok.harvest;

import java.util.Date;
import java.util.List;

/**
 * Tjänst som hanterar status mm för skördetjänster (cron-jobb).
 */
public interface StatusService {

	/**
	 * Enum för de olika steg en tjänst kan befinna sig i.
	 */
	enum Step { FETCH, STORE, EMPTYINDEX, INDEX, IDLE }

	/**
	 * Återställer status för tjänsten så att den är redo för en ny körning.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 */
	void initStatus(HarvestService service, String message);

	/**
	 * Begär att en körande tjänst ska avbryta sig själv så snart den kan.
	 * 
	 * @param service tjänst
	 */
	void requestInterrupt(HarvestService service);

	/**
	 * Kollar och kastar exception om tjänsten ska avbrytas. Används av tjänsten
	 * för att kontrollera sin status.
	 * 
	 * @param service tjänst
	 */
	void checkInterrupt(HarvestService service);

	/**
	 * Sätter statusmeddelande och lägger också till meddelandet i tjänstens logg.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 */
	void setStatusTextAndLog(HarvestService service, String message);

	/**
	 * Sätter statusmeddelande utan att logga det.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 */
	void setStatusText(HarvestService service, String message);

	/**
	 * Sätter varningsmeddelande och lägger också till meddelandet i tjänstens logg.
	 * Ett varningsmeddelande är samma sak som ett statusmeddelande pss att tex
	 * {@linkplain #getStatusText(HarvestService)} ger senast satta varningsmeddelande.
	 * Den enda skillnaden är att de lagras med en annan kod i databasen.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 */
	void setWarningTextAndLog(HarvestService service, String message);

	/**
	 * Sätter varningsmeddelande och lägger också till meddelandet i tjänstens logg.
	 * Ett varningsmeddelande är samma sak som ett statusmeddelande pss att tex
	 * {@linkplain #getStatusText(HarvestService)} ger senast satta varningsmeddelande.
	 * Den enda skillnaden är att de lagras med en annan kod i databasen.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 * @param date tidsstämpel att ge meddelandet
	 */
	void setWarningTextAndLog(HarvestService service, String message, Date date);

	/**
	 * Hämtar senast satta statusmeddelande för tjänsten.
	 * 
	 * @param service tjänst
	 * @return statusmeddelande eller null
	 */
	String getStatusText(HarvestService service);

	/**
	 * Hämtar loggmedelanden för senaste körning. Denna metod hämtar bara
	 * meddelanden från minnet så om ett jobb ej körts efter uppstart kommer
	 * listan inte innehålla några meddelanden.
	 * 
	 * @param service tjänst
	 * @return lista med loggmeddelanden
	 */
	List<String> getStatusLog(HarvestService service);

	/**
	 * Hämtar en tjänsts loggmeddelandehistorik.
	 * 
	 * @param service tjänst
	 * @return lista med loggmeddelanden
	 */
	List<String> getStatusLogHistory(HarvestService service);

	/**
	 * Hämtar en tjänsts problemloggmeddelandehistorik.
	 * 
	 * @param service tjänst
	 * @param maxRows max antal loggrader
	 * @param sort sorteringskolumn
	 * @param sortDir sorteringriktning (asc/desc)
	 * @return lista med loggmeddelanden av typen varningar eller fel
	 */
	List<LogEvent> getProblemLogHistory(int maxRows, String sort, String sortDir);

	/**
	 * Sätter felmeddelande och lägger till meddelandet i tjänstens logg.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 */
	void setErrorTextAndLog(HarvestService service, String message);

	/**
	 * Hämtar felmeddelande.
	 * 
	 * @param service tjänst
	 * @return felmeddelande eller null
	 */
	String getErrorText(HarvestService service);

	/**
	 * Hämtar senaste starttid för tjänsten.
	 * 
	 * @param service tjänst
	 * @return senaste starttid som en sträng, eller null
	 */
	String getLastStart(HarvestService service);

	/**
	 * Hämtar vilket steg tjänsten befinner sig i.
	 * 
	 * @param service tjänst
	 * @return aktuellt steg
	 */
	Step getStep(HarvestService service);

	/**
	 * Sätter vilket steg tjänsten befinner sig i.
	 * 
	 * @param service tjänst
	 * @param step steg
	 */
	void setStep(HarvestService service, Step step);

	/**
	 * Hämtar vilket steg tjänsten ska börja köra ifrån.
	 * 
	 * @param service tjänst
	 * @return steg
	 */
	Step getStartStep(HarvestService service);

	/**
	 * Sätter vilket steg tjänsten ska börja köra ifrån.
	 * 
	 * @param service tjänst
	 * @param step steg
	 */
	void setStartStep(HarvestService service, Step step);

	/**
	 * Sätter flagga som talar om att det finns rdf-parsningsfel
	 * @param service tjänst
	 */
	void signalRDFError(HarvestService service);
	
	/**
	 * Ger sant om det finns rdf-parsningsfel för tjänsten
	 * @param service tjänst
	 * @return om rdf-parsningsfel har upptäckts
	 */
	boolean containsRDFErrors(HarvestService service);

}
