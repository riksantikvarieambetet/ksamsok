package se.raa.ksamsok.lucene;

import org.junit.Assert;
import org.junit.Test;
import se.raa.ksamsok.api.exception.BadParameterException;


public class ProtocolNumberTest {

	@Test
	public void testCompare() throws Exception {

		// test equal
		ProtocolNumber pn1 = new ProtocolNumber("0.0");
		ProtocolNumber pn2 = new ProtocolNumber("0.0");
		Assert.assertEquals(0, pn1.compareTo(pn2));

		// test pn1 bigger than pn2
		pn1 = new ProtocolNumber("0.2");
		pn2 = new ProtocolNumber("0.0");
		Assert.assertTrue(pn1.compareTo(pn2) > 0);

		// test pn2 bigger than pn1
		pn2 = new ProtocolNumber("0.2");
		pn1 = new ProtocolNumber("0.0");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

		// let's say that 0.2.0 is the same as 0.2, right?
		pn1 = new ProtocolNumber("0.2");
		pn2 = new ProtocolNumber("0.2.0");
		Assert.assertEquals(0, pn1.compareTo(pn2));

		// And the other way around
		pn1 = new ProtocolNumber("0.2.0");
		pn2 = new ProtocolNumber("0.2");
		Assert.assertEquals(0, pn1.compareTo(pn2));

		// Here pn1 should be bigger
		pn1 = new ProtocolNumber("0.2.2");
		pn2 = new ProtocolNumber("0.2");
		Assert.assertTrue(pn1.compareTo(pn2) > 0);

		// Here, however, pn2 should be bigger
		pn1 = new ProtocolNumber("0.2");
		pn2 = new ProtocolNumber("0.2.2");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

		// Here, pn2 should be bigger
		pn1 = new ProtocolNumber("0.2");
		pn2 = new ProtocolNumber("0.2.2");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

		// 1.11 is bigger than 1.2
		pn1 = new ProtocolNumber("1.11");
		pn2 = new ProtocolNumber("1.2");
		Assert.assertTrue(pn1.compareTo(pn2) > 0);

		// and the other way around
		pn1 = new ProtocolNumber("1.2");
		pn2 = new ProtocolNumber("1.11");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

		// second part is more relevant than the third
		pn1 = new ProtocolNumber("0.2.8");
		pn2 = new ProtocolNumber("0.3.2");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

		// also the other way around
		pn1 = new ProtocolNumber("0.3.2");
		pn2= new ProtocolNumber("0.2.8");
		Assert.assertTrue(pn1.compareTo(pn2) > 0);

		// and we should handle really long strings
		pn1= new ProtocolNumber("0.2.8.7.2.4.9.2");
		pn2 = new ProtocolNumber("0.2.8.7.2.4.9.4");
		Assert.assertTrue(pn1.compareTo(pn2) < 0);

	}

	@Test
	public void testEquals() throws Exception {
		// test equal
		ProtocolNumber pn1 = new ProtocolNumber("0.0");
		ProtocolNumber pn2 = new ProtocolNumber("0.0");
		Assert.assertTrue(pn1.equals(pn2));
		Assert.assertTrue(pn2.equals(pn1));

		pn1 = new ProtocolNumber("1.11");
		pn2 = new ProtocolNumber("1.11");
		Assert.assertTrue(pn1.equals(pn2));
		Assert.assertTrue(pn2.equals(pn1));


		// This is equal, right?
		pn1 = new ProtocolNumber("1.1.0");
		pn2 = new ProtocolNumber("1.1");
		Assert.assertTrue(pn1.equals(pn2));
		Assert.assertTrue(pn2.equals(pn1));

		// This is not equal
		pn1 = new ProtocolNumber("1.1.1");
		pn2 = new ProtocolNumber("1.1");
		Assert.assertFalse(pn1.equals(pn2));
		Assert.assertFalse(pn2.equals(pn1));

		// This is not equal
		pn1 = new ProtocolNumber("2.7.6.9.7.8.9.3.4");
		pn2 = new ProtocolNumber("2.7.6.9.7.8.9.3.3");
		Assert.assertFalse(pn1.equals(pn2));
		Assert.assertFalse(pn2.equals(pn1));

		// But this is
		pn1 = new ProtocolNumber("2.7.6.9.7.8.9.3.3");
		pn2 = new ProtocolNumber("2.7.6.9.7.8.9.3.3");
		Assert.assertTrue(pn1.equals(pn2));
		Assert.assertTrue(pn2.equals(pn1));
	}

	@Test
	public void testConstructor() throws Exception {
		String s = "1.2";
		ProtocolNumber pn;
		pn = new ProtocolNumber(s);
		Assert.assertEquals(s, pn.toString());
	}



	@Test
	public void testNoPeriodInConstructor() {


		BadParameterException e = Assert.assertThrows(BadParameterException.class, () -> {
			String s = "12";
			new ProtocolNumber(s);
		});

		Assert.assertTrue(e.getMessage().contains("minst en punkt"));

	}

	@Test
	public void testNoNumbersInConstructor() {
		BadParameterException e = Assert.assertThrows(BadParameterException.class, () -> {
			String s = "a.b";
			new ProtocolNumber(s);
		});
		Assert.assertTrue(e.getMessage().contains("numeriskt"));

	}


	@Test
	public void testNoNumberInConstructor()  {
		BadParameterException e = Assert.assertThrows(BadParameterException.class, () -> {
			String s = "1.b";
			new ProtocolNumber(s);
		});
		Assert.assertTrue(e.getMessage().contains("numeriskt"));
	}

	@Test
	public void testNullInConstructor() {
		BadParameterException e = Assert.assertThrows(BadParameterException.class, () -> {
			String s = null;
			//noinspection ConstantConditions
			new ProtocolNumber(s);
		});
		Assert.assertTrue(e.getMessage().contains("tomt"));

	}

}
