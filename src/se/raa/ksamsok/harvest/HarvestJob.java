package se.raa.ksamsok.harvest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.quartz.InterruptableJob;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.StatefulJob;
import org.quartz.UnableToInterruptJobException;

import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * Basklass för skördejobb.
 */
public abstract class HarvestJob implements StatefulJob, InterruptableJob {

	protected final Logger logger;
	boolean interrupted;

	protected HarvestJob() {
		logger = Logger.getLogger(this.getClass().getName());
	}

	/**
	 * Hämtar HarvestServiceManager från cron-context.
	 * 
	 * @param ctx context
	 * @return HarvestServiceManager
	 * @throws SchedulerException
	 */
	protected HarvestServiceManager getHarvestServiceManager(JobExecutionContext ctx) throws SchedulerException {
		return (HarvestServiceManager) ctx.getScheduler().getContext().get(HarvestServiceManager.HSM_KEY);
	}

	/**
	 * Hämtar HarvestRepositoryManager från cron-context.
	 * 
	 * @param ctx context
	 * @return HarvestRepositoryManager
	 * @throws SchedulerException
	 */
	protected HarvestRepositoryManager getHarvestRepositoryManager(JobExecutionContext ctx) throws SchedulerException {
		return (HarvestRepositoryManager) ctx.getScheduler().getContext().get(HarvestServiceManager.HRM_KEY);
	}

	/**
	 * Hämtar StatusService från cron-context.
	 * 
	 * @param ctx context
	 * @return StatusService
	 * @throws SchedulerException
	 */
	protected StatusService getStatusService(JobExecutionContext ctx) throws SchedulerException {
		return (StatusService) ctx.getScheduler().getContext().get(HarvestServiceManager.SS_KEY);
	}

	/**
	 * Gör (OAI-PMH) identify.
	 * 
	 * @param service tjänst
	 * @return ett värdeobjekt med metadata om skördenoden
	 * @throws Exception
	 */
	protected abstract ServiceMetadata performIdentify(HarvestService service) throws Exception;

	/**
	 * Gör (OAI-PMH) getFormats.
	 * 
	 * @param service tjänst
	 * @return lista med av skördenoden stödda format
	 * @throws Exception
	 */
	protected abstract List<ServiceFormat> performGetFormats(HarvestService service) throws Exception;

	/**
	 * Gör (OAI-PMH) getSets.
	 * 
	 * @param service tjänst
	 * @return lista med av skördenoden stödda sets.
	 * @throws Exception
	 */
	protected List<String> performGetSets(HarvestService service) throws Exception {
		return Collections.emptyList();
	}

	/**
	 * Gör (OAI-PMH) getRecords.
	 * 
	 * @param service tjänst
	 * @param sm service-metadata (från identify)
	 * @param f service-format (önskat format)
	 * @param storeTo katalog att mellanlagra i
	 * @param ss statusservice
	 * @return antal records, eller -1 om det inte kunde bestämmas
	 * @throws Exception
	 */
	protected abstract int performGetRecords(HarvestService service, ServiceMetadata sm, ServiceFormat f, File storeTo, StatusService ss) throws Exception;

	/**
	 * Ger uri för önskat metadataformat.
	 * 
	 * @return uri
	 */
	protected String getMetadataFormat() {
		return "http://www.openarchives.org/OAI/2.0/oai_dc/";
	}

	/* (non-Javadoc)
	 * @see org.quartz.InterruptableJob#interrupt()
	 */
	public void interrupt() throws UnableToInterruptJobException {
		interrupted = true;
	}

	/* (non-Javadoc)
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
	public void execute(JobExecutionContext ctx) throws JobExecutionException {
		// 1. init
		// 2. kontrollera startsteg
		// 3. om start är index kör det och sluta, annars kör identify
		// 4. om vi har en hämtad fil sen tidigare i spool ta den och gå till steg 8
		// 5. hämta och kontrollera metadataformat (getMetadataFormats)
		// 6. om vi ska hämta ett visst set, hämta stödda sets och kontrollera (getSets)
		// 7. hämta data till temp och flytta sen till spool-fil (getRecords)
		// 8. gå igenom och lagra skörd i repo (lagra undan full skörd)
		// 9. uppdatera lucene-index från repo
		// 10. klar
		interrupted = false;
		Date now = new Date();
		Timestamp nowTs = new Timestamp(now.getTime());
		Date lastSuccessfulHarvestDate; // TODO: ts eller date?
		File temp = null;
		File spoolFile = null;
		HarvestService service = null;
		StatusService ss = null;
		long start = System.currentTimeMillis();
		try {
			JobDetail jd = ctx.getJobDetail();
			HarvestServiceManager hsm = getHarvestServiceManager(ctx);
			HarvestRepositoryManager hrm = getHarvestRepositoryManager(ctx);
			ss = getStatusService(ctx);
			String serviceId = jd.getName();
			if (logger.isInfoEnabled()) {
				logger.info("Running job for " + serviceId);
			}
			service = hsm.getService(serviceId);
			ss.containsErrors(service, false);
			if (service == null) {
				throw new JobExecutionException("Could not find service with ID: " + serviceId);
			}
			// specialfall för indexering från repo
			if (ss.getStartStep(service) == Step.INDEX) {
				ss.initStatus(service, "Init");
				ss.setStatusTextAndLog(service, "Updating lucene index from repository");
				ss.setStep(service, Step.INDEX);
				hrm.updateLuceneIndex(service, null);
				hsm.storeFirstIndexDateIfNotSet(service);
				long durationMillis = System.currentTimeMillis() - start;
				ss.setStatusTextAndLog(service, "Ok, job time: " + ContentHelper.formatRunTime(durationMillis));
				ss.setStep(service, Step.IDLE);
				return;
			}
			if (ss.getStartStep(service) == Step.EMPTYINDEX) {
				ss.initStatus(service, "Init");
				ss.setStatusTextAndLog(service, "Removing lucene index for service with ID: " + serviceId);
				ss.setStep(service, Step.EMPTYINDEX);
				hrm.removeLuceneIndex(service);
				//hsm.storeFirstIndexDateIfNotSet(service);
				long durationMillis = System.currentTimeMillis() - start;
				ss.setStatusTextAndLog(service, "Ok, job time: " + ContentHelper.formatRunTime(durationMillis));
				ss.setStep(service, Step.IDLE);
				return;
			}
			ss.initStatus(service, "Init");
			ss.setStep(service, Step.FETCH);
			ss.setStatusTextAndLog(service, "Performing Identify");

			ServiceMetadata sm = performIdentify(service);

			int numRecords = -1; // -1 är okänt antal poster
			spoolFile = hrm.getSpoolFile(service);
			// kolla om vi har en hämtad fil som vi kan använda
			if (!spoolFile.exists()) {
				// ingen tidigare hämtning att använda
				ss.setStatusTextAndLog(service, "Fetching metadata format");
				List<ServiceFormat> formats = performGetFormats(service);
				String f = getMetadataFormat();
				ServiceFormat format = null;
				for (ServiceFormat sf: formats) {
					if (sf.getNamespace().equals(f)) {
						format = sf;
						break;
					}
				}
				if (format == null) {
					throw new Exception("Requested format (" + f + ") not supported");
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// kontrollera om ev önskat set stöds av tjänsten
				String setSpec = service.getHarvestSetSpec();
				if (setSpec != null) {
					boolean setSpecSupported = false;
					ss.setStatusTextAndLog(service, "Checking specified set: " + setSpec);
					List<String> setSpecs = performGetSets(service);
					for (String fetchedSetSpec: setSpecs) {
						if (setSpec.equals(fetchedSetSpec)) {
							setSpecSupported = true;
							break;
						}
					}
					if (!setSpecSupported) {
						throw new Exception("Specified set not supported: (" + setSpec + "), " + setSpecs);
					}
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// skapa tempfil
				temp = File.createTempFile(jd.getName().substring(0, Math.min(4, serviceId.length())), null);
				// hämta data till tempfilen
				ss.setStatusTextAndLog(service, "Fetching data to temp file");
				numRecords = performGetRecords(service, sm, format, temp, ss);
				if (numRecords != 0) {
					if (logger.isDebugEnabled()) {
						logger.debug(serviceId + ", Fetched " + numRecords + " records");
					}
					ss.setStatusTextAndLog(service, "Moving temp file to spool");
					if (!temp.renameTo(spoolFile)) {
						throw new Exception("Could not move temp file to spool file, " +
								temp + " -> " + spoolFile);
					}
				}
			} else {
				ss.setStatusTextAndLog(service, "Using existing file from spool");
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", using existing file from spool: " + spoolFile.getName());
				}
			}
			// om vi har records och en spool-fil ska vi bearbeta den
			if (numRecords != 0 && spoolFile.exists()) {
				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				long fsizeMb = spoolFile.length() / (1024 * 1024);
				// lagra skörd i repot
				ss.setStatusTextAndLog(service, "Storing data in repo (" + numRecords + " records, appr " +
						fsizeMb + "MB)");
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", storing data in repo (" + numRecords + " records, appr " +
						fsizeMb + "MB)");
				}
				ss.setStep(service, Step.STORE);
				boolean changed = hrm.storeHarvest(service, sm, spoolFile, nowTs);
				if (logger.isDebugEnabled()) {
					logger.debug(serviceId + ", stored records");
				}

				// arkivera fulla skördar
				// TODO: arkiveringskatalog? tråd? delta-skördar?
				if (!sm.handlesPersistentDeletes() || service.getLastHarvestDate() == null) {
					// "full skörd", arkivera
					ss.setStatusTextAndLog(service, "Archiving full harvest");
					OutputStream os = null;
					InputStream is = null;
					File of = new File(spoolFile.getAbsolutePath() + ".gz");
					byte[] buf = new byte[8192];
					int c;
					try {
						is = new BufferedInputStream(new FileInputStream(spoolFile));
						os = new GZIPOutputStream(new BufferedOutputStream(
								new FileOutputStream(of)));
						while ((c = is.read(buf)) > 0) {
							os.write(buf, 0, c);
						}
						os.flush();
					} finally {
						closeStream(is);
						closeStream(os);
					}
					ss.setStatusTextAndLog(service, "Archived full harvest to gzip (" + (of.length() / (1024*1024)) + " MB)");
				}
				// ta bort spool-filen då vi är klara med innehållet
				if (!spoolFile.delete()) {
					logger.error(serviceId + ", could not remove spool file");
					ss.setStatusTextAndLog(service, "Note: Could not remove spool file");
				}

				// TODO: är detta rätt datum/tid att sätta även om vi har återupptagit
				//       ett jobb som inte gick bra? kanske ska ta datum från spoolFile?

				// hämta senaste lyckade körning, bara om tjänsten stödjer persistent deletes
				// möjliggör omindexering av mindre mängd
				Timestamp lastSuccessfulHarvestTs = null;
				if (sm.handlesPersistentDeletes()) {
					lastSuccessfulHarvestDate = service.getLastHarvestDate();
					if (lastSuccessfulHarvestDate != null) {
						lastSuccessfulHarvestTs = new Timestamp(lastSuccessfulHarvestDate.getTime());
					}
				}
				// uppdatera senaste skörde datum/tid för servicen
				if(!ss.containsErrors(service)) {
					hsm.updateServiceDate(service, nowTs);
				}

				// kolla om vi ska avbryta
				checkInterrupt(ss, service);

				// uppdatera lucene-index för servicen
				if (changed) {
					ss.setStatusTextAndLog(service, "Updating lucene index" +
							(lastSuccessfulHarvestTs != null ?
									" > " + lastSuccessfulHarvestTs : ""));
					ss.setStep(service, Step.INDEX);
					hrm.updateLuceneIndex(service, lastSuccessfulHarvestTs);
					hsm.storeFirstIndexDateIfNotSet(service);
				} else {
					if (logger.isInfoEnabled()) {
						logger.info(serviceId + ", no index update needed");
					}
				}
				if (logger.isInfoEnabled()) {
					logger.info(serviceId + ", harvested");
				}
			} else {
				if (logger.isInfoEnabled()) {
					logger.info(serviceId + ", harvest resulted in no records");
				}
				if(!ss.containsErrors(service)) {
					// uppdatera med senaste skördetid
					hsm.updateServiceDate(service, now);
				}
			}
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Ok, job time: " +
					ContentHelper.formatRunTime(durationMillis) + ", " + numRecords + " records");
			ss.setStep(service, Step.IDLE);

			if (logger.isDebugEnabled()) {
				List<String> log = ss.getStatusLog(service);
				logger.debug(serviceId + ": ----- log summary -----");
				for (String logMsg: log) {
					logger.debug(serviceId + ": " + logMsg);
				}
			}
		} catch (Throwable e) {
			// sätt felmeddelande i statusservicen
			String errMsg = e.getMessage();
			if (errMsg == null || errMsg.length() == 0) {
				errMsg = e.toString();
			}
			if (ss != null) {
				reportError(service, "Error in job step: " + ss.getStep(service), e);
				ss.setErrorTextAndLog(service, errMsg);
				ss.setStep(service, Step.IDLE);
			} else {
				logger.error("No status service to report error to!");
				reportError(service, "Error in job execution", e);
			}
		} finally {
			if (temp != null && temp.exists()) {
				if (!temp.delete()) {
					logger.warn("Could not remove temp file: " + temp.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Kontrollerar om jobbet ska avbrytas och kastar i så fall ett exception.
	 * 
	 * @param ss statusservice
	 * @param service tjänst
	 * @throws Exception om jobb ska avbrytas
	 */
	protected void checkInterrupt(StatusService ss, HarvestService service) throws Exception {
		if (interrupted) {
			throw new Exception("Job disrupted on request");
		}
		if (ss != null) {
			ss.checkInterrupt(service);
		}
	}

	/**
	 * Rapporterar fel i loggen.
	 * 
	 * @param service tjänst
	 * @param message meddelande
	 * @param e fel eller null
	 */
	protected void reportError(HarvestService service, String message, Throwable e) {
		if (service != null) {
			logger.error(service.getId() + " - " + message, e);
		} else {
			logger.error(message, e);
		}
	}

	/**
	 * Hjälpmetod som stänger en ut-ström.
	 * 
	 * @param os ström
	 */
	protected void closeStream(OutputStream os) {
		if (os != null) {
			try {
				os.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Hjälpmetod som stänger en in-ström.
	 * 
	 * @param is ström
	 */
	protected void closeStream(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (Exception ignore) {}
		}
	}
}
