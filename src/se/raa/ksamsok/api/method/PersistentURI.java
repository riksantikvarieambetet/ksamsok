package se.raa.ksamsok.api.method;

import java.io.PrintWriter;

/**
 * hämtar persistent URI från given URL
 * @author Henrik Hjalmarsson
 */
public class PersistentURI implements APIMethod 
{
	/** namn på metoden */
	public static final String METHOD_NAME = "persistentURI";
	/** parameter för url */
	public static final String URI_PARAMETER = "url";
	
	private PrintWriter writer;
	private String url;
	
	/**
	 * skapar ett objekt av PersistentURI
	 * @param writer
	 * @param url
	 */
	public PersistentURI(PrintWriter writer, String url)
	{
		this.writer = writer;
		this.url = url;
	}
	
	@Override
	public void performMethod() 
	{
	}
}