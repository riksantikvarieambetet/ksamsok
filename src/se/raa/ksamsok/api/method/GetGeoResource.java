package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import javax.sql.DataSource;

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
		try {
			c = ds.getConnection();
			// hämta sträng för anrop geometri->gml
			String toGmlCall = DBUtil.toGMLCall(c, "geometri");
			// bygg dynamisk fråga och ta med förälder om den är satt
			pst = c.prepareStatement("select namn, " +
					(parentColumn != null ? parentColumn + ", " : "")
					+ toGmlCall + " gml from " +
					table + " where " + pkColumn + " = ?");
			switch (type) {
			case PARISH:
				// sockenkod är, till skillnad från de andra en siffra
				int parishCode;
				try {
					parishCode = Integer.parseInt(code);
				} catch (Exception e) {
					throw new DiagnosticException("Bad parish code", "GetGeoResource", null, false);
				}
				pst.setInt(1, parishCode);
				break;
			case COUNTY:
			case PROVINCE:
			case MUNICIPALITY:
				pst.setString(1, code);
				break;
			}
			rs = pst.executeQuery();
			if (rs.next()) {
				nameResult = rs.getString("namn");
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
