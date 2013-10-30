package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.organization.Organization;
import se.raa.ksamsok.organization.Service;

/**
 * Metod som hämtar information om institutioner och deras tjänster 
 * @author Henrik Hjalmarsson
 */
public class GetServiceOrganization extends AbstractAPIMethod {
	/** Metodens namn */
	public static final String METHOD_NAME = "getServiceOrganization";
	/** namn på parametern som håller institutions kortnamn */
	public static final String VALUE = "value";

	//privata statiska
	private static final String ALL = "all";

	//privata variabler
	private String value;
	protected List<Organization> orgList = Collections.emptyList();

	/**
	 * Skapar ett objekt av GetServiceOrganization
	 * @param writer Skrivaren som används för att skriva resultatet
	 * @param value kortnamn på organisationen som skall hämtas data om
	 */
	public GetServiceOrganization(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		value = StringUtils.defaultIfEmpty(params.get(GetServiceOrganization.VALUE), ALL);
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		if (value.equals(ALL)) {
			orgList = serviceProvider.getOrganizationManager().getAllOrganizations();
		} else {
			Organization org = serviceProvider.getOrganizationManager().getOrganization(value, true);
			if (org != null) {
				orgList = new ArrayList<Organization>();
				orgList.add(org);
			}
		}
	}

	@Override
	protected void writeResult() throws IOException {
		for (Organization org: orgList) {
			writeInstitution(org);
		}
	}

	/**
	 * Skriver ut xml data om en institution
	 * @param org organisationen som skall skrivas ut
	 * @throws IOException 
	 */
	protected void writeInstitution(Organization org) throws IOException {
		xmlWriter.writeEntity("institution");
		xmlWriter.writeEntityWithText("kortnamn",org.getServ_org() != null ? org.getServ_org() : "");
		xmlWriter.writeEntityWithText("namnswe", org.getNamnSwe() != null ? org.getNamnSwe() : "");
		xmlWriter.writeEntityWithText("namneng", org.getNamnEng() != null ? org.getNamnEng() : "");
		xmlWriter.writeEntityWithText("beskrivswe", org.getBeskrivSwe() != null ? org.getBeskrivSwe() : "");
		xmlWriter.writeEntityWithText("beskriveng", org.getBeskrivEng() != null ? org.getBeskrivEng() : "");
		xmlWriter.writeEntityWithText("adress1", org.getAdress1() != null ? org.getAdress1() : "");
		xmlWriter.writeEntityWithText("adress2", org.getAdress2() != null ? org.getAdress2() : "");
		xmlWriter.writeEntityWithText("postadress", org.getPostadress() != null ? org.getPostadress() : "");
		xmlWriter.writeEntityWithText("kontaktperson", org.getKontaktperson() != null ? org.getKontaktperson() : "");
		xmlWriter.writeEntityWithText("epostkontaktperson", org.getEpostKontaktperson() != null ? org.getEpostKontaktperson() : "");
		xmlWriter.writeEntityWithText("websida", org.getWebsida() != null ? org.getWebsida() : "");
		xmlWriter.writeEntityWithText("websidaks", org.getWebsidaKS() !=  null ? org.getWebsidaKS() : "");
		xmlWriter.writeEntityWithText("lowressurl", org.getLowressUrl() != null ? org.getLowressUrl() : "");
		xmlWriter.writeEntityWithText("thumbnailurl", org.getThumbnailUrl() != null ? org.getThumbnailUrl() : "");
		xmlWriter.writeEntity("services");
		for (int i = 0; org.getServiceList() != null && i < org.getServiceList().size(); i++) {
			Service service = org.getServiceList().get(i);
			xmlWriter.writeEntity("service");
			xmlWriter.writeEntityWithText("namn", service.getNamn() != null ? service.getNamn() : "");
			xmlWriter.writeEntityWithText("beskrivning", service.getBeskrivning() != null ? service.getBeskrivning() : "");
			xmlWriter.endEntity();
		}
		xmlWriter.endEntity();
		xmlWriter.endEntity();
	}
}
