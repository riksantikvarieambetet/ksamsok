package se.raa.ksamsok.spatial;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.Connection;

import oracle.jdbc.OracleConnection;
import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.GML;
import oracle.xml.parser.v2.DOMParser;

import org.w3c.dom.Node;

/**
 * Oracle-specifik variant av GMLDBWriter.
 */
public class OracleGMLDBWriter extends AbstractGMLDBWriter {

	private DOMParser parser;

	public OracleGMLDBWriter() {
		parser = new DOMParser();
	}

	@Override
	public void init(String serviceId, Connection c) {
		// måste vara en oracle-connection för att det ska funka med GML-klassen nedan
		// hämta ut underliggande oracle-uppkopplingen med ett hack
		if (c instanceof OracleConnection == false) {
			try {
				c = c.getMetaData().getConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (c instanceof OracleConnection == false) {
				c = find(c);
			}
		}
		super.init(serviceId, c);
	}

	private OracleConnection find(Connection c) {
		if (c != null) {
			if (c instanceof OracleConnection) {
	            return (OracleConnection) c;
			}
	        // try to find the Oracleconnection recursively
			for (Method method : c.getClass().getMethods()) {
				if (method.getReturnType().isAssignableFrom(java.sql.Connection.class) &&
						method.getParameterTypes().length == 0) {
		                    try {
		                    	return find((java.sql.Connection) (method.invoke(c, new Object[] {})));
		                    } catch (Exception e) {
		                            // Shouldn't ever happen.
		                    	e.printStackTrace();
		                    }
				}
			}
		}
		throw new RuntimeException("Fick ingen spatial-kompatibel oracleuppkoppling, " +
			"oracle spatial-jarfilerna måste kanske läggas i tomcat/lib och tas bort från WEB-INF lib?");
	}

	@Override
	protected Object convertToNative(String gml) throws Exception {
		// hack då oracles GML-klasser inte gillar GeometryCollection utan föredrar MultiGeometry
		// GeometryCollection finns inte i gml 2+ utan bara i gml 1 men förekommer i raä:s data fn
		gml = gml.replace("GeometryCollection", "MultiGeometry");
		parser.parse(new StringReader(gml));
		Node geomNode = parser.getDocument().getFirstChild();
		JGeometry jGeometry = GML.fromNodeToGeometry(geomNode);
		Object g = JGeometry.store(jGeometry, c);
		return g;
	}

}
