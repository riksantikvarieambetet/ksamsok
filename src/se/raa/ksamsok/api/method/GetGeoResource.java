package se.raa.ksamsok.api.method;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.harvest.DBUtil;

/**
 * Metod som hämtar rdf för en geo-resurs, dvs län, kommun, landskap eller socken från databas.
 */
public class GetGeoResource extends AbstractAPIMethod {

	/** Metodnamn */
	public static final Object METHOD_NAME = "getGeoResource";

	// diverse hjälpkonstanter
	private static final String URI_PARAMETER = "uri";
	private static final String URI_PREFIX = "http://kulturarvsdata.se/resurser/aukt/geo/";
	private static final String URI_PREFIX_COUNTY = URI_PREFIX + "county#";
	private static final String URI_PREFIX_MUNICIPALITY = URI_PREFIX + "municipality#";
	private static final String URI_PREFIX_PROVINCE = URI_PREFIX + "province#";
	private static final String URI_PREFIX_PARISH = URI_PREFIX + "parish#";
	private static final String URI_SVERIGE = "http://kulturarvsdata.se/resurser/aukt/geo/country#se";
	private static final int COUNTY = 0;
	private static final int MUNICIPALITY = 1;
	private static final int PROVINCE = 2;
	private static final int PARISH = 3;
	private static final String WFS_MAPSERVER_URI="http://map.raa.se/deegree/services/wfs?";
	private static final String OGC_PARISH_FILTER_TEMPLATE="<wfs:GetFeature xmlns:wfs='http://www.opengis.net/wfs' "+
															"xmlns:ad='urn:x-inspire:specification:gmlas:Addresses:3.0' "+
															"xmlns:ogc='http://www.opengis.net/ogc' "+
															"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' version='1.0.0' service='WFS' "+
															"xsi:schemaLocation='http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/wfs.xsd' outputFormat='text/xml; subtype=gml/3.2.1'>"+
															"<wfs:Query typeName='raa:socken'>"+
															"<ogc:PropertyName>raa:geometri</ogc:PropertyName>"+
															"<ogc:Filter>"+
															"<ogc:PropertyIsEqualTo>"+
															"<ogc:PropertyName>raa:sockenkod</ogc:PropertyName>"+
															"<ogc:Literal>%d</ogc:Literal>"+//%d will be used by the String.format and replace by the parish code
															"</ogc:PropertyIsEqualTo>"+
															"</ogc:Filter>"+
															"</wfs:Query>"+
															"</wfs:GetFeature>";
	private static final String OGC_PROVINCE_FILTER_TEMPLATE="<wfs:GetFeature xmlns:wfs='http://www.opengis.net/wfs' "+
															"xmlns:ad='urn:x-inspire:specification:gmlas:Addresses:3.0' "+
															"xmlns:ogc='http://www.opengis.net/ogc' "+
															"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' version='1.0.0' service='WFS' "+
															"xsi:schemaLocation='http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/wfs.xsd' outputFormat='text/xml; subtype=gml/3.2.1'>"+
															"<wfs:Query typeName='raa:landskap'>"+
															"<ogc:PropertyName>raa:geometri</ogc:PropertyName>"+
															"<ogc:Filter>"+
															"<ogc:PropertyIsEqualTo>"+
															"<ogc:PropertyName>raa:landskapsk</ogc:PropertyName>"+
															"<ogc:Literal>%s</ogc:Literal>"+//%d will be used by the String.format and replace by the province code
															"</ogc:PropertyIsEqualTo>"+
															"</ogc:Filter>"+
															"</wfs:Query>"+
															"</wfs:GetFeature>";

	// parameter- och tillståndsvariabler
	String uri;
	int type;
	String table;
	String pkColumn;
	String tagName;
	String parentColumn;
	String parentURI;
	String parentTagName;

	// resultatvariabler
	String nameResult;
	String gmlResult;

	/**
	 * Skapa ny instans.
	 * @param serviceProvider tjänstetillhandahållare
	 * @param writer writer
	 * @param params parametrar
	 */
	public GetGeoResource(APIServiceProvider serviceProvider,
			PrintWriter writer, Map<String, String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		uri = getMandatoryParameterValue("uri", "GetGeoResource", null, false);
		// sätt lite interna tillstånd baserat på hur uri:n börjar
		if (uri.startsWith(URI_PREFIX_COUNTY)) {
			type = COUNTY;
			table = "lan";
			pkColumn = "lanskod";
			tagName = "County";
		} else if (uri.startsWith(URI_PREFIX_MUNICIPALITY)) {
			type = MUNICIPALITY;
			table = "kommun";
			pkColumn = "kommunkod";
			tagName = "Municipality";
			parentColumn = "lanskod";
		} else if (uri.startsWith(URI_PREFIX_PROVINCE)) {
			type = PROVINCE;
			table = "landskap";
			pkColumn = "landskapskod";
			tagName = "Province";
		} else if (uri.startsWith(URI_PREFIX_PARISH)) {
			type = PARISH;
			table = "socken";
			pkColumn = "sockenkod";
			tagName = "Parish";
			parentColumn = "landskapskod";
		} else {
			throw new BadParameterException("Värdet för parametern " + URI_PARAMETER + " är ogiltigt.",
					"GetGeoResource.extractParameters", null, false);
		}
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		// TODO: det kanske är bättre att hämta gml från geoserver istället?
		// tex nedan + ett ogc-filter för att få bara en viss feature
		// http://localhost:9090/geoserver/wfs?request=getFeature&type=wfs&version=1.0.0&typename=GEM:LANDSKAP
		String code = uri.substring(uri.lastIndexOf("#") + 1);
		DataSource ds = serviceProvider.getDataSource();
		Connection c = null;
		PreparedStatement pst = null;
		ResultSet rs = null;
		String wfsQuery="";
		try {
			c = ds.getConnection();
			switch (type) {
			case PARISH:
				// sockenkod är, till skillnad från de andra en siffra
				int parishCode;
				try {
					parishCode = Integer.parseInt(code);
				} catch (Exception e) {
					throw new DiagnosticException("Bad parish code", "GetGeoResource", null, false);
				}
				wfsQuery=String.format(OGC_PARISH_FILTER_TEMPLATE,parishCode);
				pst = c.prepareStatement("select namn " +
						(parentColumn != null ? ","+parentColumn : "")
						+ " from " +
						table + " where " + pkColumn + " = ?");
				pst.setString(1, Integer.toString(parishCode));
				break;
			case PROVINCE:
				wfsQuery=String.format(OGC_PROVINCE_FILTER_TEMPLATE,code);
				pst = c.prepareStatement("select namn " +
						(parentColumn != null ? ","+parentColumn : "")
						+ " from " +
						table + " where " + pkColumn + " = ?");
				pst.setString(1, code);
				break;
			case COUNTY:
			case MUNICIPALITY:
				// hämta sträng för anrop geometri->gml
				String toGmlCall = DBUtil.toGMLCall(c, "geometri");
				// bygg dynamisk fråga och ta med förälder om den är satt
				pst = c.prepareStatement("select namn, " +
						(parentColumn != null ? parentColumn + ", " : "")
						+ toGmlCall + " gml from " +
						table + " where " + pkColumn + " = ?");
				pst.setString(1, code);
				break;
			}
			if (type==PARISH || type==PROVINCE)
			{
				HttpClient webClient=new HttpClient();
				PostMethod method =new PostMethod(WFS_MAPSERVER_URI);
				method.setRequestEntity(new StringRequestEntity(wfsQuery,"text/xml","utf-8"));
				webClient.executeMethod(method);
				BufferedReader bodyReader;
				String respString;
				Boolean isGmlBlock=false;
				String gmlBlock="";
				if (method.getStatusCode()==200)
				{
					bodyReader =new BufferedReader(new InputStreamReader(method.getResponseBodyAsStream()));
					while((respString=bodyReader.readLine())!=null)
					{
						System.out.println(respString);
						if(respString.contains("<raa:geometri>"))
						{
							isGmlBlock=true;
						}
						else if (respString.contains("</raa:geometri>"))
						{
							isGmlBlock=false;
						}
						else if(isGmlBlock)
						{
							gmlBlock=gmlBlock+respString;
						}
					}
					gmlResult=gmlBlock;
				}
				else
				{
					throw new DiagnosticException("Error in communication with map server", "Http status code: "+method.getStatusCode(), null, false);
				}
			}
			rs = pst.executeQuery();
			if (rs.next()) {
				nameResult = rs.getString("namn");
				if (type!=PARISH && type!=PROVINCE)
					gmlResult = rs.getString("gml");
				// ta ut extrakolumn och sätt taggnamn
				switch (type) {
				case PARISH:
					parentURI = URI_PREFIX_PROVINCE + rs.getString(parentColumn);
					parentTagName = "province";
					break;
				case MUNICIPALITY:
					parentURI = URI_PREFIX_COUNTY + rs.getString(parentColumn);
					parentTagName = "county";
					break;
				}
				// fixa in ns om det saknas, tex för pg (8 i alla fall)
				if (gmlResult != null && !gmlResult.contains("xmlns:gml=")) {
					gmlResult = gmlResult.replace(" srsName", " xmlns:gml=\"http://www.opengis.net/gml\" srsName");
				}
			} else {
				throw new Exception("Felaktig kod (" + code + ") i uri:n " + uri + "?");
			}
		} catch (Exception e) {
			throw new DiagnosticException("Fel vid uppslag av resurs: " + e.getMessage(),
					"GetGeoResourcce", null, false);
		} finally {
			DBUtil.closeDBResources(rs, pst, c);
		}
	}

	@Override
	protected void writeHead() {
		// överlagrad för att bara få ren rdf
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	}

	@Override
	protected void writeFoot() {
		headWritten = true;
		footWritten = true;
	}

	@Override
	protected void writeResult() throws DiagnosticException {
		writer.print("<rdf:RDF");
		writer.print(" xmlns=\"http://kulturarvsdata.se/schema/ksamsok-rdf#\"");
		writer.println(" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">");

		writer.println("<" + tagName + " rdf:about=\"" + uri + "\">");
		writer.println(" <name>" + nameResult + "</name>");
		// har vi en parent-uri använder vi den, annars tar vi sverige
		if (parentURI != null) {
			writer.println(" <" + parentTagName + " rdf:resource=\"" + parentURI + "\"/>");
		} else {
			writer.println(" <country rdf:resource=\"" + URI_SVERIGE + "\"/>");
		}
		writer.println(" <coordinates rdf:parseType=\"Literal\">");
		writer.println(gmlResult);
		writer.println(" </coordinates>");
		writer.println("</" + tagName + ">");

		writer.println("</rdf:RDF>");
	}
}
