package se.raa.ksamsok.api;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import static org.junit.Assert.fail;

public abstract class AbstractStatisticTest extends AbstractBaseTest {
	ByteArrayOutputStream out;

	@Before
	public void setUp() throws MalformedURLException {
		super.setUp();
		reqParams = new HashMap<>();
		reqParams.put("method", "statistic");
		reqParams.put("index", "mediaType=*|itemName=yxa*");
		reqParams.put("removeBelow","10");
	}

	@Test
	public void testStatisticsRespWithoutIndex(){
		out = new ByteArrayOutputStream();
		APIMethod statistic;
		reqParams.remove("index");
		try {
			statistic = apiMethodFactory.getAPIMethod(reqParams, out);
			statistic.setFormat(APIMethod.Format.JSON_LD);
			statistic.performMethod();
			System.out.println(new JSONObject(out.toString("UTF-8")).toString(1));
			fail("No excption thrown, expected MissingParameterException");
		} catch (MissingParameterException e) {
			//Ignore this exception is expected
		} catch (Exception e) {
			fail("Wrong excption thrown, expected MissingParameterException");
		}

	}
}
