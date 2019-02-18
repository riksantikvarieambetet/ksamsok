package se.raa.ksamsok.statistic;

import java.io.Serializable;

/**
 * Böna som håller information om statistik för en unik sökning
 * @author Henrik Hjalmarsson
 */
public class Statistic implements Serializable
{
	private static final long serialVersionUID = 1033382392022199836L;
	
	private String APIKey;
	private String queryString;
	private String param;
	private int count;
	
	public String getAPIKey()
	{
		return APIKey;
	}
	
	public void setAPIKey(String aPIKey)
	{
		APIKey = aPIKey;
	}
	
	public String getQueryString()
	{
		return queryString;
	}
	
	public void setQueryString(String queryString)
	{
		this.queryString = queryString;
	}
	
	public String getParam()
	{
		return param;
	}
	
	public void setParam(String param)
	{
		this.param = param;
	}
	
	public int getCount()
	{
		return count;
	}
	
	public void setCount(int count)
	{
		this.count = count;
	}
}
