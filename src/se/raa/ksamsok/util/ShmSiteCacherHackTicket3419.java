package se.raa.ksamsok.util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import se.raa.ksamsok.harvest.HarvestService;

/**
 * Hack för att gringgå problem med stora rdf:er som har många relationer, specifikt
 * shm/site/56171 som har över 200k st.
 * Kopplad till #3419 och hela denna klass kan/ska tas bort när den ticketen är överspelad.
 * Cachen rensas när skördedatum för tjänsten för shm site uppdateras, alternativt
 * kan det göras manuellt genom att anropa resolverservlet med parametern clear_cache=true,
 * exvis http://kulturarvsdata.se/ksamsok/?clear_cache=true, eller tex
 * http://kulturarvsdata.se/ksamsok/raa/fmi/123?clear_cache=true
 * Kringla identifierar sig fn för detta med parametern kringla=true vid sökningar etc så för
 * att simulera ett kringla-anrop och se det kringla ser lägg på den parametern, exvis
 * http://kulturarvsdata.se/ksamsok/shm/site/56171?kringla=true
 *
 */
public class ShmSiteCacherHackTicket3419 {

	private static final Logger logger = Logger.getLogger(ShmSiteCacherHackTicket3419.class);
	private static final Map<String,String> cache = new HashMap<String, String>();

	public static final String KRINGLA = "kringla";
	public static final String CLEAR_CACHE = "clear_cache";

	/**
	 * Om cachen ska användas. Kringla-parametern måste vara sann och uri:n måste
	 * matcha objekt man vill cacha.
	 * @param kringlaParam sant om det är kringla som frågar
	 * @param uri uri
	 * @return sant om cachen ska användas
	 */
	public static boolean useCache(String kringlaParam, String uri) {
		return "true".equals(kringlaParam) && uri != null &&
				uri.endsWith("shm/site/56171");
	}

	/**
	 * Rensar cache för om tjänsten shm/site skickas in. Skörde-URL:en används för att
	 * avgöra om det är rätt tjänst.
	 * @param service tjänst
	 */
	public static void clearCache(HarvestService service) {
		if (service != null && service.getHarvestURL() != null &&
				service.getHarvestURL().trim().toLowerCase().endsWith("shm/site")) {
			clearCache();
		}
	}

	/**
	 * Rensar cache om argumentet är "true", används för att manuellt kunna trigga
	 * en cacherensning.
	 * @param clearCache
	 */
	public static void clearCache(String clearCache) {
		if ("true".equals(clearCache)) {
			clearCache();
		}
	}

	// rensar ovillkorligen cachen
	private static void clearCache() {
		logger.warn("Clearing shm site cache");
		synchronized (cache) {
			cache.clear();
		}
	}

	/**
	 * Hämtar data från cachen, eller strippar (utvalda) relationer från
	 * rdf och pres och cachar sen resultatet.
	 * @param uri uri
	 * @param xmlContent xml-data
	 * @return strippad rdf
	 * @throws UnsupportedEncodingException
	 */
	public static String getOrRecache(String uri, byte[] xmlContent) throws UnsupportedEncodingException {
		synchronized (cache) {
			String content = cache.get(uri);
			if (content != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Returning cached content for " + uri);
				}
				return content;
			}
			content = new String(xmlContent, "UTF-8");
			logger.warn("Recaching and stripping stuff for " + uri + ", size=" + content.length());
			content = content.replaceAll("<pres:reference>.*</pres:reference>", "");
			logger.warn("after pres:reference removal, size=" + content.length());
			content = content.replaceAll("<hasPart>.*</hasPart>", "");
			logger.warn("after hasPart removal, size=" + content.length());
			content = content.replaceAll("<hasFind>.*</hasFind>", "");
			logger.warn("after hasFind removal, size=" + content.length());
			content = content.replaceAll("<isPartOf>.*</isPartOf>", "");
			logger.warn("after isPartOf removal, size=" + content.length());
			content = content.replaceAll("<isDescribedBy>.*</isDescribedBy>", "");
			logger.warn("after isDescribedBy removal, size=" + content.length());

			cache.put(uri,  content);
			return content;
		}
	}

}
