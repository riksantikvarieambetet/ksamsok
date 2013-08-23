package se.raa.ksamsok.ugchub;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import se.raa.ksamsok.util.HttpService;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class UGCHubManager extends HttpService {

	private @Autowired ApplicationContext appContext;
	private static final Logger logger = Logger.getLogger(UGCHubManager.class);
	//TODO !! variabler skall få sina värden från apiconnect.properties via applicationContext.xml
	private String scheme;// = "http";
	private String host;// = "lx-ra-ugchubtest:8080";
	private String path;// = "/UGC-hub/api";
	private String key;// = "st4609gu77";
	private String method;// = "retrieve";
	private String objectUri;// = "all";
	private String scope;// = "all";
	private String format;// = "json";
	private int limit = 20;
	private int pageDisp = 5;
	private String apiUri; 
	private String apiCount;
	
	public UGCHubManager() {
		apiUri = scheme + "://" + host + path + "?x-api=" + key + "&method=" + method + "&objectUri=" + objectUri + "&scope=" + scope + "&format=" + format;
		apiCount = scheme + "://" + host + path + "?x-api=" + key + "&method=" + method + "&objectUri=" + objectUri + "&scope=" + "count" + "&format=" + format;
	}
	
	/*init and destroy methods ärvs från se.raa.ksamsok.util.HttpService*/

	/**
	 * Gets all ugchub-objects.
	 * @return a list of ugchub-objects.
	 * @throws Exception
	 */
	public List<UGCHub> getUGCHubs() throws Exception {
		Gson gson = new Gson();
		
		String jsonResponse = basicJsonQuery(apiUri);
		jsonResponse = jsonResponse.substring(jsonResponse.indexOf("["), jsonResponse.lastIndexOf("]") + 1);
		List<UGCHub> ugcHubs = gson.fromJson(jsonResponse, new TypeToken<List<UGCHub>>(){}.getType());
		
		return ugcHubs;
	}
	
	/**
	 * Gets ugchub-objects, as specified by limit and offset.
	 * @param limit
	 * @param offset
	 * @return a list of ugchub-objects limited by parameters limit and offset.
	 * @throws Exception
	 */
	public List<UGCHub> getUGCHubsPaginate(String limit, String offset) throws Exception {
		Gson gson = new Gson();
		apiUri = apiUri + "&maxCount=" + limit + "&selectFrom=" + offset;

		String jsonResponse = basicJsonQuery(apiUri);
		jsonResponse = jsonResponse.substring(jsonResponse.indexOf("["), jsonResponse.lastIndexOf("]") + 1);
		List<UGCHub> ugcHubs = gson.fromJson(jsonResponse, new TypeToken<List<UGCHub>>(){}.getType());
		
		return ugcHubs;
	}
	
	/**
	 * Gets the total count of ugchub-object.
	 * @return int, number of objects.
	 * @throws Exception
	 */
	public int getCount() throws Exception {
		JsonParser parser = new JsonParser();
		
		String jsonResponse = basicJsonQuery(apiCount);
		
		JsonObject jo = parser.parse(jsonResponse).getAsJsonObject();
		jo = jo.getAsJsonObject("response");
		JsonObject relations = jo.get("relations").getAsJsonObject();
		int numberOfRelations = relations.get("numberOfRelations").getAsInt();
		
		return numberOfRelations;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public void setObjectUri(String objectUri) {
		this.objectUri = objectUri;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getLimit() {
		return this.limit;
	}
	public void setPageDisp(int pageDisp) {
		this.pageDisp = pageDisp;
	}
	public int getPageDisp() {
		return this.pageDisp;
	}
	public void setApiUri(String apiUri) {
		this.apiUri = apiUri;
	}
	public String getApiUri() {
		return apiUri;
	}
	public void setApiCount(String apiCount) {
		this.apiCount = apiCount;
	}
	public String getApiCount() {
		return apiCount;
	}
}