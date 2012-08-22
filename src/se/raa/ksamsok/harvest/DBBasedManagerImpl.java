package se.raa.ksamsok.harvest;

import javax.sql.DataSource;

/**
 * Basklass för databasbaserad tjänstehanterare.
 */
public class DBBasedManagerImpl {

	protected DataSource ds;
	
	protected DBBasedManagerImpl(DataSource ds) {
		this.ds = ds;
	}
}
