package se.raa.ksamsok.ugchub;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class UGCHubManager {

	private @Autowired ApplicationContext appContext;
	private static final Logger logger = Logger.getLogger(UGCHubManager.class);
	private String scheme;
	private String host;
	private String path;
	
	public UGCHubManager(String scheme, String host, String path) {
		this.scheme = scheme;
		this.host = host;
		this.path = path;
	}
	
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}
	public String getScheme() {
		return this.scheme;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getHost() {
		return this.host;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getPath() {
		return this.path;
	}
}