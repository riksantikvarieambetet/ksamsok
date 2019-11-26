package se.raa.ksamsok.harvest;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import se.raa.ksamsok.harvest.StatusService.Step;
import se.raa.ksamsok.lucene.ContentHelper;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Klass som kör en optimering av index i form av en tjänst/cron-jobb.
 */
public class IndexOptimizeJob extends HarvestJob {

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service) {
		return Collections.emptyList();
	}

	@Override
	protected int performGetRecords(HarvestService service, ServiceMetadata sm,
			ServiceFormat f, File storeTo, StatusService ss) {
		return 0;
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service) {
		return null;
	}

	@Override
	public void execute(JobExecutionContext ctx) {
		interrupted = false;
		StatusService ss = null;
		HarvestService service = null;
		try {
			JobDetail jd = ctx.getJobDetail();
			HarvestServiceManager hsm = getHarvestServiceManager(ctx);
			HarvestRepositoryManager hrm = getHarvestRepositoryManager(ctx);
			ss = getStatusService(ctx);
			String serviceId = jd.getKey().getName();
			if (logger.isInfoEnabled()) {
				logger.info("Running job to optimize index (" + serviceId + ")");
			}
			service = hsm.getService(serviceId);
			boolean hasService = (service != null);
			if (!hasService) {
				service = new HarvestServiceImpl();
				service.setId(serviceId);
				service.setName("Temp job for index optimization");
			}
			ss.initStatus(service, "Init");
			ss.setStep(service, Step.INDEX);
			ss.setStatusTextAndLog(service, "Starting index optimization");
			long start = System.currentTimeMillis();
			hrm.optimizeIndex();
			long durationMillis = System.currentTimeMillis() - start;
			ss.setStatusTextAndLog(service, "Index optimization performed in " +
					ContentHelper.formatRunTime(durationMillis));
			ss.setStep(service, Step.IDLE);
			// uppdatera bara om vi har en tjänst med inskickat id, annars är det en engångskörning
			if (hasService) {
				hsm.updateServiceDate(service, new Date());
			}
			if (logger.isDebugEnabled()) {
				List<String> log = ss.getStatusLog(service);
				logger.debug(serviceId + ": ----- log summary -----");
				for (String logMsg: log) {
					logger.debug(serviceId + ": " + logMsg);
				}
			}

		} catch (Exception e) {
			String errMsg = e.getMessage();
			if (errMsg == null || errMsg.length() == 0) {
				errMsg = e.toString();
			}
			if (ss != null) {
				reportError(service, "Error when running job i step " + ss.getStep(service), e);
				ss.setErrorTextAndLog(service, errMsg);
				ss.setStep(service, Step.IDLE);
			} else {
				logger.error("No status service to report errors towards!");
				reportError(service, "Error when running job", e);
			}
		}
	}

	
}
