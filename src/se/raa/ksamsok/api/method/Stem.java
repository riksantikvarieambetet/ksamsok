package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.StaticMethods;

/**
 * Uför ordstammning av inskickad sträng och ger tillbaka en lista med unika ordstammar.
 */
public class Stem extends AbstractAPIMethod {

	public static final String METHOD_NAME = "stem";
	public static final String PARAM_WORDS = "words";
	//private static final Logger logg = Logger.getLogger(Stem.class);

	private String words;
	private Set<String> stems;

	public Stem(APIServiceProvider serviceProvider, PrintWriter writer, Map<String,String> params) {
		super(serviceProvider, writer, params);
		stems = Collections.emptySet();
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		this.words = StringUtils.trimToNull(params.get(PARAM_WORDS));
		if (words == null) {
			throw new MissingParameterException("Missing or empty parameter (" + PARAM_WORDS + ")",
					getClass().getName(), "Parameter " + PARAM_WORDS + " is required", false);
		}
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		try {
			stems = serviceProvider.getSearchService().analyze(words);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod", "Stem.performMethod", e.getMessage(), true);
		} catch (Exception e) {
			throw new DiagnosticException("Oväntat fel uppstod vid ordstammning", "Stem.performMethod", e.getMessage(), true);
		}
	}

	/**
	 * Skriver ut början av svaret
	 * @param stemList
	 */
	@Override
	protected void writeHeadExtra(){
		writer.println("<numberOfStems>" + stems.size() + "</numberOfStems>");
		writer.println("<stems>");
	}
	
	/**
	 * skriver ut resultatet av svaret
	 * @param termList
	 */
	@Override
	protected void writeResult() {
		for (String stem: stems) {
			writer.print("<stem>");
			writer.print(StaticMethods.xmlEscape(stem));
			writer.println("</stem>");
		}
	}
	
	/**
	 * Skriver ut foten av svaret
	 */
	@Override
	protected void writeFootExtra() {
		writer.println("</stems>");
		writer.println("<echo>");
		writer.println("<method>" + METHOD_NAME + "</method>");
		writer.println("<words>" + StaticMethods.xmlEscape(words) + "</words>");
		writer.println("</echo>");
	}

}
