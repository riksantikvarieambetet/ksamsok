package se.raa.ksamsok.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class RedirectChecker
{
	protected URL url;
    protected URLConnection urlCon;
    protected HttpURLConnection httpUrlCon;
    protected URL redirect;
    
	public RedirectChecker(URL url)
	{
		this.url = url;
	}
	 
	 
	public RedirectChecker(String url) throws MalformedURLException
	{
		this.url = new URL(url);
	}
	
	public void setURL(URL url)
	{
		this.url = url;
	}
	 
	public boolean isRedirected() throws IOException
	{
		try {
			redirect = null;
	        urlCon = url.openConnection();
	        httpUrlCon = HttpURLConnection.class.cast(urlCon);
	        httpUrlCon.setInstanceFollowRedirects(false);
	        if (httpUrlCon.getResponseCode() != 301) {
	            redirect = null;
	            return false;
	        }
	        redirect = new URL(httpUrlCon.getHeaderField("Location"));
	        }catch (IOException e) {
	        	//logger.error(e.getMessage() + " When ckecking for redirect for URL: " + url);
	            throw new IOException(e.getMessage() + " When ckecking for redirect for URL: " + url);
	        }
	        return true;
	}
	 
	public URL getRedirect()
	{
		return redirect;
	}
	 
	public String getRedirectString()
	{
		return redirect.toString();
	}	    
}