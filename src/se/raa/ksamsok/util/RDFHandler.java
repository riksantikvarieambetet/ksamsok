package se.raa.ksamsok.util;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RDFHandler extends DefaultHandler {
	private ArrayList<String> countryCodes=new ArrayList<String>(); 
	private boolean isCountryCode=false;
	public void startElement(String uri, String localName,String qName,Attributes attributes) throws SAXException {
		if(qName.equalsIgnoreCase("alpha2"))
			isCountryCode=true;
	}
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(qName.equalsIgnoreCase("alpha2"))
			isCountryCode=false;
		}
	public void characters(char ch[], int start, int length) throws SAXException {
				if (isCountryCode)
					countryCodes.add(new String(ch, start, length).toUpperCase());
	}
	public ArrayList<String>getCountryCodes(){
		return this.countryCodes;
	}

}
