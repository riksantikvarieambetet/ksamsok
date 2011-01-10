package se.raa.ksamsok.spatial;

import java.sql.PreparedStatement;

/**
 * Postgres/postgis-specifik variant av GMLDBWriter.
 */
public class PostgresGMLDBWriter extends AbstractGMLDBWriter {

	// TODO: möjligt att denna överagring inte behövs om setObject hanteras ok med det ändrade pst:t
	@Override
	public int insert(GMLInfoHolder gmlInfoHolder) throws Exception {
		int inserted = 0;
		if (gmlInfoHolder.hasGeometries()) {
			//pst = c.prepareStatement("insert into geometries " +
			//"(uri, serviceId, name, geometry) values (?, ? , ?, ?)");
			insertPst.setString(1, gmlInfoHolder.getIdentifier());
			insertPst.setString(2, serviceId);
			insertPst.setString(3, gmlInfoHolder.getName());
			for (String gml: gmlInfoHolder.getGmlGeometries()) {
				String g = (String) convertToNative(gml);
				insertPst.setString(4, g);
				inserted += insertPst.executeUpdate();
			}
		}
		return inserted;
	}

	@Override
	protected PreparedStatement prepareInsert() throws Exception {
		return c.prepareStatement("insert into geometries " +
				"(uri, serviceId, name, geometry) values (?, ? , ?, ST_GeomFromGML(?))");
	}
	@Override
	protected Object convertToNative(String gml) throws Exception {
		// hack då postgres/postgis inte gillar GeometryCollection utan föredrar MultiGeometry
		// GeometryCollection finns inte i gml 2+ utan bara i gml 1 men förekommer ev i raä:s data fn
		return gml.replace("GeometryCollection", "MultiGeometry");
	}

}
