package se.raa.ksamsok.api;

import org.springframework.beans.factory.annotation.Autowired;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.AllIndexUniqueValueCount;
import se.raa.ksamsok.api.method.Facet;
import se.raa.ksamsok.api.method.GetRelationTypes;
import se.raa.ksamsok.api.method.GetRelations;
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

import javax.sql.DataSource;
import java.io.OutputStream;
import java.util.Map;

/**
 * Factory-klass som bygger APIMethod-objekt
 * 
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
	// @Qualifier("dataSourceReader")
	DataSource dataSource;

	public APIMethodFactory() {}

	/**
	 * returnerar en instans av APIMethod beroende på vilka parametrar som kommer in
	 * 
	 * @param params mottagna parametrar
	 * @param writer för att skriva svaret
	 * @return APIMethod en instans av någon subklass till APIMethod
	 * @throws DiagnosticException TODO
	 */
	public APIMethod getAPIMethod(Map<String, String> params, OutputStream out)
		throws MissingParameterException, DiagnosticException {
		// hämtar ut metodnamnet från parametermappen
		String method = params.get(APIMethod.METHOD);
		if (method == null) { // måste alltid finnas en metod
			throw new MissingParameterException("obligatorisk parameter " + APIMethod.METHOD + " saknas",
				"APIMethodFactory.getAPIMethod", "metod saknas", false);
		}
		return getMethod(method, params, out);
	}

	/**
	 * returnerar en APIMethod
	 * 
	 * @param method metodens namn
	 * @param params
	 * @param writer
	 * @return
	 * @throws MissingParameterException
	 * @throws DiagnosticException
	 */
	private APIMethod getMethod(String method, Map<String, String> params, OutputStream out)
		throws MissingParameterException, DiagnosticException {
		APIMethod m;
		// en ny if-sats läggs till för varje ny metod
		switch (method) {
			case Search.METHOD_NAME:
				m = new Search(this, out, params);
				break;
			case Statistic.METHOD_NAME:
				m = new Statistic(this, out, params);
				break;
			case StatisticSearch.METHOD_NAME:
				m = new StatisticSearch(this, out, params);
				break;
			case AllIndexUniqueValueCount.METHOD_NAME:
				m = new AllIndexUniqueValueCount(this, out, params);
				break;
			case Facet.METHOD_NAME:
				m = new Facet(this, out, params);
				break;
			case SearchHelp.METHOD_NAME:
				m = new SearchHelp(this, out, params);
				break;
			case RSS.METHOD_NAME:
				m = new RSS(this, out, params);
				break;
			case GetServiceOrganization.METHOD_NAME:
				m = new GetServiceOrganization(this, out, params);
				break;
			case Stem.METHOD_NAME:
				m = new Stem(this, out, params);
				break;
			case GetRelations.METHOD_NAME:
				m = new GetRelations(this, out, params);
				break;
			case GetRelationTypes.METHOD_NAME:
				m = new GetRelationTypes(this, out, params);
				break;
			default:
				throw new MissingParameterException("metoden " + method + " finns inte", "APIMethodFactory.getAPIMethod",
						"felaktig metod", false);
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
	public DataSource getDataSource() {
		return dataSource;
	}
}
