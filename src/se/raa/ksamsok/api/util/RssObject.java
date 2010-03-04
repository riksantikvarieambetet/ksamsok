package se.raa.ksamsok.api.util;

/**
 * Böna för ett RSS objekt
 * @author Henrik Hjalmarsson
 *
 */
public class RssObject
{
	private String title;
	private String link;
	private String description;
	private String thumbnailUrl;
	private String imageUrl;
	
	public void setThumbnailUrl(String url)
	{
		thumbnailUrl = url;
	}
	
	public String getThumbnailUrl()
	{
		return thumbnailUrl;
	}
	
	public void setImageUrl(String url)
	{
		imageUrl = url;
	}
	
	public String getImageUrl()
	{
		return imageUrl;
	}
	
	public void setTitle(String title)
	{
		this.title = title;
	}
	
	public String getTitle()
	{
		return title;
	}
	
	public void setLink(String link)
	{
		this.link = link;
	}
	
	public String getLink()
	{
		return link;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public String getDescription()
	{
		return description;
	}
}
