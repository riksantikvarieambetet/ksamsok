package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;

public class GetRelationTypes extends AbstractAPIMethod {

	/** Metodnamn */
	public static final String METHOD_NAME = "getRelationTypes";

	/** Parameternamn för relationstyp */
	public static final String RELATION_PARAMETER = "relation";
	/** Parametervärde för att ange alla relationstyper */
	public static final String RELATION_ALL = "all";

	private Map<String, String> relationTypes = Collections.emptyMap();

	// datavariabler
	private String relation;
	// hjälpvariabler
	private boolean isAll;

	/**
	 * Skapa ny instans.
	 * @param serviceProvider tjänstetillhandahållare
	 * @param writer writer
	 * @param params parametrar
	 */
	public GetRelationTypes(APIServiceProvider serviceProvider, PrintWriter writer, Map<String, String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		relation = getMandatoryParameterValue(RELATION_PARAMETER, "GetRelations.extractParameters", null, false);
		isAll = RELATION_ALL.equals(relation);
		if (!isAll && !GetRelations.relationXlate.containsKey(relation)) {
			throw new BadParameterException("Värdet för parametern " + RELATION_PARAMETER + " är ogiltigt",
					"GetRelations.extractParameters", null, false);
		}
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		if (isAll) {
			relationTypes = GetRelations.relationXlate;
		} else {
			relationTypes = Collections.singletonMap(relation,
					GetRelations.relationXlate.get(relation));
		}
	}

	@Override
	protected void writeResult() throws DiagnosticException {
		writer.println("<relationTypes count=\"" + relationTypes.size() + "\">");
		for (Entry<String, String> rel: relationTypes.entrySet()) {
			writer.print("<relationType name=\"" + rel.getKey() + "\"" +
					" reverse=\"" + rel.getValue() + "\">");
			writer.println("</relationType>");
		}
		writer.println("</relationTypes>");
	}

}
