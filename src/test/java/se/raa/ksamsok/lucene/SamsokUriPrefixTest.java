package se.raa.ksamsok.lucene;


import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SamsokUriPrefixTest {

	@Test
	public void testLookupPrefix() {
		// These should be corrected
		assertExcessIsStripped(SamsokProtocol.uriPrefixFoaf);
		assertExcessIsStripped(SamsokProtocol.uriPrefixMindswap);
		assertExcessIsStripped(SamsokProtocol.uriPrefixKSamsok);
		assertExcessIsStripped(SamsokProtocol.uriPrefix_cidoc_crm);
		assertExcessIsStripped(SamsokProtocol.uriPrefix_bio);

		// but nothing else
		assertEquals("foo", SamsokUriPrefix.lookupPrefix("foo"));
	}

	private void assertExcessIsStripped(String prefix) {
		assertEquals(prefix, SamsokUriPrefix.lookupPrefix(prefix + "whatever"));
	}
}
