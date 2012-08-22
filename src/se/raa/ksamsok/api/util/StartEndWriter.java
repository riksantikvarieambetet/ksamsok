package se.raa.ksamsok.api.util;

import java.io.PrintWriter;

import se.raa.ksamsok.api.method.APIMethod;

/**
 * Klass för att hålla koll på och skriva ut huvuddel av XML svar
 * och fot av XML fil
 * @author Henrik Hjalmarsson
 */
public class StartEndWriter {
	/**
	 * skriver ut huvuddelen av XML svar
	 * @param writer
	 */
	public static void writeStart(PrintWriter writer, String stylesheet) {
		writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		if (stylesheet != null && stylesheet.trim().length() > 0) {
			writer.println("<?xml-stylesheet type=\"text/xsl\" href=\""
					+ stylesheet.replace("\"", "&quot;") + "\"?>");
		}
		writer.println("<result>");
		writer.println("<version>" + APIMethod.API_VERSION + "</version>");
	}
	
	/**
	 * Skriver ut foten av XML svar
	 * @param writer
	 */
	public static void writeEnd(PrintWriter writer) {
		writer.println("</result>");
	}
	
	/**
	 * skriver ut error om sådant uppstår
	 * @param writer
	 * @param e
	 */
	public static void writeError(PrintWriter writer, boolean writeHead,
			boolean writeFoot, String stylesheet, Exception e) {
		if(writeHead) {
			writeStart(writer, stylesheet);
		}
		writer.println("<error>");
		writer.println(e.getMessage());
		writer.println("</error>");
		if(writeFoot) {
			writeEnd(writer);
		}
	}
}
