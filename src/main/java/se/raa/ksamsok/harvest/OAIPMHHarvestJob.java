package se.raa.ksamsok.harvest;

import org.oclc.oai.harvester2.verb.Identify;
import org.oclc.oai.harvester2.verb.ListMetadataFormats;
import org.oclc.oai.harvester2.verb.ListRecords;
import org.oclc.oai.harvester2.verb.ListSets;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.raa.ksamsok.lucene.ContentHelper;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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
	 * 
	 * @param maxTries max antal försök
	 * @param waitSecs sekunder att vänta mellan varje försök
	 */
	OAIPMHHarvestJob(int maxTries, int waitSecs) {
		this.maxTries = maxTries;
		this.waitSecs = waitSecs;
	}

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service) throws Exception {
		final List<ServiceFormat> list = new ArrayList<>();
		ListMetadataFormats formats = new ListMetadataFormats(service.getHarvestURL());
		NodeList nodes = formats.getNodeList("/oai20:OAI-PMH/oai20:ListMetadataFormats/oai20:metadataFormat");
		ServiceFormat f;
		for (int i = 0; i < nodes.getLength(); ++i) {
			Node n = nodes.item(i);
			f = new ServiceFormat(formats.getSingleString(n, "oai20:metadataPrefix"),
				formats.getSingleString(n, "oai20:metadataNamespace"), formats.getSingleString(n, "oai20:schema"));
			list.add(f);
		}
		return list;
	}

	@Override
	protected List<String> performGetSets(HarvestService service) throws Exception {
		final List<String> list = new ArrayList<>();
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
	protected ServiceMetadata performIdentify(HarvestService service) throws Exception {
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
			logger.info(service.getId() + ", deletedRecord=" + deletedRecord + ", granularity=" + granularity);
		}
		return new ServiceMetadata(deletedRecord, granularity);
	}

	@Override
	protected int performGetRecords(HarvestService service, ServiceMetadata sm, ServiceFormat f, File storeTo,
		StatusService ss) throws Exception {
		if (logger.isInfoEnabled()) {
			logger.info(service.getId() + " - Fetching " + service.getHarvestURL() + ", latest fetch: " +
				service.getLastHarvestDate());
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
				// intressant för datum
				// kontrollera om timezone-användningen är korrekt map datumgränser mm
				// url-kodning av timestamp behövs om tid är inblandat också
				String fromDateStr = df.format(service.getLastHarvestDate());
				fromDate = URLEncoder.encode(fromDateStr, StandardCharsets.UTF_8);
				if (ss != null) {
					ss.setStatusTextAndLog(service, "Fetching changes since latest harvest (" + fromDateStr + ")");
				}
			}
			os = new BufferedOutputStream(new FileOutputStream(storeTo));
			return getRecords(service.getHarvestURL(), fromDate, null, f.getPrefix(), service.getHarvestSetSpec(), os,
				logger, ss, service);
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
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix, String setSpec,
		OutputStream os, Logger logger) throws Exception {
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
	public int getRecords(String url, String fromDate, String toDate, String metadataPrefix, String setSpec,
		OutputStream os, Logger logger, StatusService ss, HarvestService service) throws Exception {
		int c = 0;
		int tryNum = 0;
		int completeListSize = -1;
		long start = System.currentTimeMillis();
		String resumptionToken = null;
		String serviceId = (service != null ? service.getId() : "Unknown service") + ": ";
		if (logger == null) {
			logger = LogManager.getLogger(OAIPMHHarvestJob.class);
		}
		try {
			try {
				os.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes(StandardCharsets.UTF_8));
				os.write("<harvest>\n".getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
					logger.error(serviceId + "Det är problem med att skriva till ut-strömmen");
				throw e;
			}

			ListRecords listRecords = null;
			while (listRecords == null) {
				++tryNum;
				try {
					listRecords = new ListRecords(url, fromDate, toDate, setSpec, metadataPrefix);
				} catch (IOException e) {
					failedTry(tryNum, e, ss, service);
				}
			}
			while (listRecords != null) {
				// kolla om vi ska avbryta
				logger.debug(serviceId + "In start of while loop, checking for interrupt signal");
				checkInterrupt(ss, service);// TODO

				logger.debug(serviceId + "Checking for errors in records");
				NodeList errors = listRecords.getErrors();
				if (errors != null && errors.getLength() > 0) {
					// inga records är inte ett "fel" egentligen
					if ("noRecordsMatch".equals(errors.item(0).getAttributes().getNamedItem("code").getNodeValue())) {
						logger.debug(serviceId + "Found noRecordsMatch - this is not an error");
						c = 0;
						break;
					}
						logger.error(serviceId + "Found " + errors.getLength() + " errors");
					throw new Exception(listRecords.toString());
				}
				try {
					logger.debug(serviceId + "About to write listRecords to outputStream");

					// filtrera bort xsi:schemaLocation som vår xmlParser inte hanterar väl
					String listRecordsString = listRecords.toString();
					listRecordsString = StringUtils.remove(listRecordsString, "xsi:schemaLocation=\"http://www.w3.org/1999/02/22-rdf-syntax-ns# http://www.openarchives.org/OAI/2.0/rdf.xsd\"");
					
					os.write(listRecordsString.getBytes(StandardCharsets.UTF_8));
					logger.debug(serviceId + "Done writing listRecords to outputStream");
					os.write("\n".getBytes(StandardCharsets.UTF_8));
				} catch (IOException e) {
						logger.error(serviceId + "Det är problem med att skriva till ut-strömmen");
					throw e;
				}
				// om token är "" betyder det ingen resumption
				logger.debug(serviceId + "Getting resumption token");
				resumptionToken = listRecords.getResumptionToken();
				logger.debug(serviceId + "Found resumption token");
				// hämta totala antalet (om det skickas) fast bara första gången
				if (completeListSize < 0 && c == 0 && StringUtils.isNotBlank(resumptionToken)) {
					try {
						logger.debug(serviceId + "Getting completeListSize");
						completeListSize = Integer.parseInt(listRecords.getSingleString(
							"/oai20:OAI-PMH/oai20:ListRecords/oai20:resumptionToken/@completeListSize"));
					} catch (Exception e) {
						logger.error(serviceId + "Error when fetching completeListSize", e);
					}
				}
				// räkna antal
				logger.debug(serviceId + "Getting number of records");
				c += Integer.parseInt(
					listRecords.getSingleString("count(/oai20:OAI-PMH/oai20:ListRecords/oai20:record)"));
				// beräkna ungefär kvarvarande hämtningstid
				logger.debug(serviceId + "Calculating remaining time");
				long deltaMillis = System.currentTimeMillis() - start;
				long aproxMillisLeft = ContentHelper.getRemainingRunTimeMillis(deltaMillis, c, completeListSize);

				if (logger.isInfoEnabled()) {
					logger.info(serviceId + "fetched " + c +
						(completeListSize > 0 ? "/" + completeListSize : "") + " records so far in " +
						ContentHelper.formatRunTime(deltaMillis) +
						(aproxMillisLeft >= 0
							? " (estimated time remaining: " + ContentHelper.formatRunTime(aproxMillisLeft) + ")"
							: "") +
						", resumptionToken: " + resumptionToken);
				}
				if (ss != null) {
					
					// vi uppdaterar bara status här, logg är inte intressant för dessa
					logger.debug(serviceId + "Updating status in ss");
					ss.setStatusText(service,
						"Fetching data to temp file (fetched " + c +
							(completeListSize > 0 ? "/" + completeListSize : "") + " records)" + (aproxMillisLeft >= 0
								? ", estimated time remaining: " + ContentHelper.formatRunTime(aproxMillisLeft)
								: ""));
				}
				if (resumptionToken == null || resumptionToken.length() == 0) {
						logger.info(serviceId + "No resumption, harvest done");
					listRecords = null;
				} else {
					listRecords = null;
					tryNum = 0;
					while (listRecords == null) {
						++tryNum;
							logger.info(serviceId + "Trying, attempt " + tryNum +
								" resumption with token " + resumptionToken);
						try {
							listRecords = new ListRecords(url, resumptionToken);
							logger.debug(serviceId + "Returned from listing records");
						} catch (IOException e) {
							failedTry(tryNum, e, ss, service);
						}
					}
				}
			}
			try {
				os.write("</harvest>\n".getBytes(StandardCharsets.UTF_8));
				os.flush();
			} catch (IOException e) {
					logger.error(serviceId + "Det är problem med att skriva till ut-strömmen");
				throw e;
			}
			long durationMillis = System.currentTimeMillis() - start;
			String msg = "Fetched " + c + " records, time: " + ContentHelper.formatRunTime(durationMillis) + " (" +
				ContentHelper.formatSpeedPerSec(c, durationMillis) + ")";
			if (ss != null) {
				ss.setStatusTextAndLog(service, msg);
			}

				logger.info(serviceId + msg);
		} catch (UnsupportedEncodingException e) {
				logger.error(serviceId + "Det är problem med strängkonvertering");
			throw e;
		} catch (IOException e) {
			// the failedTry method is designed to be called from within a loop, not like it was here
				// logg this with exception specifically here since we want to understand better when and why this happens
				logger.error(serviceId + "Unhandled IOException caught in OAIPMHHarvestjob#getRecords", e);
			throw e;
		} catch (ParserConfigurationException e) {
				logger.error(serviceId + "Det är problem med att initiera oai-pmh parser");
			throw e;
		} catch (SAXException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			String message = "Det är problem att parsa skördningen\n";
			message = message + "Url: " + url + ", metadataPrefix=" + metadataPrefix + ", fromDate:" + fromDate +
				", toDate: " + toDate + ", resumptionToken=" + resumptionToken + "\n";
			message = message + baos.toString(StandardCharsets.UTF_8);
			if (ss != null) {
				ss.setErrorTextAndLog(service, message);
			}
			throw e;
		} catch (TransformerException e) {
				logger.error(serviceId + "Det är problem med att initiera oai-pmh transformern");
			throw e;
		} catch (NoSuchFieldException e) {
				logger.error(serviceId + "Hittade inget resumption token");
			throw e;
		}
		return c;
	}

	// hantering av flera försök med viss tid mellan varje försök
	private void failedTry(int tryNum,  IOException ioe, StatusService ss,
		HarvestService service) throws Exception {
		// TODO: bättre konstanter/värden
		// skilj på connect/error?
		// olika värden per tjänst? smh/va är helt tillståndslösa, oiacat inte
		if (tryNum >= maxTries) {
			String msg = "Problem when contacting the service, surrendered after " + maxTries + " tries";
			if (logger != null) {
				logger.error(msg);
			}
			throw new Exception(msg, ioe);
		}
		// TODO: kan message vara så pass stor så att 4k-gränsen överskrids och ger nedanstående
		// databasfel?
		// java.sql.SQLException: ORA-01461: can bind a LONG value only for insert into a LONG
		// column
		// i så fall måste vi begränsa feltexten, se:
		// http://vsadilovskiy.wordpress.com/2007/10/19/ora-01461-can-bind-a-long-value-only-for-insert-into-a-long-column/
		String msg = "Exception (" + ioe.getMessage() + "), waiting " + waitSecs + " seconds and trying again";
		if (logger != null) {
			logger.warn((service != null ? service.getId() + ": " : "") + msg);
		}
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
}
