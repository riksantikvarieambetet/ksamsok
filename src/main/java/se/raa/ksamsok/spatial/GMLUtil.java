package se.raa.ksamsok.spatial;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml.producer.GeometryTransformer;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.geotools.xml.transform.Translator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.xml.sax.ContentHandler;

import javax.vecmath.Point2d;
import java.io.StringReader;

/**
 * Klass med metoder för att jobba med GML, koordinater och koordinatsystem.
 * TODO: se över allt som har med GML-versioner och koordinatsystem/ordning att göra - nu
 *       känns det som om det är en salig blandning
 */
public class GMLUtil {

	private static final Logger logger = LogManager.getLogger(GMLUtil.class);

	static {
		// OBS! denna är väldigt viktig - det blir fel annars - i alla fall om srsName är på formen EPSG:4326
		// TODO: kolla upp vad som händer om man får in en uri på formen
		//       http://www.opengis.net/gml/srs/epsg.xml#4326 (lon först?)
		//       eller på formen urn:x-ogc:def:crs:EPSG:4326 (lat först(default)?)
		//       som geotools kan producera
		System.setProperty("org.geotools.referencing.forceXY", "true");
	}

	/** EPSG:4326, WGS 84 */
	public static final String CRS_WGS84_4326 = "EPSG:4326";
	/** EPSG:3006, SWEREF 99 TM */
	public static final String CRS_SWEREF99_TM_3006 = "EPSG:3006";
	/** EPSG:3021, RT90 2.5 gon V */
	public static final String CRS_RT90_3021 = "EPSG:3021";

	// vi använder en enda parser och synkroniserar parsning pga följande bug i geotools 2.5.5
	// http://jira.codehaus.org/browse/GEOT-2615
	// TODO: när den är fixat och geotools uppgraderas kan man ta bort synkroniseringen
	private static final Configuration configuration = new GMLConfiguration();
	private static final Parser parser = new Parser(configuration);
	private static final GeometryFactory geometryFactory = new GeometryFactory();

	/**
	 * Konverterar gml-geometri till annat koordinatsystem.
	 * @param gml gml
	 * @param crsName identifierare för koordinatsystem
	 * @return konverterad gml-sträng
	 * @throws Exception vid fel
	 */
	public static String convertTo(String gml, String crsName) throws Exception {
		Geometry g = parseGeometry(gml);
		CoordinateReferenceSystem parsedCRS = getCRS(g);
		if (parsedCRS.getIdentifiers().iterator().next().toString().equalsIgnoreCase(crsName)) {
			return gml;
		}
		CoordinateReferenceSystem toCRS = CRS.decode(crsName);
		g = transformCRS(g, parsedCRS, toCRS, false);
		gml = toXML(g, toCRS);
		return gml;
	}

	/**
	 * Hämtar ut centrumpunkt för gml-geometrin som lon/lat (x/y).
	 * @param gml gml-geometri
	 * @return centrumpunkt i WGS 84 (EPSG:4326)
	 * @throws Exception vid fel
	 */
	public static Point2d getLonLatCentroid(String gml) throws Exception {
		Geometry g = parseGeometry(gml);
		// ta ut centrumpunkt
		Point centroid = g.getCentroid();
		CoordinateReferenceSystem parsedCRS = getCRS(g);
		CoordinateReferenceSystem wsg84 = CRS.decode(CRS_WGS84_4326); // true); // lonfirst?
		boolean nameEquals = parsedCRS.getName().equals(wsg84.getName());
		boolean equals = parsedCRS.equals(wsg84);
		if (nameEquals) {
			// TODO: se över
			if (!equals) {
				// samma namn, men ej samma referenssystem - beror så gott som alltid på
				// att lat/lon tros vara omkastade mot vad vi vill, men vi "kräver" att
				// det ska vara på xy så vi antar att det är det
				logger.warn("getLonLatCentroid: gml är (WGS84) men koordinatsystemen är !equals");
			}
		} else {
			// TODO: kanske är samma problem med andra koordinatreferenssystem med omkastade
			//       koordinater och inte bara wsg 84?
			centroid = (Point) transformCRS(centroid, wsg84, parsedCRS, false);
		}
		return new Point2d(centroid.getX(), centroid.getY());
	}

	/**
	 * Transformerar array med koordinater från ett namngivet koordinatsystem till ett annat.
	 * Ordningen förutsätts vara x1, y1, x2, y2 etc och antalet inskickade koordinatvärden
	 * måste vara jämnt.
	 * @param coords koordinater
	 * @param fromCRS koordinaternas nuvarande koordinatsystem
	 * @param toCRS koordinatsystem att konvertera till
	 * @return konverterade kooordinater
	 * @throws Exception vid fel
	 */
	public static double[] transformCRS(double[] coords, String fromCRS, String toCRS) throws Exception {
		if (fromCRS.equals(toCRS)) {
			return coords;
		}
		if (coords.length % 2 != 0) {
			throw new Exception("Felaktig koordinatlista - ojämnt antal koordinater");
		}
		double[] result = new double[coords.length];
		CoordinateReferenceSystem sourceCRS = CRS.decode(fromCRS);
		CoordinateReferenceSystem targetCRS = CRS.decode(toCRS);
		for (int i = 0; i < coords.length; i+=2) {
			Point p = geometryFactory.createPoint(new Coordinate(coords[i], coords[i + 1]));
			p = (Point) transformCRS(p, sourceCRS, targetCRS, true);
			result[i] = p.getX();
			result[i + 1] = p.getY();
		}
		return result;
	}

	// transformera geometri från ett koordinatsystem till ett annat
	private static Geometry transformCRS(Geometry geometry, CoordinateReferenceSystem sourceCRS, CoordinateReferenceSystem targetCRS, boolean lenient) throws Exception {
		//if (logger.isDebugEnabled()) {
		//	logger.debug("xform (lenient: " + lenient + ") from " + sourceCRS.getName() + " (" +
		//		CRS.getGeographicBoundingBox(sourceCRS) + ") --> " +
		//		targetCRS.getName() + " (" + CRS.getGeographicBoundingBox(targetCRS) + ")");
		//}
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, lenient);
		Geometry xformed = JTS.transform(geometry, transform);
		// sätt om crs
		xformed.setUserData(targetCRS);
		return xformed;
	}

	// försöker skapa en geometri-instans från gml
	private static Geometry parseGeometry(String gml) throws Exception {
		Object o;
		if (gml == null) {
			throw new Exception("gml är null");
		}
		// fulfix för oracle-genererade srsName med SDO som authority vilket geotools inte
		// känner till nåt om
		gml = gml.replace("SDO:", "EPSG:");
		try {
			// vi måste synkronisera parsningen pga en bug i geotools (eller eclipse-emf/xsd)
			// se http://jira.codehaus.org/browse/GEOT-2615
			// obs att det inte spelar nån roll om man använder nya parserinstanser istf en
			// enda utan problemet uppstår i alla fall
			// ett alternativ skulle kunna vara att använda "xdo"-parser istället då den
			// inte berörs av buggen och dessutom är snabbare  - den gör dock lite annorlunda
			// med koordinatsystemen och lägger till skillnad från gt-xml bara namnet på
			// utparsat srsName i userData istället för själva CoordinateReferenceSystem-
			// instansen vilket man i så fall måste ta hänsyn till
			// typ:
			// XMLReader reader = XMLReaderFactory.createXMLReader();
			// XMLSAXHandler xmlHandler = new XMLSAXHandler(new HashMap());
			// reader.setContentHandler(xmlHandler);
			// reader.parse(new InputSource(new StringReader(gml)));
			// Object o = xmlHandler.getDocument(); // bör ge en Geometry-instans
			synchronized (parser) {
				o = parser.parse(new StringReader(gml));
			}
		} catch (Throwable t) {
			throw new Exception("Fel vid parsning av gml: " + t.getMessage(), t);
		}
		if (o == null) {
			throw new Exception("Kunde inte tolka inskickad xml som gml");
		}
		if (!(o instanceof Geometry)) {
			throw new Exception("XML verkar vara gml, men kunde inte få fram en geometri");
		}
		return (Geometry) o;
	}

	// hämtar ut koordinatsystem från geometri
	// TODO: är detta samma som GML2EncodingUtils.getCRS()? om ja, använd den istället?
	private static CoordinateReferenceSystem getCRS(Geometry g) throws Exception {
		// i userdata lägger geotools-parsern fn info om vilket koordinatsystem gml:en var i
		Object o = g.getUserData();
		if (o == null || "".equals(o)) {
			// TODO: förutsätta att det är nåt speciellt crs och ge tillbaka en sån instans?
			throw new Exception("GML/XML-parsern från Geotools la ingen koordinatsystem-info " +
					"i userdata, saknas den i gml:en?");
		}
		if (!(o instanceof CoordinateReferenceSystem || o instanceof String)) {
			throw new Exception("GML/XML-parsern har lagt nåt annat än koordinatsystem-info " +
					"i userdata, en instans av " + o.getClass().getName());
		}
		if (o instanceof String) {
			CoordinateReferenceSystem crs = CRS.decode((String) o);
			if (crs == null) {
				throw new Exception("Fick ingen koordinatsystem-info vid avkodning av uttolkat " +
						"srsName: " + o);
			}
			o = crs;
		}

		return (CoordinateReferenceSystem) o;
	}

	// ger geometrin som ett gml-fragment med angivet koordinatreferenssystem som srsName.
	private static String toXML(Geometry g, CoordinateReferenceSystem crs) throws Exception {
		if (crs == null) {
			crs = getCRS(g);
		}
		// skulle kunna använda xdo istället men det är svårt att styra saker, typ:
		// GMLConfiguration conf = new GMLConfiguration(); // gml2 eller 3
		// org.geotools.xml.Encoder e = new org.geotools.xml.Encoder(conf);
		// e.setOmitXMLDeclaration(true);
		// e.setIndenting(true);
		// QName qname = new QName(GMLSchema.NAMESPACE.toString(), gType, "gml");
		// // g måste ha en crs-instans i userdata för att få med srsName i gml:en, namnet räcker ej
		// e.encode(g, qname, System.out);

		// TODO: detta är lite hackigt då det var svårt att få ut ett ok gml-fragment
		//       så som vi vill ha det med angivet srsName och inga extra namespaces
		//       från geotools
		final String toSrsName = crs.getIdentifiers().iterator().next().toString();
		GeometryTransformer gt = new GeometryTransformer() {
			@Override
			public Translator createTranslator(
					ContentHandler handler) {
				// TODO: 8 decimaler ok?
				return new GeometryTranslator(handler, 8) {

					@Override
					public void encode(Geometry geometry,
							String srsName) {
						if (srsName == null) {
							srsName = toSrsName;
						}
						super.encode(geometry, srsName);
					}
					@Override
					public String getDefaultNamespace() {
						// TODO: flytta till constructor etc
						// bort med extra namespaces
						coordWriter.setNamespaceUri(null);
						return null;
					}
				};
			}
		};
		gt.setIndentation(4); // TODO: detta kan skippas men är trevligt vid debug
		gt.setOmitXMLDeclaration(true);
		gt.setNamespaceDeclarationEnabled(true);
		return gt.transform(g);
	}

}
