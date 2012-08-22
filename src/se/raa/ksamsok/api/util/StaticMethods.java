package se.raa.ksamsok.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * innehåller statiska metoder som används av flera klasser i systemet
 * @author Henrik Hjalmarsson
 */
public class StaticMethods
{
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.StaticMethods");
	/**
	 * används för att escapa special tecken i sökningar
	 * @param s sträng med text
	 * @return ny sträng med escape tecken fixade
	 */
	public static String escape(String s)
	{
		String escaped = ClientUtils.escapeQueryChars(s);
		return escaped;
	}
	
	/**
	 * formaterar special tecken som ej är tillåtna i XML
	 * @param s text
	 * @return formaterad text
	 */
	public static String xmlEscape(String s) 
	{
		String escape = StringEscapeUtils.escapeXml(s);
		escape = escape.replaceAll("<", "&lt;");
		escape = escape.replaceAll(">", "&gt;");
		return escape;
	}

	/**
	 * encodar en url
	 * @param url
	 * @return
	 */
	public static String encode(String url) 
	{
		String result = "" + url;
		result = StringUtils.replace(result, " ", "%20");
		result = StringUtils.replace(result, "&", "%26");
		return result;
	}
	
	/**
	 * Hämtar ut parametrar med rätt teckenkodning
	 * @param param parametern som skall hämtas ut
	 * @return parametern i rätt teckenkodning
	 */
	public static String getParam(String param)
	{
		try {//Vet inte om detta är ultimat. Men det tycks funka
			if(param != null) {
				param = URLDecoder.decode(param, "UTF-8");
				param = new String(param.getBytes("ISO-8859-1"), "UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return param;
	}

	/**
	 * Tar bort ett visst tecken ur en sträng, t ex citationstecken
	 * @param s strängen som ska redigeras
	 * @param c tecknet som ska tas bort
	 * @return den nya strängen
	 */
	public static String removeChar(String s, char c) {
		   String r = "";
		   for (int i = 0; i < s.length(); i ++) {
		      if (s.charAt(i) != c) r += s.charAt(i);
		      }
		   return r;
		}
}