package se.raa.ksamsok.resolve;


import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class PreparedResponseTest {

	private PreparedResponse pr;
	private ArrayList<String> replacedByUris = new ArrayList<>();

	private final static String URI_1 = "foo";
	private final static String URI_2 = "bar";
	private final static String RESPONSE = "foo";

	@Before
	public void setUp() {
		replacedByUris.add(URI_1);
		replacedByUris.add(URI_2);
		pr = new PreparedResponse();
		pr.setReplacedByUris(replacedByUris);
		pr.setResponse(RESPONSE);
	}

	@Test
	public void testGetResponse() {
		assertEquals(RESPONSE, pr.getResponse());
	}
	@Test
	public void testGetReplaceUri() {
		ArrayList<String> replaceUris2 = pr.getReplacedByUris();
		assertNotNull(replaceUris2);
		assertEquals(URI_1, replaceUris2.get(0));
		assertEquals(URI_2, replaceUris2.get(1));
	}

	@Test
	public void testAddReplaceUri() {
		final String addedUri = "foobar";
		pr.addReplacedByUri(addedUri);
		ArrayList<String> replaceUris2 = pr.getReplacedByUris();
		assertNotNull(replaceUris2);
		assertEquals(URI_1, replaceUris2.get(0));
		assertEquals(URI_2, replaceUris2.get(1));
		assertEquals(addedUri, replaceUris2.get(2));

	}
}