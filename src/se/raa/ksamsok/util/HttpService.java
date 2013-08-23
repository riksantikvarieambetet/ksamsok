package se.raa.ksamsok.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a service for making http requests.
 *
 */
public class HttpService {

	// some default values
	protected static final int DEFAULT_CONNECTIONTIMEOUT = 15000;
	protected static final int DEFAULT_READTIMEOUT = 30000;
	protected static final int DEFAULT_MAXCONNECTIONSPERHOST = 10;
	protected static final int DEFAULT_MAXTOTALCONNECTIONS = 20;
	// variables that must be set from container, ie spring, to make it work
	protected String scheme;
	protected String host;
	protected String path;
	protected Logger logger;
	protected int connectionTimeOut;
	protected int readTimeOut;
	protected int maxConnectionsPerHost;
	protected int maxTotalConnections;

	protected HttpClient httpClient;

	/**
	 * Gets the logger to use, override in subclasses.
	 * @return a logger, must not return null
	 */
	protected Logger getLogger() {
		return LoggerFactory.getLogger(HttpService.class);
	}

	/**
	 * Initialization method that creates the internal http client.
	 * @throws Exception no logger, other problems
	 */
	public void init() throws Exception {
		logger = getLogger();
		if (logger == null) {
			throw new Exception("No logger, fix the getLogger() method!");
		}
		logger.info("Init");
		MultiThreadedHttpConnectionManager connectionManager = 
				new MultiThreadedHttpConnectionManager();
		// set some timeouts so that we won't wait too long for a reply
		HttpConnectionManagerParams cp = new HttpConnectionManagerParams();
		if (connectionTimeOut <= 0) {
			connectionTimeOut = DEFAULT_CONNECTIONTIMEOUT;
		}
		cp.setConnectionTimeout(connectionTimeOut);
		if (readTimeOut <= 0) {
			readTimeOut = DEFAULT_READTIMEOUT;
		}
		cp.setSoTimeout(readTimeOut);
		if (maxConnectionsPerHost <= 0) {
			maxConnectionsPerHost = DEFAULT_MAXCONNECTIONSPERHOST;
		}
		cp.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, maxConnectionsPerHost);
		if (maxTotalConnections <= 0) {
			maxTotalConnections = DEFAULT_MAXTOTALCONNECTIONS;
		}
		cp.setMaxTotalConnections(maxTotalConnections);
		// enable stale checking
		cp.setStaleCheckingEnabled(true);

		// use the params
		connectionManager.setParams(cp);

		HttpClientParams clientParams = new HttpClientParams();
		// don't retry
		DefaultHttpMethodRetryHandler retryhandler = new DefaultHttpMethodRetryHandler(0, false);
		clientParams.setParameter(HttpMethodParams.RETRY_HANDLER, retryhandler);

		// create the client
		httpClient = new HttpClient(clientParams, connectionManager);
		logger.info("Init done using timeouts (connect/read): " +
				connectionTimeOut + "/" + readTimeOut + " and " +
				"connections (maxPerHost/maxTotal) " +
				maxConnectionsPerHost + "/" + maxTotalConnections);
	}

	/**
	 * Destroy method that shuts down the internal http client.
	 */
	public void destroy() {
		logger.info("Destroy");
		if (httpClient != null) {
			((MultiThreadedHttpConnectionManager) httpClient.getHttpConnectionManager()).shutdown();
		}
		logger.info("Destroy done");
	}

	/**
	 * Performs a basic query to the specified host/uri using the supplied parameters
	 * and return the result as a json string.
	 * @param baseURI host/uri
	 * @return a json string
	 * @throws Exception
	 */
	public String basicJsonQuery(String baseURI) throws Exception {
		
		HttpMethod httpMethod = new GetMethod();
		httpMethod.setPath(baseURI);
		String jsonResponse = "";
		int result = -1;
		
		try{
			long startTime = System.currentTimeMillis();
			result = httpClient.executeMethod(httpMethod);
			long endTime = System.currentTimeMillis();
			// fetch might not be the correct term but...
			long fetchDuration = endTime - startTime;
			if(result == 200) {
				jsonResponse = httpMethod.getResponseBodyAsString();
			}  else {
				logger.error(httpMethod.getPath() +
						(httpMethod.getQueryString() != null ? "?" + httpMethod.getQueryString() : "" ) +
						" Duration: " + fetchDuration + "ms Status code: " + result);
			}
		} catch(ConnectTimeoutException to) {
			logger.warn("http call to host " + httpMethod.getPath() +
					" took too long to respond, >" +
					httpClient.getHttpConnectionManager().getParams().getConnectionTimeout() + "ms");
		} catch (IOException ie) { 
		    logger.warn("i/o error in request ", ie);
		} finally {
			httpMethod.releaseConnection();
		}
		
		return jsonResponse;
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

	public void setConnectionTimeOut(int connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}
	public void setReadTimeOut(int readTimeOut) {
		this.readTimeOut = readTimeOut;
	}
	public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
		this.maxConnectionsPerHost = maxConnectionsPerHost;
	}
	public void setMaxTotalConnections(int maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}
}
