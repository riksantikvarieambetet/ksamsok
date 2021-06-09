package se.raa.ksamsok.api.method;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.organization.Organization;
import se.raa.ksamsok.organization.Service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
	 * @param serviceProvider provider av {@link APIMethod}
	 * @param out Skrivaren som används för att skriva resultatet
	 * @param params map med parametrar till metoden
	 * @throws DiagnosticException om det inte går att initiera ett xml-dokument
	 */
	public GetServiceOrganization(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() {
		value = StringUtils.defaultIfEmpty(params.get(GetServiceOrganization.VALUE), ALL);
	}

	@Override
	protected void performMethodLogic() {
		if (value.equals(ALL)) {
			orgList = serviceProvider.getOrganizationManager().getAllOrganizations();
		} else {
			Organization org = serviceProvider.getOrganizationManager().getOrganization(value, true);
			if (org != null) {
				orgList = new ArrayList<>();
				orgList.add(org);
			}
		}
	}

	@Override
	protected void generateDocument() {
		Element result = super.generateBaseDocument();
		if (orgList == null || orgList.size() == 0) {
			result.appendChild(createInstitution(null));
		} else {
			for (Organization org : orgList) {
				result.appendChild(createInstitution(org));
			}
		}
		// Echo
		Element echo = doc.createElement("echo");
		result.appendChild(echo);
		
		Element methodEl = doc.createElement(METHOD);
		methodEl.appendChild(doc.createTextNode(METHOD_NAME));
		echo.appendChild(methodEl);
		
		Element valueEl = doc.createElement(VALUE);
		valueEl.appendChild(doc.createTextNode(value));
		echo.appendChild(valueEl);
	}

	/**
	 * Skapar xml data om en institution
	 * @param org organisationen som skall skrivas ut
	 */
	protected Element createInstitution(Organization org) {
		Element institution = doc.createElement("institution");
		if (org != null) {

			Element kortNamn = doc.createElement("kortnamn");
			kortNamn.appendChild(doc.createTextNode(org.getServ_org() != null ? org.getServ_org() : ""));
			institution.appendChild(kortNamn);

			Element namnSwe = doc.createElement("namnswe");
			namnSwe.appendChild(doc.createTextNode(org.getNamnSwe() != null ? org.getNamnSwe() : ""));
			institution.appendChild(namnSwe);

			Element namnEng = doc.createElement("namneng");
			namnEng.appendChild(doc.createTextNode(org.getNamnEng() != null ? org.getNamnEng() : ""));
			institution.appendChild(namnEng);

			Element beskrivSwe = doc.createElement("beskrivswe");
			beskrivSwe.appendChild(doc.createTextNode(org.getBeskrivSwe() != null ? org.getBeskrivSwe() : ""));
			institution.appendChild(beskrivSwe);

			Element beskrivEng = doc.createElement("beskriveng");
			beskrivEng.appendChild(doc.createTextNode(org.getBeskrivEng() != null ? org.getBeskrivEng() : ""));
			institution.appendChild(beskrivEng);

			Element adress1 = doc.createElement("adress1");
			adress1.appendChild(doc.createTextNode(org.getAdress1() != null ? org.getAdress1() : ""));
			institution.appendChild(adress1);

			Element adress2 = doc.createElement("adress2");
			adress2.appendChild(doc.createTextNode(org.getAdress2() != null ? org.getAdress2() : ""));
			institution.appendChild(adress2);

			Element postAdress = doc.createElement("postadress");
			postAdress.appendChild(doc.createTextNode(org.getPostadress() != null ? org.getPostadress() : ""));
			institution.appendChild(postAdress);

			Element epostKontaktPerson = doc.createElement("epostkontaktperson");
			epostKontaktPerson.appendChild(doc.createTextNode(org.getEpostKontaktperson() != null ? org.getEpostKontaktperson() : ""));
			institution.appendChild(epostKontaktPerson);

			Element websida = doc.createElement("websida");
			websida.appendChild(doc.createTextNode(org.getWebsida() != null ? org.getWebsida() : ""));
			institution.appendChild(websida);

			Element websidaKS = doc.createElement("websidaKS");
			websidaKS.appendChild(doc.createTextNode(org.getWebsidaKS() != null ? org.getWebsidaKS() : ""));
			institution.appendChild(websidaKS);

			Element lowResUrl = doc.createElement("lowressurl");//Felstavat?
			lowResUrl.appendChild(doc.createTextNode(org.getLowressUrl() != null ? org.getLowressUrl() : ""));
			institution.appendChild(lowResUrl);

			Element thumbnailUrl = doc.createElement("thumbnailurl");
			thumbnailUrl.appendChild(doc.createTextNode(org.getThumbnailUrl() != null ? org.getThumbnailUrl() : ""));
			institution.appendChild(thumbnailUrl);

			Element services = doc.createElement("services");
			for (int i = 0; org.getServiceList() != null && i < org.getServiceList().size(); i++) {
				Service s = org.getServiceList().get(i);
				Element service = doc.createElement("service");

				Element namn = doc.createElement("namn");
				namn.appendChild(doc.createTextNode(s.getNamn() != null ? s.getNamn() : ""));
				service.appendChild(namn);

				Element beskrivning = doc.createElement("beskrivning");
				beskrivning.appendChild(doc.createTextNode(s.getBeskrivning() != null ? s.getBeskrivning() : ""));
				service.appendChild(beskrivning);

				services.appendChild(service);
			}
			institution.appendChild(services);
		}
		
		return institution;
	}
}
