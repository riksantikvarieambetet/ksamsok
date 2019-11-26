package se.raa.ksamsok.harvest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Klass som hanterar hämtning/skörd av data från fil.
 */
public class SimpleHarvestJob extends HarvestJob {

	public SimpleHarvestJob() {
		super();
	}

	@Override
	protected ServiceMetadata performIdentify(HarvestService service) {
		return new ServiceMetadata(ServiceMetadata.D_TRANSIENT, ServiceMetadata.G_DAY);
	}

	@Override
	protected List<ServiceFormat> performGetFormats(HarvestService service) {
		final List<ServiceFormat> list = new ArrayList<>(1);
		list.add(new ServiceFormat("oai_dc",
				"http://www.openarchives.org/OAI/2.0/oai_dc/",
				"http://www.openarchives.org/OAI/2.0/oai_dc.xsd"));
		return list;
	}

	@Override
	protected int performGetRecords(HarvestService service,
			ServiceMetadata sm, ServiceFormat f, File storeTo, StatusService ss) throws Exception {
		int result;
		if (logger.isDebugEnabled()) {
			logger.debug(service.getId() + " - Hämtar " + service.getHarvestURL() + ", senaste hämtning: " + service.getLastHarvestDate());
		}
		byte[] buf = new byte[16384];
		int read;
		InputStream is = null;
		FileOutputStream fos = null;
		try {
			URL u = new URL(service.getHarvestURL());
			is = u.openStream();
			fos = new FileOutputStream(storeTo);
			while ((read = is.read(buf)) > 0) {
				fos.write(buf, 0, read);
			}
			fos.flush();
			// använd -1 då vi inte vet hur många poster det är
			result = -1;
		} finally {
			closeStream(is);
			closeStream(fos);
		}
		return result;
	}
}
