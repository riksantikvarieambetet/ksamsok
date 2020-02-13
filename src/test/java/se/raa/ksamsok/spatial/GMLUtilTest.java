package se.raa.ksamsok.spatial;

import org.junit.Test;

import javax.vecmath.Point2d;

import static junit.framework.TestCase.assertEquals;

public class GMLUtilTest {

	@Test
	public void testGetLonLatCentroid() throws Exception {
		Point2d point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">18.072394042188,59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(18.072394042188, point.x);
		assertEquals(59.3270266104628, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">18.072394042188,0</gml:coordinates></gml:Point>");
		assertEquals(18.072394042188, point.x);
		assertEquals(0.0, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">0,59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(0.0, point.x);
		assertEquals(59.3270266104628, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">0,-59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(0.0, point.x);
		assertEquals(-59.3270266104628, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">-18.072394042188,0</gml:coordinates></gml:Point>");
		assertEquals(-18.072394042188, point.x);
		assertEquals(0.0, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">-18.072394042188,59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(-18.072394042188, point.x);
		assertEquals(59.3270266104628, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">18.072394042188,-59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(18.072394042188, point.x);
		assertEquals(-59.3270266104628, point.y);

		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">-18.072394042188,-59.3270266104628</gml:coordinates></gml:Point>");
		assertEquals(-18.072394042188, point.x);
		assertEquals(-59.3270266104628, point.y);


		point = GMLUtil.getLonLatCentroid("<gml:Point xmlns:gml=\"http://www.opengis.net/gml\" " +
				"srsName=\"EPSG:4326\"><gml:coordinates cs=\",\" decimal=\".\" " +
				"ts=\" \">0,0</gml:coordinates></gml:Point>");
		assertEquals(0.0, point.x);
		assertEquals(0.0, point.y);

		// TODO: Testfall för ogiltiga värden (men biblioteket vi använder verkar glatt konvertera även sådana)
	}
}
