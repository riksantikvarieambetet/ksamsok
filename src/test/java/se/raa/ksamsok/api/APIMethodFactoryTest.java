package se.raa.ksamsok.api;

import org.junit.Test;
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

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class APIMethodFactoryTest {

	@Test
	public void testGetMethod() throws Exception {
		Map<String, String> params = new HashMap<>();
		OutputStream os = new BufferedOutputStream(null);

		APIMethodFactory apiMethodFactory = new APIMethodFactory();

		params.put(APIMethod.METHOD, Search.METHOD_NAME);
		APIMethod apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof Search);

		params.put(APIMethod.METHOD, Statistic.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof Statistic);

		params.put(APIMethod.METHOD, StatisticSearch.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof StatisticSearch);

		params.put(APIMethod.METHOD, AllIndexUniqueValueCount.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof AllIndexUniqueValueCount);

		params.put(APIMethod.METHOD, Facet.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof Facet);

		params.put(APIMethod.METHOD, SearchHelp.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof SearchHelp);

		params.put(APIMethod.METHOD, RSS.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof RSS);

		params.put(APIMethod.METHOD, GetServiceOrganization.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof GetServiceOrganization);

		params.put(APIMethod.METHOD, Stem.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof Stem);

		params.put(APIMethod.METHOD, GetRelations.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof GetRelations);

		params.put(APIMethod.METHOD, GetRelationTypes.METHOD_NAME);
		apiMethod = apiMethodFactory.getAPIMethod(params, os);
		assertTrue(apiMethod instanceof GetRelationTypes);

	}

	@Test(expected = MissingParameterException.class)
	public void testFailedGetMethod() throws Exception {
		Map<String, String> params = new HashMap<>();
		OutputStream os = new BufferedOutputStream(null);

		APIMethodFactory apiMethodFactory = new APIMethodFactory();
		params.put(APIMethod.METHOD, "non_existant_method_name");
		apiMethodFactory.getAPIMethod(params, os);
	}
}
