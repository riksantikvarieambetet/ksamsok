package se.raa.ksamsok.api.method;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Uför ordstammning av inskickad sträng och ger tillbaka en lista med unika ordstammar.
 */
public class Stem extends AbstractAPIMethod {

	public static final String METHOD_NAME = "stem";
	public static final String PARAM_WORDS = "words";
	//private static final Logger logg = LogManager.getLogger(Stem.class);

	private String words;
	private Set<String> stems;

	public Stem(APIServiceProvider serviceProvider, OutputStream out, Map<String,String> params) throws DiagnosticException {
		super(serviceProvider, out, params);
		stems = Collections.emptySet();
	}

	@Override
	protected void extractParameters() throws MissingParameterException {
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

	@Override
	protected void generateDocument() {
		Element result = super.generateBaseDocument();
		Element numberOfStems = doc.createElement("numberOfStems");
		numberOfStems.appendChild(doc.createTextNode(Integer.toString(stems.size(), 10)));
		result.appendChild(numberOfStems);
		Element stemsEl = doc.createElement("stems");
		result.appendChild(stemsEl);
		for (String s : stems){
			Element stemEl = doc.createElement("stem");
			stemEl.appendChild(doc.createTextNode(s));
			stemsEl.appendChild(stemEl);
		}
		Element echo = doc.createElement("echo");
		result.appendChild(echo);
		Element method = doc.createElement("method");
		method.appendChild(doc.createTextNode(METHOD_NAME));
		echo.appendChild(method);
		Element wordsEl = doc.createElement("words");
		wordsEl.appendChild(doc.createTextNode(words));
		echo.appendChild(wordsEl);
	}

}
