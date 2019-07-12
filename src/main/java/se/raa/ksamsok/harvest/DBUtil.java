package se.raa.ksamsok.harvest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hjälpmetoder och konstanter för databasoperationer.
 */
public class DBUtil {

	private static final Logger logger = LogManager.getLogger(DBUtil.class);
	// stödda databastyper (nödvändigt då det är olika syntax för rownum/limit/offet etc)
	private enum DBType  { DERBY, ORACLE, POSTGRES }

	// instans för att komma ihåg vilken databastyp det var
	private static volatile DBType dbType = null;

	/** Konstant för normalt värde för status på poster */
	public static final int STATUS_NORMAL = 0;
	/** Konstant för att flagga att en post håller på att behandlas */
	public static final int STATUS_PENDING = 1;

	/** Fetchsize att använda på statements om man inte använder begränsande sql (limit/rownum etc)
	 * Se även {@linkplain #fetchFirst(Connection, String, int)} för annat alternativ.
	 * Oracle verkar ha ett default-värde (via row prefetching) på 10, för diskusion om detta se
	 * http://download.oracle.com/docs/cd/B10501_01/java.920/a96654/oraperf.htm#1002425
	 */
	public static final int FETCH_SIZE = 100;

	/**
	 * Hjälpmetod som stänger databasresurser.
	 * 
	 * @param rs resultset eller null
	 * @param st statement eller null
	 * @param c connection eller null
	 */
	public static void closeDBResources(ResultSet rs, Statement st, Connection c) {
		if (rs != null) {
			try {
				rs.close();
			} catch (Exception ignore) {}
		}
		if (st != null) {
			try {
				st.close();
			} catch (Exception ignore) {}
		}
		if (c != null) {
			try {
				c.close();
			} catch (Exception ignore) {}
		}
	}

	/**
	 * Hjälpmetod som gör commit om c != null och ej i autocommit-läge.
	 * 
	 * @param c connection
	 * @throws SQLException
	 */
	public static void commit(Connection c) throws SQLException {
		if (c != null && !c.getAutoCommit()) {
			c.commit();
		}
	}

	/**
	 * Hjälpmetod som gör rollback om c != null och ej i autocommit-läge.
	 * 
	 * @param c connection
	 */
	public static void rollback(Connection c) {
		try {
			if (c != null && !c.getAutoCommit()) {
				c.rollback();
			}
		} catch (SQLException e) {
			logger.error("Fel vid rollback", e);
		}
	}

	/**
	 * Ger sql för att hämta de första fetchNum raderna av inskickad sql för den
	 * databastyp som uppkopplingen stödjer.
	 * @param c databasuppkoppling
	 * @param sql sql
	 * @param fetchNum antal rader att hämta
	 * @return sql anpassad till aktuell databas
	 */
	public static String fetchFirst(Connection c, String sql, int fetchNum) {
		switch (determineDBType(c)) {
		case ORACLE:
			return "select * from (" + sql + ") where rownum <= " + fetchNum;
		case DERBY:
			return sql + " FETCH FIRST " + fetchNum + " ROWS ONLY";
		case POSTGRES:
			return sql + " LIMIT "+ fetchNum;
			default:
				logger.error("Unsupported database");
				throw new RuntimeException("unsupported database");
		}
	}

	// avgör och cachar upp databastyp för uppkopplingen, kastar runtime exception
	// om databastypen inte gick att avgöra eller om den inte stöds
	private static DBType determineDBType(Connection c) {
		if (dbType == null) {
			try {
				String dbName = c.getMetaData().getDatabaseProductName();
				if (dbName != null) {
					if (dbName.toLowerCase().contains("derby")) {
						dbType = DBType.DERBY;
					} else if (dbName.toLowerCase().contains("oracle")) {
						dbType = DBType.ORACLE;
					} else if (dbName.toLowerCase().contains("postgres")) {
						dbType = DBType.POSTGRES;
					}
				}
				if (dbType == null) {
					throw new Exception("could not determine database from product name: " +
							dbName);
				}
			} catch (Exception e) {
				logger.error("There was a problem determining database type", e);
				throw new RuntimeException("Unsupported database");
			} 
		}
		return dbType;
	}
}
