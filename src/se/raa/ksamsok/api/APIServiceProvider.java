package se.raa.ksamsok.api;

import javax.sql.DataSource;

import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.organization.OrganizationManager;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.statistic.StatisticsManager;

/**
 * Interface fÃ¶r att koppla loss factory frÃ¥n metoderna och undvika cirkelberoenden.
 */
public interface APIServiceProvider {

	/**
	 * Ger sÃ¶ktjÃ¤nst
	 * @return the searchService
	 */
	SearchService getSearchService();

	/**
	 * Ger repository manager
	 * @return the repository manager
	 */
	HarvestRepositoryManager getHarvestRepositoryManager();

	/**
	 * Ger organization manager
	 * @return the organization manager
	 */
	OrganizationManager getOrganizationManager();

	/**
	 * Ger statistics manager
	 * @return the statistics manager
	 */
	StatisticsManager getStatisticsManager();

	/**
	 * Ger datasource
	 * @return datasource
	 */
	DataSource getDataSource();
}
