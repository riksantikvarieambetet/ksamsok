package se.raa.ksamsok.harvest;

import java.util.Date;

/**
 * Böna för en skördetjänst.
 */
public interface HarvestService {

	/**
	 * Ger tjänstens id.
	 * 
	 * @return ett id
	 */
	String getId();

	/**
	 * Sätter id.
	 * 
	 * @param serviceId id
	 */
	void setId(String serviceId);

	/**
	 * Ger cronsträng (körschema).
	 * 
	 * @return cronsträng
	 */
	String getCronString();

	/**
	 * Sätter cronsträng (körschema).
	 * 
	 * @param cronString cronsträng
	 */
	void setCronString(String cronString);

	/**
	 * Ger tjänstens namn.
	 * 
	 * @return tjänstens namn
	 */
	String getName();

	/**
	 * Sätter tjänstens namn.
	 * 
	 * @param name namn
	 */
	void setName(String name);

	/**
	 * Ger skörde-URL.
	 * 
	 * @return url till skörd
	 */
	String getHarvestURL();

	/**
	 * Sätter skörde-URL.
	 * 
	 * @param harvestURL url
	 */
	void setHarvestURL(String harvestURL);

	/**
	 * Ger datum/tid för senast lyckade skörd.
	 * 
	 * @return datum
	 */
	Date getLastHarvestDate();

	/**
	 * Sätter datum/tid för senast lyckade skörd.
	 * 
	 * @param date datum
	 */
	void setLastHarvestDate(Date date);

	/**
	 * Ger datum/tid för första lyckade indexeringen.
	 * 
	 * @return datum
	 */
	Date getFirstIndexDate();

	/**
	 * Sätter datum/tid för första lyckade indexeringen.
	 * 
	 * @param date datum
	 */
	void setFirstIndexDate(Date date);

	/**
	 * Ger tjänstetyp som talar om vad denna tjänst klarar av att skörda.
	 * 
	 * @return tjänstetyp
	 */
	String getServiceType();

	/**
	 * Sätter tjänstetyp.
	 * 
	 * @param type typ
	 */
	void setServiceType(String type);

	/**
	 * Ger om man för denna tjänst alltid ska skörda allt och aldrig försöka att göra
	 * en inkrementell skörd.
	 * 
	 * @return sant om man alltid ska skörda allt
	 */
	boolean getAlwaysHarvestEverything();

	/**
	 * Sätter värde för att alltid skörda allt.
	 * 
	 * @param value sant/falskt
	 */
	void setAlwaysHarvestEverything(boolean value);

	/**
	 * Hämtar namn på det set (delmängd) som ska skördas för denna tjänst.
	 * 
	 * @return setnamn eller null
	 */
	String getHarvestSetSpec();

	/**
	 * Sätter namn på set (delmängd) som ska användas vid skörd av denna tjänst.
	 * 
	 * @param setSpec setnamn
	 */
	void setHarvestSetSpec(String setSpec);
	
	/**
	 * Returnerar kortnamn som används för att koppla tjänst till organisation
	 * @return
	 */
	String getShortName();
	
	/**
	 * Sätter kortnamn för tjänst
	 * @param shortName kortnamn
	 */
	void setShortName(String shortName);
	
	/**
	 * Returnerar pausad status.
	 * @return
	 */
	boolean getPaused();
	
	/**
	 * Sätter pausad för tjänst
	 * @param pausad motsvarande boolean.
	 */
	void setPaused(boolean paused);
}
