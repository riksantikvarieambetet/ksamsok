package se.raa.ksamsok.ugchub;

import java.text.ParseException;

public class UGCHub {

	private int id;
	private String objectUri;
	private String createDate;
	private String userName;
	private String applicationName;
	private String tag;
	private String coordinate;
	private String comment;
	private String relatedUri;
	private String relationType;
	private String updateDate;
	private String imageUrl;
	
	public UGCHub() {
		
	}
	
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return this.id;
	}
	public void setObjectUri(String objectUri) {
		this.objectUri = objectUri;
	}
	public String getObjectUri() {
		return this.objectUri;
	}
	public void setCreateDate(String createdate) {
		this.createDate = createdate;
	}
	public String getCreateDate() throws ParseException {
		return this.createDate;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getUserName() {
		return this.userName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public String getApplicationName() {
		return this.applicationName;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	public String getTag() {
		return this.tag;
	}
	public void setCoordinate(String coordinate) {
		this.coordinate = coordinate;
	}
	public String getCoordinate() {
		return this.coordinate;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getComment() {
		return this.comment;
	}
	public void setRelatedUri(String relatedUri) {
		this.relatedUri = relatedUri;
	}
	public String getRelatedUri() {
		return this.relatedUri;
	}
	public void setRelationType(String relationType) {
		this.relationType = relationType;
	}
	public String getRelationType() {
		return this.relationType;
	}
	public void setUpdateDate(String updatedate) {
		this.updateDate = updatedate;
	}
	public String getUpdateDate() throws ParseException {
		return this.updateDate;
	}
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	public String getImageUrl() {
		return this.imageUrl;
	}
}