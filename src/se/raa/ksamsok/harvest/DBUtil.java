package se.raa.ksamsok.harvest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * Hjälpmetoder och konstanter för databasoperationer.
 */
public class DBUtil {

	private static final Logger logger = Logger.getLogger(DBUtil.class);

	/** Konstant för normalt värde för status på poster */
	public static final int STATUS_NORMAL = 0;
	/** Konstant för att flagga att en post håller på att behandlas */
	public static final int STATUS_PENDING = 1;

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

}
