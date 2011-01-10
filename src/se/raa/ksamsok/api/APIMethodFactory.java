package se.raa.ksamsok.api;

import java.io.PrintWriter;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.AllIndexUniqueValueCount;
import se.raa.ksamsok.api.method.Facet;
import se.raa.ksamsok.api.method.GetServiceOrganization;
import se.raa.ksamsok.api.method.RSS;
import se.raa.ksamsok.api.method.Search;
import se.raa.ksamsok.api.method.SearchHelp;
import se.raa.ksamsok.api.method.Statistic;
import se.raa.ksamsok.api.method.StatisticSearch;
import se.raa.ksamsok.api.method.Stem;
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.organization.OrganizationManager;
import se.raa.ksamsok.solr.SearchService;
import se.raa.ksamsok.statistic.StatisticsManager;

/**
 * Factory-klass som bygger APIMethod-objekt
 * @author Henrik Hjalmarsson
 */
public class APIMethodFactory implements APIServiceProvider {
	// diverse tjänster och managers som används av metoder
	// de görs tillgängliga via APIServiceProvider och sätts mha spring
	// en gång på fabriken istället för vid varje metodinstansiering
	@Autowired
	SearchService searchService;
	@Autowired
	HarvestRepositoryManager hrm;
	@Autowired
	OrganizationManager organizationManager;
	@Autowired
	StatisticsManager statisticsManager;

	public APIMethodFactory() {
	}

	/**
	 * returnerar en instans av APIMethod beroende på vilka parametrar som
	 * kommer in
	 * @param params mottagna parametrar
	 * @param writer för att skriva svaret
	 * @return APIMethod en instans av någon subklass till APIMethod
	 */
	public APIMethod getAPIMethod(Map<String, String> params, PrintWriter writer)
			throws MissingParameterException, BadParameterException {
		//hämtar ut metodnamnet från parametermappen
		String method = params.get(APIMethod.METHOD);
		if (method == null) { //måste alltid finnas en metod
			throw new MissingParameterException("obligatorisk parameter " + 
					APIMethod.METHOD + " saknas",
					"APIMethodFactory.getAPIMethod", "metod saknas", false);
		}
		return getMethod(method, params, writer);
	}
	
	/**
	 * returnerar en APIMethod
	 * @param method metodens namn
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws BadParameterException
	 */
	private APIMethod getMethod(String method, Map<String,String> params,
			PrintWriter writer) throws MissingParameterException {
		APIMethod m = null;
		//en ny if-sats läggs till för varje ny metod
		if (method.equals(Search.METHOD_NAME)) {
			m = new Search(this, writer, params);
		} else if (method.equals(Statistic.METHOD_NAME)) {
			m = new Statistic(this, writer, params);
		} else if (method.equals(StatisticSearch.METHOD_NAME)) {
			m = new StatisticSearch(this, writer, params);
		} else if (method.equals(AllIndexUniqueValueCount.METHOD_NAME)) {
			m = new AllIndexUniqueValueCount(this, writer, params);
		} else if (method.equals(Facet.METHOD_NAME)) {
			m = new Facet(this, writer, params);
		} else if (method.equals(SearchHelp.METHOD_NAME)) {
			m = new SearchHelp(this, writer, params);
		} else if (method.equals(RSS.METHOD_NAME)) {
			m = new RSS(this, writer, params);
		} else if (method.equals(GetServiceOrganization.METHOD_NAME)) {
			m = new GetServiceOrganization(this, writer, params);
		} else if (method.equals(Stem.METHOD_NAME)) {
			m = new Stem(this, writer, params);
		} else {
			throw new MissingParameterException("metoden " + method + " finns inte", "APIMethodFactory.getAPIMethod", "felaktig metod", false);
		}
		return m;
	}

	@Override
	public SearchService getSearchService() {
		return searchService;
	}

	@Override
	public HarvestRepositoryManager getHarvestRepositoryManager() {
		return hrm;
	}

	@Override
	public OrganizationManager getOrganizationManager() {
		return organizationManager;
	}

	@Override
	public StatisticsManager getStatisticsManager() {
		return statisticsManager;
	}

}