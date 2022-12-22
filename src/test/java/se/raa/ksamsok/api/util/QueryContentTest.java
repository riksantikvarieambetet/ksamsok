package se.raa.ksamsok.api.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class QueryContentTest {

	QueryContent qc;

	@Before
	public void setup() {
		qc = new QueryContent();
		qc.addTerm("foo", "one");
		qc.addTerm("bar", "two");

	}

	@Test
	public void testAddTerm() {
		Map<String, String> map = qc.getTermMap();

		assertNotNull(map);
		assertEquals(2,map.size());
		assertEquals("one", map.get("foo"));
		assertEquals("two", map.get("bar"));
	}

	@Test
	public void testGetQueryString() {
		String qs = qc.getQueryString();
		// hashmap, the order is not important

		String[] result = qs.split(" AND ");

		// it should look something like 'foo:"one" AND bar:"two"', but the order is not important
		assertTrue(Arrays.asList(result).contains("foo:\"one\""));
		assertTrue(Arrays.asList(result).contains("bar:\"two\""));


	}

	@Test
	public void testGetQueryStringWithParams() {
		String qs = qc.getQueryString("baz:\"three\"");
		String[] result = qs.split(" AND ");
		assertTrue(Arrays.asList(result).contains("foo:\"one\""));
		assertTrue(Arrays.asList(result).contains("bar:\"two\""));
		assertTrue(Arrays.asList(result).contains("baz:\"three\""));
		}

}
