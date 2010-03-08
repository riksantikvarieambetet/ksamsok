package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

public class GetServiceOrganization extends DefaultHandler 
	implements APIMethod
{
	public static final String METHOD_NAME = "getServiceOrganization";
	public static final String VALUE = "value";
	
	private static final String ALL = "all";
	
	private PrintWriter writer;
	private String value;
	private Institution institution;
	private StringBuffer tempValue;
	private boolean store;
	private InputStream is;
	
	public GetServiceOrganization(PrintWriter writer, String value)
	{
		this.writer = writer;
		this.value = value;
		is = this.getClass().getResourceAsStream("/" + this.getClass().getPackage().getName().replace('.', '/').trim() + "/" + "serviceOrganizations.xml");
		institution = new Institution();
	}
	
	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException
	{
		try {
			if(value.equals(ALL)) {
				writeEntireDocument();
			}else {
				setOutputData();
				writeResult();
			}
			is.close();
		} catch (ParserConfigurationException e) {
			throw new DiagnosticException("parser fel", "GetServiceOrganization.performMethod", e.getMessage(), true);
		} catch (SAXException e) {
			throw new DiagnosticException("SAX fel", "GetServiceOrganization.performMethod", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("IO fel", "GetServiceOrganization.performMethod", e.getMessage(), true);
		}
	}
	
	private void writeResult()
	{
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writer.println("<institution id=\"" + institution.getId() + "\">");
		writer.println("<name>" + institution.getName() + "</name>");
		writer.println("</institution>");
	}
	
	private void setOutputData() 
		throws ParserConfigurationException, SAXException, IOException
	{
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
		parser.parse(is, this);
	}
	
	private void writeEntireDocument() 
		throws ParserConfigurationException, SAXException, IOException
	{
		InputStreamReader reader = new InputStreamReader(is, "UTF-8");
		while(true) {
			int b = reader.read();
			if(b == -1) {
				break;
			}
			writer.write(b);
		}
	}
	
	
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException
	{
		tempValue.append(new String(ch, start, length));
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException
	{
		if(store) {
			if(qName.equalsIgnoreCase("institution")) {
				store = false;
			}else if(qName.equalsIgnoreCase("name")) {
				institution.setId(value);
				institution.setName(tempValue.toString());
			}
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException
	{
		//reset
		tempValue = new StringBuffer();
		if(qName.equalsIgnoreCase("institution") && attributes.getValue("id").equalsIgnoreCase(value)) {
			store = true;
		}
	}

	private class Institution
	{
		private String id;
		private String name;
		
		public void setId(String id)
		{
			this.id = id;
		}
		public String getId()
		{
			return id;
		}
		public String getName()
		{
			return name;
		}
		public void setName(String name)
		{
			this.name = name;
		}
	}
}
