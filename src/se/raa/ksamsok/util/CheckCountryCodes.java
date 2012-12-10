package se.raa.ksamsok.util;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class CheckCountryCodes {
	
	private static String countryRDF="./web/resurser/aukt/geo/country/country.rdf";
	private static String geonamesURL="http://ws.geonames.org/countryInfo";
	
	public static void main(String argv[])
	{
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser=parserFactory.newSAXParser();
			RDFHandler rdfHandler = new RDFHandler();
			saxParser.parse(new File(countryRDF).getAbsoluteFile(), rdfHandler);
			GEOnameHandler geonameHandler = new GEOnameHandler();
			HttpClient httpClient=new HttpClient();
			HttpMethod method = new GetMethod(geonamesURL);
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
