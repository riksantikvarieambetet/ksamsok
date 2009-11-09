package se.raa.ksamsok.api.method;

import java.io.Writer;

/**
 * hämtar persistent URI från given URL
 * @author Henrik Hjalmarsson
 */
public class PersistentURI implements APIMethod 
{
	/** namn på metoden */
	public static String METHOD_NAME = "persistentURI";
	/** parameter för url */
	public static String URI_PARAMETER = "url";
	
	private Writer writer;
	private String url;
	
	/**
	 * skapar ett objekt av PersistentURI
	 * @param writer
	 * @param url
	 */
	public PersistentURI(Writer writer, String url)
	{
		this.writer = writer;
		this.url = url;
	}
	
	@Override
	public void performMethod() 
	{
	}
}