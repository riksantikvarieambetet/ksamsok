package se.raa.ksamsok.harvest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import se.raa.ksamsok.lucene.ContentHelper;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import ORG.oclc.oai.harvester2.verb.ListSets;

/**
 * Basklass för att hantera skörd mha OAI-PMH-protokollet.
 */
public class OAIPMHHarvestJob extends HarvestJob {

	// provar nåt snällare för Tekniska Museets skull:
	private static final int MAX_TRIES = 6;
	private static final int WAIT_SECS = 100;

	/** max antal försök */
	protected final int maxTries;
	/** sekunder att vänta mellan varje försök */
	protected final int waitSecs;

	/**
	 * Skapa ny instans med default antal hämtningsförsök och väntetid.
	 */
	public OAIPMHHarvestJob() {
		this(MAX_TRIES, WAIT_SECS);
	}

	/**
	 * Skapa med specifika värden (främst för test för att slippa vänta)
	 * @param maxTries max antal försök
	 * @param waitSecs sekunder att vänta mellan varje försök
	 */
	OAIPMHHarvestJob(int maxTries, int waitSecs) {
		this.maxTries = maxTries;
		this.waitSecs = waitSecs;
	}

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service)
			throws Exception {
		final List<ServiceFormat> list = new ArrayList<ServiceFormat>();
		ListMetadataFormats formats = new ListMetadataFormats(service.getHarvestURL());
		NodeList nodes = formats.getNodeList("/oai20:OAI-PMH/oai20:ListMetadataFormats/oai20:metadataFormat");
		ServiceFormat f;
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node n = nodes.item(i);
			f = new ServiceFormat(formats.getSingleString(n, "oai20:metadataPrefix"),
					formats.getSingleString(n, "oai20:metadataNamespace"),
					formats.getSingleString(n, "oai20:schema"));
			list.add(f);
		}
		return list;
	}

	@Override
	protected List<String> performGetSets(HarvestService service)
			throws Exception {
		final List<String> list = new ArrayList<String>();
		ListSets sets = new ListSets(service.getHarvestURL());
		NodeList nodes = sets.getNodeList("/oai20:OAI-PMH/oai20:ListSets/oai20:set");
		String setSpec;
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node n = nodes.item(i);
			setSpec = sets.getSingleString(n, "oai20:setSpec");
			list.add(setSpec);
		}
		return list;
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service)
			throws Exception {
		Identify identify = new Identify(service.getHarvestURL());
		if (!"2.0".equals(identify.getProtocolVersion())) {
			throw new Exception("Service does not specify 2.0 but " + identify.getProtocolVersion());
		}
		String granularity = identify.getSingleString("/oai20:OAI-PMH/oai20:Identify/oai20:granularity");
		if (granularity == null) {
			throw new Exception("Could not find granularity");
		}
		String deletedRecord = identify.getSingleString("/oai20:OAI-PMH/oai20:Identify/oai20:deletedRecord");
		if (deletedRecord == null) {
			throw new Exception("Could not find deletedRecord");
		}
		if (service.getAlwaysHarvestEverything()) {
			deletedRecord = ServiceMetadata.D_TRANSIENT;
			if (logger.isInfoEnabled()) {
				logger.info(service.getId() + ", Forcing deletedRecord=" + deletedRecord);
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info(service.getId() + ", deletedRecord=" + deletedRecord +
					", granularity=" + granularity);
		}
		return new ServiceMetadata(deletedRecord, granularity);
	}

	@Override
	protected int performGetRecords(HarvestService service, ServiceMetadata sm, ServiceFormat f,
			File storeTo, StatusService ss) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(service.getId() + " - Fetching " + service.getHarvestURL() + ", latest fetch: " + service.getLastHarvestDate());
		}
		OutputStream os = null;
		try {
			String fromDate = null;
			// bara om tjänsten (permanent) hanterar info om borttagna
			if (sm.handlesPersistentDeletes() && service.getLastHarvestDate() != null) {
				// YYYY-MM-DDThh:mm:ssZ eller YYYY-MM-DD
				SimpleDateFormat df = new SimpleDateFormat(sm.getDateFormatString());
				df.setTimeZone(TimeZone.getTimeZone("UTC"));
				// TODO: öka/minska på datum eller tid då from/tom är inclusive? kanske bara
				//       intressant för datum
				//       kontrollera om timezone-användningen är korrekt map datumgränser mm
				// url-kodning av timestamp behövs om tid är inblandat också
				String fromDateStr = df.format(service.getLastHarvestDate());
				fromDate = URLEncoder.encode(fromDateStr, "UTF-8");
				if (ss != null) {
					ss.setStatusTextAndLog(service, "Fetching changes since latest harvest (" +
							fromDateStr + ")");
				}
			}
			os = new BufferedOutputStream(new FileOutputStream(storeTo));
			return getRecords(service.getHarvestURL(), fromDate, null, f.getPrefix(),
					service.getHarvestSetSpec(), os, logger, ss, service);
		} finally {
			closeStream(os);
		}
	}

	/**
	 * Utför en hämtning/skörd av data via oai-pmh-protokollet.
	 * 
	 * @param url skörde-url
	 * @param fromDate from eller null
	 * @param toDate tom eller null
	 * @param metadataPrefix metadataprefix
	 * @param setSpec set eller null
	 * @param os ström att skriva till
	 * @param logger logger eller null
	 * @return antal hämtade poster
	 * @throws Exception
	 */
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix,
			String setSpec, OutputStream os, Logger logger) throws Exception {
		return getRecords(url, fromDate, toDate, metadataPrefix, setSpec, os, logger, null, null);
	}

	/**
	 * Utför en hämtning/skörd av data via oai-pmh-protokollet.
	 * 
	 * @param url skörde-url
	 * @param fromDate from eller null
	 * @param toDate tom eller null
	 * @param metadataPrefix metadataprefix
	 * @param setSpec set eller null
	 * @param os ström att skriva till
	 * @param logger logger eller null
	 * @param ss statusservice eller null
	 * @param service tjänst eller null
	 * @return antal hämtade poster
	 * @throws Exception
	 */
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix,
			String setSpec, OutputStream os, Logger logger, StatusService ss, HarvestService service) throws Exception {
		int c = 0;
		int tryNum = 0;
		int completeListSize = -1;
		long start = System.currentTimeMillis();
		os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
		os.write("<harvest>\n".getBytes("UTF-8"));
        ListRecords listRecords = null;
       	while (listRecords == null) {
       		++tryNum;
       		try {
        		listRecords = new ListRecords(url, fromDate, toDate, setSpec, metadataPrefix);
            } catch (IOException ioe) {
            	failedTry(tryNum, null, ioe, ss, service);
        	}
        }
        while (listRecords != null) {
			// kolla om vi ska avbryta
        	checkInterrupt(ss, service);

            NodeList errors = listRecords.getErrors();
            if (errors != null && errors.getLength() > 0) {
            	// inga records är inte ett "fel" egentligen
            	if ("noRecordsMatch".equals(errors.item(0).getAttributes().getNamedItem("code").getNodeValue())) {
            		c = 0;
            		break;
            	}
            	if (logger != null) {
            		logger.error("Found " + errors.getLength() + " errors");
            	}
                throw new Exception(listRecords.toString());
            }
            os.write(listRecords.toString().getBytes("UTF-8"));
            os.write("\n".getBytes("UTF-8"));
            // om token är "" betyder det ingen resumption
            String resumptionToken = listRecords.getResumptionToken();
            // hämta totala antalet (om det skickas) fast bara första gången
            if (completeListSize < 0 && c == 0 && StringUtils.isNotBlank(resumptionToken)) {
            	try {
            		completeListSize = Integer.parseInt(listRecords.getSingleString(
            				"/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken/@completeListSize"));
            	} catch (Exception e) {
            		if (logger != null && logger.isDebugEnabled()) {
            			logger.debug("Error when fetching completeListSize", e);
            		}
            	}
            }
            // räkna antal
            c += Integer.parseInt(listRecords.getSingleString("count(/oai20:OAI-PMH/oai20:ListRecords/oai20:record)"));
            // beräkna ungefär kvarvarande hämtningstid
            long deltaMillis = System.currentTimeMillis() - start;
            long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(
            		deltaMillis, c, completeListSize);

            if (logger != null && logger.isDebugEnabled()) {
            	logger.debug((service != null ? service.getId() + ": " : "" ) +
            			"fetched " + c + (completeListSize > 0 ? "/" + completeListSize : "")
            			+ " records so far in " + ContentHelper.formatRunTime(deltaMillis) +
            			(aproxMillisLeft >= 0 ? " (estimated time remaining: " +
            					ContentHelper.formatRunTime(aproxMillisLeft) + ")": "") +
            			", resumptionToken: " + resumptionToken);
            }
			if (ss != null) {

				// vi uppdaterar bara status här, logg är inte intressant för dessa
				ss.setStatusText(service, "Fetching data to temp file (fetched " + c +
						(completeListSize > 0 ? "/" + completeListSize : "") + " records)" +
						(aproxMillisLeft >= 0 ? ", estimated time remaining: " +
								ContentHelper.formatRunTime(aproxMillisLeft) : ""));
			}
            if (resumptionToken == null || resumptionToken.length() == 0) {
                listRecords = null;
            } else {
            	listRecords = null;
            	tryNum = 0;
            	while (listRecords == null) {
            		++tryNum;
	            	try {
	            		listRecords = new ListRecords(url, resumptionToken);
	            	} catch (IOException ioe) {
	            		failedTry(tryNum, resumptionToken, ioe, ss, service);
	            	}
            	}
            }
        }
        os.write("</harvest>\n".getBytes("UTF-8"));
        os.flush();
    	long durationMillis = System.currentTimeMillis() - start;
    	String msg = "Fetched " + c + " records, time: " +
    		ContentHelper.formatRunTime(durationMillis) +
    		" (" + ContentHelper.formatSpeedPerSec(c, durationMillis) + ")";
    	if (ss != null) {
    		ss.setStatusTextAndLog(service, msg);
    	}

        if (logger != null && logger.isInfoEnabled()) {
        	logger.info((service != null ? service.getId() + ": " : "" ) + msg);
        }
        return c;
	}

	// hantering av flera försök med viss tid mellan varje försök
	private void failedTry(int tryNum, String resumptionToken, IOException ioe, StatusService ss, HarvestService service) throws Exception {
    	// TODO: bättre konstanter/värden
    	//       skilj på connect/error?
    	//       olika värden per tjänst? smh/va är helt tillståndslösa, oiacat inte 
		if (tryNum >= maxTries) {
			throw new Exception("Problem when contacting the service, surrendered after " + maxTries + " tries" + (resumptionToken != null ? " with token: " +
					resumptionToken : ""), ioe);
		}
		// TODO: kan message vara så pass stor så att 4k-gränsen överskrids och ger nedanstående databasfel?
		//       java.sql.SQLException: ORA-01461: can bind a LONG value only for insert into a LONG column
		//       i så fall måste vi begränsa feltexten, se:
		//       http://vsadilovskiy.wordpress.com/2007/10/19/ora-01461-can-bind-a-long-value-only-for-insert-into-a-long-column/
		String msg = "Exception (" + ioe.getMessage() +
			"), waiting " + waitSecs + " seconds and trying again";
		logger.warn((service != null ? service.getId() + ": " : "" ) + msg);
		if (ss != null) {
    		ss.setStatusTextAndLog(service, msg);
		}
		// sov totalt waitSecs sekunder, men se till att vi är snabba på att avbryta
		for (int i = 0; i < waitSecs; ++i) {
			Thread.sleep(1000);
        	if (ss != null) {
        		checkInterrupt(ss, service);
        	}
		}

	}

	public static void main(String[] args) {
		Logger logger = Logger.getLogger("se.raa.ksamsok.harvest.OAIPMHHarvestJob");
		FileOutputStream fos = null;
		OAIPMHHarvestJob j = new OAIPMHHarvestJob();
		long start = System.currentTimeMillis();
		try {
			/*
			fos = new FileOutputStream(new File("d:/temp/oaipmh.xml"));
			j.getRecords("http://alcme.oclc.org/oaicat/OAIHandler", null, null, "oai_dc", null, fos, logger);
			*/
			/*
			fos = new FileOutputStream(new File("d:/temp/kthdiva.xml"));
			j.getRecords("http://www.diva-portal.org/oai/kth/OAI", null, null, "oai_dc", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/sudiva.xml"));
			j.getRecords("http://www.diva-portal.org/oai/su/OAI", null, null, "oai_dc", null, fos, logger);
			*/
			
			/* funkar ej ok - otroooligt seg i alla fall?
			fos = new FileOutputStream(new File("d:/temp/usc.xml"));
			j.getRecords("http://oai.usc.edu:8085/oaidp", null, null, "oai_dc", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/brighton.xml"));
			j.getRecords("http://eprints.brighton.ac.uk/perl/oai2", null, null, "oai_dc", null, fos, logger,);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/nils1.xml"));
			j.getRecords("http://172.20.6.106:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/utvnod.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			fos = new FileOutputStream(new File("d:/temp/utvnod_kmb.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "kmb", fos, logger);

			/*
			fos = new FileOutputStream(new File("d:/temp/utvnod_big2.xml"));
			j.getRecords("http://ux-ra-utvap.raa.se:8081/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi_big", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/lokal_fmi2.xml"));
			j.getRecords("http://127.0.0.1:8080/oaicat/OAIHandler", null, null, "ksamsok-rdf", "fmi", fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/shm_context.xml"));
			j.getRecords("http://mis.historiska.se/OAICat/SHM/context", null, null, "ksamsok-rdf", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/shm_media.xml"));
			j.getRecords("http://mis.historiska.se/OAICat/SHM/media", null, null, "ksamsok-rdf", null, fos, logger);
			*/

			/*
			fos = new FileOutputStream(new File("d:/temp/va_gnm_media.xml"));
			j.getRecords("http://www9.vgregion.se/vastarvet/OAICat/gnm/media", null, null, "ksamsok-rdf", null, fos, logger);
			*/

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception ignore) {}
			}
		}
		long durationMillis = System.currentTimeMillis() - start;
		System.out.println("Time: " + ContentHelper.formatRunTime(durationMillis));
	}

}
