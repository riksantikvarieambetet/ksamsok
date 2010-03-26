package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.organization.Organization;
import se.raa.ksamsok.organization.OrganizationDatabaseHandler;
import se.raa.ksamsok.organization.Service;

/**
 * Metod som hämtar information om institutioner och deras tjänster 
 * @author Henrik Hjalmarsson
 */
public class GetServiceOrganization implements APIMethod
{
	/** Metodens namn */
	public static final String METHOD_NAME = "getServiceOrganization";
	/** namn på parametern som håller institutions kortnamn */
	public static final String VALUE = "value";
	
	//privata statiska
	private static final String ALL = "all";
	private static final String DATASOURCE_NAME = "harvestdb";
	
	//privata variabler
	private PrintWriter writer;
	private String value;
	private DataSource ds = null;
	
	/**
	 * Skapar ett objekt av GetServiceOrganization
	 * @param writer Skrivaren som används för att skriva resultatet
	 * @param value kortnamn på organisationen som skall hämtas data om
	 */
	public GetServiceOrganization(PrintWriter writer, String value)
	{
		this.writer = writer;
		this.value = value;
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void performMethod() 
		throws MissingParameterException, BadParameterException, 
			DiagnosticException
	{
		OrganizationDatabaseHandler organizationDatabaseHandler = new OrganizationDatabaseHandler(ds);
		StartEndWriter.writeStart(writer);
		if(value.equals(ALL)) {
			List<Organization> orgList = organizationDatabaseHandler.getAllOrganizations();
			for(int i = 0; i < orgList.size(); i++) {
				Organization org = orgList.get(i);
				writeInstitution(org);
			}
		}else {
			Organization org = organizationDatabaseHandler.getOrganization(value);
			if(org != null) {
				writeInstitution(org);
			}
		}
		StartEndWriter.writeEnd(writer);
	}
	
	/**
	 * Skriver ut xml data om en institution
	 * @param org organisationen som skall skrivas ut
	 */
	private void writeInstitution(Organization org)
	{
		writer.println("<institution>");
		writer.println("<kortnamn>" + StaticMethods.xmlEscape(org.getKortnamn() != null ? org.getKortnamn() : "") + "</kortnamn>");
		writer.println("<namnswe>" + StaticMethods.xmlEscape(org.getNamnSwe() != null ? org.getNamnSwe() : "") + "</namnswe>");
		writer.println("<namneng>" + StaticMethods.xmlEscape(org.getNamnEng() != null ? org.getNamnEng() : "") + "</namneng>");
		writer.println("<beskrivswe>" + StaticMethods.xmlEscape(org.getBeskrivSwe() != null ? org.getBeskrivSwe() : "") + "</beskrivswe>");
		writer.println("<beskriveng>" + StaticMethods.xmlEscape(org.getBeskrivEng() != null ? org.getBeskrivEng() : "") + "</beskriveng>");
		writer.println("<adress1>" + StaticMethods.xmlEscape(org.getAdress1() != null ? org.getAdress1() : "") + "</adress1>");
		writer.println("<adress2>" + StaticMethods.xmlEscape(org.getAdress2() != null ? org.getAdress2() : "") + "</adress2>");
		writer.println("<postadress>" + StaticMethods.xmlEscape(org.getPostadress() != null ? org.getPostadress() : "") + "</postadress>");
		writer.println("<kontaktperson>" + StaticMethods.xmlEscape(org.getKontaktperson() != null ? org.getKontaktperson() : "") + "</kontaktperson>");
		writer.println("<epostkontaktperson>" + StaticMethods.xmlEscape(org.getEpostKontaktperson() != null ? org.getEpostKontaktperson() : "") + "</epostkontaktperson>");
		writer.println("<websida>" + StaticMethods.xmlEscape(org.getWebsida() != null ? org.getWebsida() : "") + "</websida>");
		writer.println("<websidaks>" + StaticMethods.xmlEscape(org.getWebsidaKS() !=  null ? org.getWebsidaKS() : "") + "</websidaks>");
		writer.println("<lowressurl>" + StaticMethods.xmlEscape(org.getLowressUrl() != null ? org.getLowressUrl() : "") + "</lowressurl>");
		writer.println("<thumbnailurl>" + StaticMethods.xmlEscape(org.getThumbnailUrl() != null ? org.getThumbnailUrl() : "") + "</thumbnailurl>");
		writer.println("<services>");
		for(int i = 0; org.getServiceList() != null && i < org.getServiceList().size(); i++) {
			Service service = org.getServiceList().get(i);
			writer.println("<service>");
			writer.println("<namn>" + StaticMethods.xmlEscape(service.getNamn() != null ? service.getNamn() : "") + "</namn>");
			writer.println("<beskrivning>" + StaticMethods.xmlEscape(service.getBeskrivning() != null ? service.getBeskrivning() : "") + "</beskrivning>");
			writer.println("</service>");
		}
		writer.println("</services>");
		writer.println("</institution>");
	}
}
