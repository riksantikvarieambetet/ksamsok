package se.raa.ksamsok.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class updats the parish rdf with links to the parishe's Wikipedia article.
 */
public class UpdateParishRdfWithWikipediaLinks {
	private static File parishRDF= new File("./web/resurser/aukt/geo/parish/parish.rdf");
	private static String queryUrl="http://toolserver.org/~kolossos/templatetiger/tt-table4.php?lang=svwiki&template=Infobox socken Sverige&where=sockenkod&is=%04d";
	private static HashMap<Integer,String> parishUrl=new HashMap<Integer,String>();
	
	public static void main(String argv[]){
		 /* **
		 * This inner class parses the rdf for the parish codes
		 */
		class RDFHandler extends DefaultHandler {
			private HashMap<Integer, String> parishCodes=new HashMap<Integer,String>(); 
			private boolean isParishCode=false;
			private int parishCode=0;
			private String parishName="";
			private boolean isParishName=false;
			public void startElement(String uri, String localName,String qName,Attributes attributes) throws SAXException {
				if(qName.equalsIgnoreCase("Parish"))
				{
					isParishCode=true;
					parishCode=Integer.parseInt(attributes.getValue("rdf:about"));
				}
				if(qName.equalsIgnoreCase("name"))
					isParishName=true;
			}
			public void endElement(String uri, String localName, String qName) throws SAXException {
				if(qName.equalsIgnoreCase("Parish"))
					isParishCode=false;
				if(qName.equalsIgnoreCase("name"))
					isParishName=false;
			}
			public void characters(char ch[], int start, int length) throws SAXException {
				if (isParishCode&&isParishName)
				{
					parishName=new String(ch, start, length);
					parishCodes.put(parishCode, parishName);
				}
			}
			public HashMap<Integer,String>getParishCodes(){
				return this.parishCodes;
			}
		}
		/*
		 * Here are the parish codes extracted from the parish rdf.
		 * Then an query is sent to the toolserver (a wikipedia project) to get the link
		 * to the parish wikipedia article
		 */
		SAXParserFactory parserFactory = SAXParserFactory.newInstance();
		RDFHandler rdfHandler = new RDFHandler();
		try {
			SAXParser saxParser=parserFactory.newSAXParser();
			saxParser.parse(parishRDF.getAbsoluteFile(), rdfHandler);
			Iterator<Integer> parishCodeIterator =rdfHandler.getParishCodes().keySet().iterator();
			int parishCode=0;
			HttpClient httpClient=new HttpClient();
			HttpMethod method;
			BufferedReader bodyReader;
			String respString;
			String parishHref;
			while (parishCodeIterator.hasNext())
			{
				parishCode=parishCodeIterator.next();
				method=new GetMethod(String.format(queryUrl, parishCode).replace(" ", "%20"));
				httpClient.executeMethod(method);
				bodyReader =new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
				while((respString=bodyReader.readLine())!=null)
				{
					if(respString.contains("<a href")&&!respString.contains("tools"))
					{
						parishHref=respString.substring(respString.indexOf("<a href=")+8, respString.indexOf(">", respString.indexOf("<a href=")));
						parishUrl.put(parishCode, parishHref);
						System.out.println(parishCode+": "+rdfHandler.parishCodes.get(parishCode)+": "+parishHref);
					}
				}
				
			}
				
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parishRDFUpdater();
		
	}
	
	/**
	 * This method updates the parish rdf with the links to their's wikipedia articels.
	 * In a perfect world the org.jrdf package should have been used. But we are treating
	 * the rdf-file as a normal text-file...
	 */
	private static void parishRDFUpdater(){
		BufferedReader in=null;
		try {
			FileReader fInStream=new FileReader(parishRDF);
			in = new BufferedReader(fInStream);
			FileWriter fstream=new FileWriter(new File(parishRDF.getParent(),"parish_wiki.rdf"),true);
			BufferedWriter out = new BufferedWriter(fstream);
			String inString;
			int parishCode;
			String parishLinkTemplate="<rdf:isDescribedBy rdf:resource=%s/>";
			String parishLink="";
			boolean parishStartTag=false;
			while((inString=in.readLine())!=null){
				if (inString.contains("<Parish rdf:about="))
				{
					if (parishStartTag)
					{
						System.out.println("ERROR");
					}
					else
					{
						parishStartTag=true;
						parishCode=Integer.parseInt(inString.replaceAll("[^0-9]",""));
						if(parishUrl.get(parishCode)!=null)
							parishLink="\t\t"+String.format(parishLinkTemplate,parishUrl.get(parishCode));
					}
				}
				else if(inString.contains("</Parish>"))
				{
					if(!parishStartTag)
					{
						System.out.println("ERROR");
					}
					else
					{
						parishStartTag=false;
						if (parishLink.length()>0)
						{	
							out.write(parishLink);
							out.newLine();
						}	
						parishLink="";
					}
				}
				if(!inString.contains("<rdf:isDescribedby rdf:resource="))
				{
					out.write(inString);
					out.newLine();
				}
					
			}
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (in != null){
				try {
					in.close();
				} catch (IOException e) {
					//Ignore
				}
			}
		}
		
	}

}
