package se.raa.ksamsok.spatial;

import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GMLInfoHolderTest {

	@Test
	public void testHasGeometries() {
		GMLInfoHolder holder = new GMLInfoHolder();
		assertFalse(holder.hasGeometries());

		final ArrayList<String> gmlGeometries = new ArrayList<>();
		holder.setGmlGeometries(gmlGeometries);
		assertFalse(holder.hasGeometries());

		gmlGeometries.add("foo");
		assertTrue(holder.hasGeometries());
	}
}

