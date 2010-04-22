package se.raa.ksamsok.api.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.ContentHelper;

/**
 * innehåller statiska metoder som används av flera klasser i systemet
 * @author Henrik Hjalmarsson
 */
public class StaticMethods
{
	private static final Logger logger =
		Logger.getLogger("se.raa.ksamsok.api.StaticMethods");
	/**
	 * används för att escapa special tecken i sökningar
	 * @param s sträng med text
	 * @return ny sträng med escape tecken fixade
	 */
	public static String escape(String s)
	{
		String escaped = QueryParser.escape(s);
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
	 * analyserar ett query
	 * @param field
	 * @param value
	 * @return
	 * @throws DiagnosticException
	 */
	public static Query analyseQuery(String field, String value)
		throws DiagnosticException
	{	
		if(value.indexOf(" ") != -1 && ContentHelper.isAnalyzedIndex(field)) {
			PhraseQuery phraseQuery = new PhraseQuery();
			StringTokenizer tokenizer = new StringTokenizer(value, " ");
			// använd svensk stamning för dessa analyserade index, samma 
			// som för indexeringen
			while (tokenizer.hasMoreTokens()) {
				String curValue = tokenizer.nextToken();
				// analysera sökvärdet pss som vid indexering
				curValue = CQL2Lucene.analyzeIndexText(curValue);
				// ta bara med termen om det ej är ett stopp-ord
				if (curValue != null) {
					phraseQuery.add(new Term(field, curValue));
				}
			}
			logger.info(phraseQuery);
			return phraseQuery;
		}else {
			value = CQL2Lucene.transformValueForField(field, value);
			Term term = new Term(field, value);
			TermQuery termQuery = new TermQuery(term);
			return termQuery;
		}
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
		try { //TODO vet inte om detta är ultimat, men det funkar ;)
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