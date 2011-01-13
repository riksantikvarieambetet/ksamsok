package se.raa.ksamsok.api.method;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.Relation;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.solr.SearchService;

public class GetRelations extends AbstractAPIMethod {

	private static final Logger logger = Logger.getLogger(GetRelations.class);

	// dubbelriktade
	private static final String HAS_PART = "hasPart";
	private static final String IS_PART_OF = "isPartOf";
	private static final String CONTAINS_OBJECT = "containsObject";
	private static final String IS_CONTAINED_IN = "isContainedIn";
	private static final String IS_FOUND_IN = "isFoundIn";
	private static final String HAS_FIND = "hasFind";
	private static final String HAS_PARENT = "hasParent";
	private static final String HAS_CHILD = "hasChild";
	private static final String VISUALIZES = "visualizes";
	private static final String IS_VISUALIZED_BY = "isVisualizedBy";
	private static final String DESCRIBES = "describes";
	private static final String IS_DESCRIBED_BY = "isDescribedBy";
	private static final String CONTAINS_INFORMATION_ABOUT = "containsInformationAbout";
	private static final String IS_MENTIONED_BY = "isMentionedBy";
	private static final String HAS_OBJECT_EXAMPLE = "hasObjectExample";
	private static final String IS_OBJECT_EXAMPLE_FOR = "isObjectExampleFor";
	// enkelriktade
	private static final String HAS_BEEN_USED_IN = "hasBeenUsedIn";
	private static final String HAS_IMAGE = "hasImage";
	// samma i bägge riktningarna
	private static final String IS_RELATED_TO = "isRelatedTo";
	private static final String SAME_AS = "sameAs";

	/** Metodnamn */
	public static final String METHOD_NAME = "getRelations";

	/** Parameternamn för relation */
	public static final String RELATION_PARAMETER = "relation";
	/** Parameternamn för objektidentifierare */
	public static final String IDENTIFIER_PARAMETER = "objectId";
	/** Parameternamn för max antal träffar */
	public static final String MAXCOUNT_PARAMETER = "maxCount";
	/** Parametervärde för att ange alla relationer */
	public static final String RELATION_ALL = "all";

	private static final String SOURCE_DIRECT = null; // började med värde här men kom fram till null ist
	private static final String SOURCE_REVERSE = "deduced";

	private static final String URI_PREFIX = "http://kulturarvsdata.se/";

	// datavariabler
	protected String relation;
	protected String partialIdentifier;
	protected int maxCount;
	// hjälpvariabler
	protected boolean isAll;

	private Set<Relation> relations = Collections.emptySet();

	/** map som håller översättningsinformation för relationer */
	protected static final Map<String, String> relationXlate;
	/** map som håller envägsrelationer */
	protected static final List<String> relationOneWay;

	static {
		Map<String, String> map = new HashMap<String, String>();
		// dubbelriktade
		map.put(IS_PART_OF, HAS_PART);
		map.put(HAS_PART, IS_PART_OF);

		map.put(CONTAINS_OBJECT, IS_CONTAINED_IN);
		map.put(IS_CONTAINED_IN, CONTAINS_OBJECT);

		map.put(IS_FOUND_IN, HAS_FIND);
		map.put(HAS_FIND, IS_FOUND_IN);

		map.put(HAS_CHILD, HAS_PARENT);
		map.put(HAS_PARENT, HAS_CHILD);

		map.put(VISUALIZES, IS_VISUALIZED_BY);
		map.put(IS_VISUALIZED_BY, VISUALIZES);

		map.put(IS_DESCRIBED_BY, DESCRIBES);
		map.put(DESCRIBES, IS_DESCRIBED_BY);

		map.put(CONTAINS_INFORMATION_ABOUT, IS_MENTIONED_BY);
		map.put(IS_MENTIONED_BY, CONTAINS_INFORMATION_ABOUT);

		map.put(HAS_OBJECT_EXAMPLE, IS_OBJECT_EXAMPLE_FOR);
		map.put(IS_OBJECT_EXAMPLE_FOR, HAS_OBJECT_EXAMPLE);

		// enkelriktade
		map.put(HAS_BEEN_USED_IN, IS_RELATED_TO);
		map.put(HAS_IMAGE, IS_RELATED_TO);
		relationOneWay = Collections.unmodifiableList(Arrays.asList(HAS_BEEN_USED_IN, HAS_IMAGE));

		// samma i bägge riktningarna
		map.put(IS_RELATED_TO, IS_RELATED_TO);
		map.put(SAME_AS, SAME_AS);

		relationXlate = Collections.unmodifiableMap(map);
	}

	/**
	 * Skapa ny instans.
	 * @param serviceProvider tjänstetillhandahållare
	 * @param writer writer
	 * @param params parametrar
	 */
	public GetRelations(APIServiceProvider serviceProvider, PrintWriter writer, Map<String, String> params) {
		super(serviceProvider, writer, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		relation = getMandatoryParameterValue(RELATION_PARAMETER, "GetRelations.extractParameters", null, false);
		isAll = RELATION_ALL.equals(relation);
		if (!isAll && !relationXlate.containsKey(relation)) {
			throw new BadParameterException("Värdet för parametern " + RELATION_PARAMETER + " är ogiltigt",
					"GetRelations.extractParameters", null, false);
		}
		partialIdentifier = getMandatoryParameterValue(IDENTIFIER_PARAMETER, "GetRelations.extractParameters", null, false);
		String maxCountStr = getOptionalParameterValue(MAXCOUNT_PARAMETER, "GetRelations.extractParameters", null, false);
		if (maxCountStr != null) {
			try {
				maxCount = Integer.parseInt(maxCountStr); 
			} catch (Exception e) {
				throw new BadParameterException("Värdet för parametern " + MAXCOUNT_PARAMETER + " är ogiltigt",
						"GetRelations.extractParameters", null, false);
			}
		} else {
			maxCount = -1;
		}
	}

	@Override
	protected void performMethodLogic() throws DiagnosticException {
		// om 0 gör inget alls
		if (maxCount == 0) {
			return;
		}
		SearchService searchService = serviceProvider.getSearchService();
		final String uri = URI_PREFIX + partialIdentifier;
		String escapedUri = ClientUtils.escapeQueryChars(uri);
		SolrQuery query = new SolrQuery();
		query.setRows(Integer.MAX_VALUE); // TODO: kan det bli för många?
		// hämta uri och relationer
		query.addField(ContentHelper.I_IX_RELATIONS);
		query.addField(ContentHelper.IX_ITEMID);
		// sök fram denna post och alla som har relation till denna post, sameAs hanteras ej annorlunda än övriga
		query.setQuery(ContentHelper.IX_ITEMID + ":" + escapedUri + " OR " + ContentHelper.IX_RELURI + ":"+ escapedUri);
		try {
			QueryResponse qr = searchService.query(query);
			SolrDocumentList docs = qr.getResults();
			relations = new HashSet<Relation>();
			for (SolrDocument doc: docs) {
				String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
				boolean isSourceDoc = uri.equals(itemId);
				Collection<Object> values = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
				if (values != null) {
					for (Object value: values) {
						String parts[] = ((String) value).split("\\|");
						if (parts.length != 2) {
							logger.error("Fel på värde för relationsindex för " + itemId + ", ej på korrekt format: " + value);
							continue;
						}
						String orgTypePart = null; // håller orginaltypen om vi gör en inversupplagning
						String typePart = parts[0];
						String uriPart = parts[1];
						if (!isSourceDoc) {
							if (!uri.equals(uriPart)) {
								// inte för aktuellt objekt
								continue;
							}
							orgTypePart = typePart;
							typePart = relationXlate.get(typePart);
							uriPart = itemId;

							// försök ta bort de som är onödiga, dvs de som redan har en envägs-relation
							// deras invers är inte intressant att ha med då den inte säger nåt
							if (IS_RELATED_TO.equals(typePart)) {
								boolean exists = false;
								Relation rel = null;
								for (String owRel: relationOneWay) {
									// bara typ och uri är intressanta för detta
									rel = new Relation(owRel, itemId, null, null);
									if (relations.contains(rel)) {
										exists = true;
										break;
									}
								}
								if (exists) {
									if (logger.isDebugEnabled()) {
										logger.debug("Exists inversed already " + rel);
									}
									continue;
								}
							}
						}

						String source = isSourceDoc ? SOURCE_DIRECT : SOURCE_REVERSE;
						Relation rel = new Relation(typePart, uriPart, source, orgTypePart);
						if (!relations.add(rel)) {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel " + rel);
							}
						}
						// optimering genom att göra return direkt vid detta fall, annars ska man
						// hoppa ur denna loop och den utanför den och sen filtrera
						// funkar dock bara för fallet "alla"
						if (maxCount > 0 && isAll && relations.size() == maxCount) {
							return;
						}
					}
				}
			}
			// postfilter vid sökning på specifik relation
			if (!isAll) {
				int matches = 0;
				Iterator<Relation> iter = relations.iterator();
				while (iter.hasNext()) {
					if (!iter.next().getRelationType().equals(relation) || maxCount > 0 && matches >= maxCount) {
						iter.remove();
					} else {
						++matches;
					}
				}
			}
		} catch (Exception e) {
			throw new DiagnosticException("Fel vid metodanrop", "GetRelations.performMethodLogic", e.getMessage(), true);
		}

	}

	@Override
	protected void writeResult() throws DiagnosticException {
		String source;
		//String originalRelationType;
		writer.println("<relations count=\"" + relations.size() + "\">");
		for (Relation rel: relations) {
			source = rel.getSource();
			//originalRelationType = rel.getOriginalRelationType();
			writer.print("<relation type=\"" + rel.getRelationType() + "\"" +
					(source != null ? " source=\"" + source + "\"" : "") +
					//(originalRelationType != null ? " originalType=\"" + originalRelationType + "\"" : "") +
					">");
			writer.print(StringEscapeUtils.escapeXml(rel.getTargetUri()));
			writer.println("</relation>");
		}
		writer.println("</relations>");
	}
}
