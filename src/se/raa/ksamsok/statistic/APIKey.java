package se.raa.ksamsok.statistic;

import java.io.Serializable;

/**
 * Böna som håller data tillhörande en API nyckel
 * @author Henrik Hjalmarsson
 */
public class APIKey implements Serializable
{
	private static final long serialVersionUID = 8727853908051069427L;
	
	private String APIKey;
	private String owner;
	private String total;
	
	public String getAPIKey()
	{
		return APIKey;
	}
	
	public void setAPIKey(String aPIKey)
	{
		APIKey = aPIKey;
	}
	
	public String getOwner()
	{
		return owner;
	}
	
	public void setOwner(String owner)
	{
		this.owner = owner;
	}
	
	public String getTotal()
	{
		return total;
	}
	
	public void setTotal(String total)
	{
		this.total = total;
	}
}
