package se.raa.ksamsok.organization;

import java.util.List;

/**
 * Böna som håller information om en organisation
 * @author Henrik Hjalmarsson
 */
public class Organization
{

	private String kortNamn;
	private String serv_org;
	private String namnSwe;
	private String namnEng;
	private String beskrivSwe;
	private String beskrivEng;
	private String adress1;
	private String adress2;
	private String postadress;
	private String epostKontaktPerson;
	private String websida;
	private String websidaKS;
	private String lowressUrl;
	private String thumbnailUrl;
	private List<Service> serviceList;
	
	public String getKortnamn()
	{
		return kortNamn;
	}
	public void setKortnamn(String kortNamn)
	{
		this.kortNamn = kortNamn;
	}
	
	public String getServ_org()
	{
		return serv_org;
	}
	public void setServ_org(String serv_org)
	{
		this.serv_org = serv_org;
	}
	
	public String getNamnSwe()
	{
		return namnSwe;
	}
	
	public void setNamnSwe(String namnSwe)
	{
		this.namnSwe = namnSwe;
	}
	
	public String getNamnEng()
	{
		return namnEng;
	}
	
	public void setNamnEng(String namnEng)
	{
		this.namnEng = namnEng;
	}
	
	public String getBeskrivSwe()
	{
		return beskrivSwe;
	}
	
	public void setBeskrivSwe(String beskrivSwe)
	{
		this.beskrivSwe = beskrivSwe;
	}
	
	public String getBeskrivEng()
	{
		return beskrivEng;
	}
	
	public void setBeskrivEng(String beskrivEng)
	{
		this.beskrivEng = beskrivEng;
	}
	
	public String getAdress1()
	{
		return adress1;
	}
	
	public void setAdress1(String adress1)
	{
		this.adress1 = adress1;
	}
	
	public String getAdress2()
	{
		return adress2;
	}
	
	public void setAdress2(String adress2)
	{
		this.adress2 = adress2;
	}
	
	public String getPostadress()
	{
		return postadress;
	}
	
	public void setPostadress(String postadress)
	{
		this.postadress = postadress;
	}

	public String getEpostKontaktperson()
	{
		return epostKontaktPerson;
	}
	
	public void setEpostKontaktPerson(String epostKontaktPerson)
	{
		this.epostKontaktPerson = epostKontaktPerson;
	}
	
	public String getWebsida()
	{
		return websida;
	}
	
	public void setWebsida(String websida)
	{
		this.websida = websida;
	}
	
	public String getWebsidaKS()
	{
		return websidaKS;
	}
	
	public void setWebsidaKS(String websidaKS)
	{
		this.websidaKS = websidaKS;
	}
	
	public String getLowressUrl()
	{
		return lowressUrl;
	}
	
	public void setLowressUrl(String lowressUrl)
	{
		this.lowressUrl = lowressUrl;
	}
	
	public String getThumbnailUrl()
	{
		return thumbnailUrl;
	}
	
	public void setThumbnailUrl(String thumbnailUrl)
	{
		this.thumbnailUrl = thumbnailUrl;
	}
	
	public List<Service> getServiceList()
	{
		return serviceList;
	}
	
	public void setServiceList(List<Service> serviceList)
	{
		this.serviceList = serviceList;
	}
}
