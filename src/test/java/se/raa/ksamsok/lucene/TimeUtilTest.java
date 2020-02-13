package se.raa.ksamsok.lucene;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TimeUtilTest {

	@Test
	public void testCenturyString() {
		assertEquals("1900", TimeUtil.centuryString(1900));
		assertEquals("1900", TimeUtil.centuryString(1987));
		assertEquals("2000", TimeUtil.centuryString(2019));
		assertEquals("100", TimeUtil.centuryString(154));
		assertEquals("10000", TimeUtil.centuryString(10004));
		assertEquals("-200", TimeUtil.centuryString(-154));
		assertEquals("-100", TimeUtil.centuryString(-54));
		assertEquals("0", TimeUtil.centuryString(54));
	}

	@Test
	public void testDecadeString() {
		assertEquals("1900", TimeUtil.decadeString(1900));
		assertEquals("1980", TimeUtil.decadeString(1987));
		assertEquals("2010", TimeUtil.decadeString(2019));
		assertEquals("150", TimeUtil.decadeString(154));
		assertEquals("10000", TimeUtil.decadeString(10004));
		assertEquals("-160", TimeUtil.decadeString(-154));
		assertEquals("-60", TimeUtil.decadeString(-54));
		assertEquals("50", TimeUtil.decadeString(54));
		assertEquals("0", TimeUtil.decadeString(4));
		assertEquals("-10", TimeUtil.decadeString(-4));
	}

	@Test
	public void testEarliest() throws Exception {
		assertEquals(new Integer(1900), TimeUtil.earliest("1900", 1901));
		assertEquals(new Integer(1900), TimeUtil.earliest("1900", 1900));
		assertEquals(new Integer(1900), TimeUtil.earliest("1901", 1900));
		assertEquals(new Integer(0), TimeUtil.earliest("0", 1));
		assertEquals(new Integer(0), TimeUtil.earliest("0", 0));
		assertEquals(new Integer(0), TimeUtil.earliest("1", 0));
		assertEquals(new Integer(-1), TimeUtil.earliest("0", -1));
		assertEquals(new Integer(-1), TimeUtil.earliest("-1", -1));
		assertEquals(new Integer(-1), TimeUtil.earliest("-1", 0));
	}

	@Test
	public void testLatest() throws Exception {
		assertEquals(new Integer(1901), TimeUtil.latest("1900", 1901));
		assertEquals(new Integer(1900), TimeUtil.latest("1900", 1900));
		assertEquals(new Integer(1901), TimeUtil.latest("1901", 1900));
		assertEquals(new Integer(1), TimeUtil.latest("0", 1));
		assertEquals(new Integer(0), TimeUtil.latest("0", 0));
		assertEquals(new Integer(1), TimeUtil.latest("1", 0));
		assertEquals(new Integer(-0), TimeUtil.latest("0", -1));
		assertEquals(new Integer(-1), TimeUtil.latest("-1", -1));
		assertEquals(new Integer(0), TimeUtil.latest("-1", 0));
	}

	@Test(expected = Exception.class)
	public void testFaultyLatest() throws Exception {
		TimeUtil.latest("b", 1900);
	}

	@Test(expected = Exception.class)
	public void testFaultyEarliest() throws Exception {
		TimeUtil.earliest("b", 1900);
	}

	@Test
	public void testParseISO8601Date() {
		Date date = TimeUtil.parseISO8601Date("2019-11-27");
		assertEquals(1574809200000L, date.getTime());

		// not ISO8601 should not throw exception, but return null
		assertNull(TimeUtil.parseISO8601Date("11-27-2019"));
	}

	@Test
	public void testParseYearFromISO8601DateAndTransform() {
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 f.kr"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 F.kr"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 f.Kr"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 F.Kr"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 f.KR"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 F.KR"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 f.kR"));
		assertEquals("-1899", TimeUtil.parseYearFromISO8601DateAndTransform("1900 F.kR"));


		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 e.kr"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 E.kr"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 e.Kr"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 E.Kr"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 e.KR"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 E.KR"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 e.kR"));
		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900 E.kR"));

		// Några randvillkor. 0 fkr är ej godkänt, inte heller negativa år före kristus
		// 1 före kristus är år 0
		// TODO: fundera på om 1 före kristus verkligen ska vara år 0 - år 0 finns väl inte?
		assertEquals("0", TimeUtil.parseYearFromISO8601DateAndTransform("0 e.kr"));
		assertEquals("0", TimeUtil.parseYearFromISO8601DateAndTransform("1 f.kr"));
		assertEquals(null, TimeUtil.parseYearFromISO8601DateAndTransform("0 f.kr"));
		assertEquals(null, TimeUtil.parseYearFromISO8601DateAndTransform("-1 f.kr"));
		assertEquals(null, TimeUtil.parseYearFromISO8601DateAndTransform("-1 f.kr"));
		assertEquals(null, TimeUtil.parseYearFromISO8601DateAndTransform("1900 a.kr"));

		assertEquals("1900", TimeUtil.parseYearFromISO8601DateAndTransform("1900-01-01"));
		assertEquals("1997", TimeUtil.parseYearFromISO8601DateAndTransform("1997-05-03"));

		// is this correct?
		assertEquals("-1997", TimeUtil.parseYearFromISO8601DateAndTransform("-1997-05-03"));

	}

	@Test
	public void testTidyTimeString() throws Exception {
		assertEquals("1900", TimeUtil.tidyTimeString("19000"));
		assertEquals("1900", TimeUtil.tidyTimeString("1900"));
		assertEquals("900", TimeUtil.tidyTimeString("900"));
		assertEquals("90", TimeUtil.tidyTimeString("90"));
		assertEquals("9", TimeUtil.tidyTimeString("9"));
		assertEquals("0", TimeUtil.tidyTimeString("0"));
		assertEquals("-1", TimeUtil.tidyTimeString("-1"));

		assertEquals("-10", TimeUtil.tidyTimeString("-10"));
		assertEquals("-100", TimeUtil.tidyTimeString("-100"));
		assertEquals("-1000", TimeUtil.tidyTimeString("-1000"));

		assertEquals(TimeUtil.old_times, TimeUtil.tidyTimeString("-10000"));
	}

}

