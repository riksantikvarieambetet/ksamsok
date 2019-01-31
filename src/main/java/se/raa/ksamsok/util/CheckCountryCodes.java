package se.raa.ksamsok.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * This class compares ksamsok's rdf with country codes with the xml from geonames.org

 */
public class CheckCountryCodes {
	
	private static final String COUNTRY_RDF = "./web/resurser/aukt/geo/country/country.rdf";
	private static final String GEONAMES_URL = "http://ws.geonames.org/countryInfo";
	
	public static void main(String argv[])
	{
		
		/**
		 * This inner class parses the rdf for the country codes
		 */
		class RDFHandler extends DefaultHandler {
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
		
		/**
		 * This class parses the xml from geonames.org for the country codes
		 */
		class GEOnameHandler extends DefaultHandler {
			private ArrayList<String> countryCodes=new ArrayList<String>(); 
			private boolean isCountryCode=false;
			public void startElement(String uri, String localName,String qName,Attributes attributes) throws SAXException {
				if(qName.equalsIgnoreCase("countryCode"))
					isCountryCode=true;
			}
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if(qName.equalsIgnoreCase("countryCode"))
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
		
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		RDFHandler rdfHandler = new RDFHandler();
		GEOnameHandler geonameHandler = new GEOnameHandler();
		try {
			SAXParser saxParser=parserFactory.newSAXParser();
			saxParser.parse(new File(COUNTRY_RDF).getAbsoluteFile(), rdfHandler);
			HttpClient httpClient=new HttpClient();
			HttpMethod method = new GetMethod(GEONAMES_URL);
			httpClient.executeMethod(method);
			saxParser.parse(method.getResponseBodyAsStream(), geonameHandler);
			for(String s:rdfHandler.getCountryCodes())
			{
				if (!geonameHandler.getCountryCodes().contains(s))
				{
					System.out.println("Not found country code:"+s);
				}
			}
			for(String s:geonameHandler.getCountryCodes())
			{
				if (!rdfHandler.getCountryCodes().contains(s))
				{
					System.out.println("Missing country code:"+s);
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
