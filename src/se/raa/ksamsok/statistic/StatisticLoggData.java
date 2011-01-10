package se.raa.ksamsok.statistic;

/**
 * Böna som håller data som skall loggas till statistik databasen
 * @author Henrik Hjalmarsson
 */
public class StatisticLoggData
{
	private String APIKey;
	private String param;
	private String queryString;
	public String getAPIKey()
	{
		return APIKey;
	}
	public void setAPIKey(String aPIKey)
	{
		APIKey = aPIKey;
	}
	public String getParam()
	{
		return param;
	}
	public void setParam(String param)
	{
		this.param = param;
	}
	public String getQueryString()
	{
		return queryString;
	}
	public void setQueryString(String queryString)
	{
		this.queryString = queryString;
	}
}
