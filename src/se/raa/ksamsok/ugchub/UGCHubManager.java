package se.raa.ksamsok.ugchub;

import java.text.MessageFormat;
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
	private String scheme;
	private String host;
	private String path;
	private String key;
	private String format;
	private int limit;
	private String methodRetrieve;
	private String methodDelete;
	private String methodUpdate;
	private String objectUriAll;
	private String scopeAll;
	private String scopeCount;
	private String scopeRelationAll;
	private String scopeSingle;
	private int pageDisp;
	private String apiUri; 
	
	public UGCHubManager(String scheme, String host, String path, String key, String format, int limit,
			String methodRetrieve, String methodDelete, String methodUpdate, String objectUriAll,
			String scopeAll, String scopeCount, String scopeRelationAll, String scopeSingle, int pageDisp) {
		this.scheme = scheme;
		this.host = host;
		this.path = path;
		this.key = key;
		this.format = format;
		this.limit = limit;
		this.methodRetrieve = methodRetrieve;
		this.methodDelete = methodDelete;
		this.methodUpdate = methodUpdate;
		this.objectUriAll = objectUriAll;
		this.scopeAll = scopeAll;
		this.scopeCount = scopeCount;
		this.scopeRelationAll = scopeRelationAll;
		this.scopeSingle = scopeSingle;
		this.pageDisp = pageDisp;
		apiUri = this.scheme + "://" + this.host + this.path + "?x-api=" + this.key + "&format=" + this.format;
	}
	
	/*init and destroy methods ärvs från se.raa.ksamsok.util.HttpService*/
	
	/**
	 * Get a single UGC-Hub record using api-method
	 * @param contentId id of the ugc-hub obload properties file valuesject.
	 * @return a UGCHub-bean.
	 * @throws Exception
	 */
	public UGCHub getUGCHub(int contentId) throws Exception {
		Gson gson = new Gson();
		UGCHub ugcHub = new UGCHub();
		
		String gethub = apiUri + "&method=" + methodRetrieve + "&objectUri=" + objectUriAll + "&scope=" + scopeSingle + "&contentId=" + String.valueOf(contentId);
		String jsonResponse = basicJsonQuery(gethub);
		jsonResponse = jsonResponse.substring(jsonResponse.indexOf("[") + 1, jsonResponse.lastIndexOf("]"));
		ugcHub = gson.fromJson(jsonResponse, UGCHub.class);
		
		return ugcHub;
	}
	
	/**
	 * Deletes a record in the UGCHub.
	 * @param contentId of the UGCHub-object.
	 * @return a boolean for SUCCESS/FAIL of deletion.
	 * @throws Exception
	 */
	public boolean deleteUGCHub(int contentId) throws Exception {
		JsonParser parser = new JsonParser();
		
		String deletehub = apiUri + "&method=" + methodDelete + "&objectId=" + String.valueOf(contentId);
		String jsonResponse = basicJsonQuery(deletehub);
		JsonObject object = parser.parse(jsonResponse).getAsJsonObject();
		JsonObject response = object.get("response").getAsJsonObject();
		String result = response.get("result").getAsString();
		
		return result.equals("SUCCESS") ? true : false;
	}

	/**
	 * Gets all ugchub-objects.
	 * @return a list of ugchub-objects.
	 * @throws Exception
	 */
	public List<UGCHub> getUGCHubs() throws Exception {
		Gson gson = new Gson();
		
		String gethubs = apiUri + "&method=" + methodRetrieve + "&objectUri=" + objectUriAll + "&scope=" + scopeAll;
		String jsonResponse = basicJsonQuery(gethubs);
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
		
		String paginate = apiUri + "&method=" + methodRetrieve + "&objectUri=" + objectUriAll + "&scope=" 
			       + scopeAll + "&maxCount=" + limit + "&selectFrom=" + offset;
		
		String jsonResponse = basicJsonQuery(paginate);
		jsonResponse = jsonResponse.substring(jsonResponse.indexOf("["), jsonResponse.lastIndexOf("]") + 1);
		List<UGCHub> ugcHubs = gson.fromJson(jsonResponse, new TypeToken<List<UGCHub>>(){}.getType());
		
		return ugcHubs;
	}
	
	/**
	 * Gets the total count of ugchub-objects.
	 * @return int, number of objects.
	 * @throws Exception
	 */
	public int getCount() throws Exception {
		JsonParser parser = new JsonParser();
		
		String count = apiUri + "&method=" + methodRetrieve + "&objectUri=" + objectUriAll + "&scope=" + scopeCount;
		String jsonResponse = basicJsonQuery(count);
		
		JsonObject jo = parser.parse(jsonResponse).getAsJsonObject();
		jo = jo.getAsJsonObject("response");
		JsonObject relations = jo.get("relations").getAsJsonObject();
		int numberOfRelations = relations.get("numberOfRelations").getAsInt();
		
		return numberOfRelations;
	}
	
	/**
	 * Gets the number of pages to paginate
	 * @return int number of pages
	 * @throws Exception
	 */
	public int getNumOfPages() throws Exception {
		int count = getCount();
		
		return (int) Math.ceil(((double)count)/(double)limit);
	}
	
	/**
	 * Updates a ugchub-object
	 * @param ugcHub object to be updated.
	 * @return boolean stating SUCCESS/FAIL for update.
	 * @throws Exception
	 */
	public boolean updateUGCHub(UGCHub ugcHub) throws Exception {
		JsonParser parser = new JsonParser();
		
		String update = apiUri + "&method=" + methodUpdate + "&objectUri=" + ugcHub.getObjectUri() 
				        + "&tag=" + ugcHub.getTag() + "&coordinate=" + ugcHub.getCoordinate() 
				        + "&comment=" + ugcHub.getComment() + "&relatedTo=" + ugcHub.getRelatedUri() 
				        + "&relationType=" + ugcHub.getRelationType() + "&imageUrl=" + ugcHub.getImageUrl() 
				        + "&objectId=" + String.valueOf(ugcHub.getId()) + "&scope=" + scopeRelationAll;
		
		String jsonResponse = basicJsonQuery(update);
		JsonObject jo = parser.parse(jsonResponse).getAsJsonObject();
		jo = jo.getAsJsonObject("response");
		String result = jo.get("result").getAsString();		

		return result.equals("SUCCESS") ? true : false;
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
}