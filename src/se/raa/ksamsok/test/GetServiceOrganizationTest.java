package se.raa.ksamsok.test;

import java.io.PrintWriter;
import java.io.StringWriter;

import junit.framework.TestCase;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.api.method.GetServiceOrganization;

public class GetServiceOrganizationTest extends TestCase
{
	public void testPerformMethod()
	{
		StringWriter sw = new StringWriter();
		PrintWriter writer = new PrintWriter(sw);
		APIMethod m = new GetServiceOrganization(writer, "all");
		try {
			m.performMethod();
		} catch (MissingParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DiagnosticException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(sw.toString());
	}
}
