package se.raa.ksamsok.api.util;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX handler som hanterar XML fil från K-samsök
 * @author Henrik Hjalmarsson
 */
public class XMLHandler extends DefaultHandler
{
	private RssObject data;
	private StringBuffer tempValue;
	private boolean store;
	private String imageType;
	
	/**
	 * skapar en XML handler
	 * @param data RssObject där extraherad data skall lagras
	 */
	public XMLHandler(RssObject data)
	{
		this.data = data;
	}
	
	/**
	 * returnerar RssObjekt
	 * @return
	 */
	public RssObject getData()
	{
		return data;
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException
	{
		if(store)
		{
			if(qName.equalsIgnoreCase("pres:representation"))
			{
				data.setLink(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:itemLabel"))
			{
				data.setTitle(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:description"))
			{
				data.setDescription(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:src"))
			{	
				if(imageType.equalsIgnoreCase("thumbnail"))
				{
					data.setThumbnailUrl(tempValue.toString());
				}else if(imageType.equalsIgnoreCase("lowres"))
				{
					data.setImageUrl(tempValue.toString());
				}
			}
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException
	{
		//reset
		tempValue = new StringBuffer();
		store = false;
		imageType = "";
		
		if(qName.equalsIgnoreCase("pres:representation"))
		{
			if(attributes.getValue("format") != null && attributes.getValue("format").equalsIgnoreCase("HTML"))
			{
				store = true;
			}
		}else if(qName.equalsIgnoreCase("pres:description"))
		{
			store = true;
		}else if(qName.equalsIgnoreCase("pres:itemLabel"))
		{
			store = true;
		}else if(qName.equalsIgnoreCase("pres:src") && (attributes.getValue("type").equalsIgnoreCase("thumbnail") || attributes.getValue("type").equalsIgnoreCase("lowres")))
		{
			store = true;
			imageType = attributes.getValue("type");
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException
	{
		tempValue.append(new String(ch, start, length));
	}
}
