package se.raa.ksamsok.api;

import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.organization.OrganizationManager;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.statistic.StatisticsManager;

/**
 * Interface för att koppla loss factory från metoderna och undvika cirkelberoenden.
 */
public interface APIServiceProvider {

	/**
	 * Ger söktjänst
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
}
