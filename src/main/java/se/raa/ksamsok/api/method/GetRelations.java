package se.raa.ksamsok.api.method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.w3c.dom.Element;
import se.raa.ksamsok.api.APIServiceProvider;
import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.Relation;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.solr.SearchService;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetRelations extends AbstractAPIMethod {

	private static final Logger logger = LogManager.getLogger(GetRelations.class);

	// om och hur sameAs ska hanteras
	private enum InferSameAs { yes, no, sourceOnly, targetsOnly }

    /** Metodnamn */
	public static final String METHOD_NAME = "getRelations";

	/** Parameternamn för relation */
	public static final String RELATION_PARAMETER = "relation";
	/** Parameternamn för objektidentifierare */
	public static final String IDENTIFIER_PARAMETER = "objectId";
	/** Parameternamn för max antal träffar */
	public static final String MAXCOUNT_PARAMETER = "maxCount";
	/** Parameternamn för hantering av sameAs-relationer */
	public static final String INFERSAMEAS_PARAMETER = "inferSameAs";
	/** Parametervärde för att ange alla relationer */
	public static final String RELATION_ALL = "all";

	private static final String SOURCE_DIRECT = null; // började med värde här men kom fram till null ist
	private static final String SOURCE_REVERSE = "deduced";

	private static final String URI_PREFIX = "http://kulturarvsdata.se/";

	// datavariabler
	protected String relation;
	protected String partialIdentifier;
	protected int maxCount;
	protected InferSameAs inferSameAs;
	// hjälpvariabler
	protected boolean isAll;

	private Set<Relation> relations = Collections.emptySet();

	/** map som håller översättningsinformation för relationer - OBS används också från getRelationTypes */
	protected static final Map<String, String> relationXlate;
	/** map som håller envägsrelationer */
	protected static final List<String> relationOneWay;

	static {
		Map<String, String> map = new HashMap<String, String>();
		// dubbelriktade
		twoWay(map, ContentHelper.IX_ISPARTOF, ContentHelper.IX_HASPART);
		twoWay(map, ContentHelper.IX_CONTAINSOBJECT, ContentHelper.IX_ISCONTAINEDIN);
		twoWay(map, ContentHelper.IX_ISFOUNDIN, ContentHelper.IX_HASFIND);
		twoWay(map, ContentHelper.IX_HASCHILD, ContentHelper.IX_HASPARENT);
		twoWay(map, ContentHelper.IX_VISUALIZES, ContentHelper.IX_ISVISUALIZEDBY);
		twoWay(map, ContentHelper.IX_ISDESCRIBEDBY, ContentHelper.IX_DESCRIBES);
		twoWay(map, ContentHelper.IX_HASOBJECTEXAMPLE, ContentHelper.IX_ISOBJECTEXAMPLEFOR);
		twoWay(map, ContentHelper.IX_ISMENTIONEDBY, ContentHelper.IX_MENTIONS);

		// bio (lite special)
		map.put(ContentHelper.IX_FATHER, ContentHelper.IX_CHILD);
		map.put(ContentHelper.IX_MOTHER, ContentHelper.IX_CHILD);
		twoWay(map, ContentHelper.IX_CHILD, ContentHelper.IX_PARENT);

		// cidoc
		twoWay(map, ContentHelper.IX_HASFORMERORCURRENTOWNER, ContentHelper.IX_ISFORMERORCURRENTOWNEROF);
		twoWay(map, ContentHelper.IX_HASFORMERORCURRENTKEEPER, ContentHelper.IX_ISFORMERORCURRENTKEEPEROF);
		twoWay(map, ContentHelper.IX_HASCREATED, ContentHelper.IX_WASCREATEDBY);
		twoWay(map, ContentHelper.IX_HASRIGHTON, ContentHelper.IX_RIGHTHELDBY);
		twoWay(map, ContentHelper.IX_WASPRESENTAT, ContentHelper.IX_OCCUREDINTHEPRESENCEOF);
		twoWay(map, ContentHelper.IX_HADPARTICIPANT, ContentHelper.IX_PARTICIPATEDIN);
		twoWay(map, ContentHelper.IX_ISCURRENTORFORMERMEMBEROF, ContentHelper.IX_HASCURRENTORFORMERMEMBER);

		// roller, obs rollerna har fn inga invers-index, dvs man kan inte ange
		// (eller rättare sagt det indexeras inte) tex en författares verk
		// från toppnivån av författarobjektet då man helst vill kunna få med
		// år etc från kontextet vilket man inte kan få om relationen går åt andra hållet
		twoWay(map, ContentHelper.IX_CLIENT, ContentHelper.CLIENT_OF);
		twoWay(map, ContentHelper.IX_COMPOSER, ContentHelper.COMPOSER_OF);
		twoWay(map, ContentHelper.IX_AUTHOR, ContentHelper.AUTHOR_OF);
		twoWay(map, ContentHelper.IX_ARCHITECT, ContentHelper.ARCHITECT_OF);
		twoWay(map, ContentHelper.IX_INVENTOR, ContentHelper.INVENTOR_OF);
		twoWay(map, ContentHelper.IX_SCENOGRAPHER, ContentHelper.SCENOGRAPHER_OF);
		twoWay(map, ContentHelper.IX_DESIGNER, ContentHelper.DESIGNER_OF);
		twoWay(map, ContentHelper.IX_PRODUCER, ContentHelper.PRODUCER_OF);
		twoWay(map, ContentHelper.IX_ORGANIZER, ContentHelper.ORGANIZER_OF);
		twoWay(map, ContentHelper.IX_DIRECTOR, ContentHelper.DIRECTOR_OF);
		twoWay(map, ContentHelper.IX_PHOTOGRAPHER, ContentHelper.PHOTOGRAPHER_OF);
		twoWay(map, ContentHelper.IX_PAINTER, ContentHelper.PAINTER_OF);
		twoWay(map, ContentHelper.IX_BUILDER, ContentHelper.BUILDER_OF);
		twoWay(map, ContentHelper.IX_MASTERBUILDER, ContentHelper.MASTERBUILDER_OF);
		twoWay(map, ContentHelper.IX_CONSTRUCTIONCLIENT, ContentHelper.CONSTRUCTIONCLIENT_OF);
		twoWay(map, ContentHelper.IX_ENGRAVER, ContentHelper.ENGRAVER_OF);
		twoWay(map, ContentHelper.IX_MINTMASTER, ContentHelper.MINTMASTER_OF);
		twoWay(map, ContentHelper.IX_ARTIST, ContentHelper.ARTIST_OF);
		twoWay(map, ContentHelper.IX_DESIGNENGINEER, ContentHelper.DESIGNENGINEER_OF);
		twoWay(map, ContentHelper.IX_CARPENTER, ContentHelper.CARPENTER_OF);
		twoWay(map, ContentHelper.IX_MASON, ContentHelper.MASON_OF);
		twoWay(map, ContentHelper.IX_TECHNICIAN, ContentHelper.TECHNICIAN_OF);
		twoWay(map, ContentHelper.IX_PUBLISHER, ContentHelper.PUBLISHER_OF);
		twoWay(map, ContentHelper.IX_PUBLICIST, ContentHelper.PUBLICIST_OF);
		twoWay(map, ContentHelper.IX_MUSICIAN, ContentHelper.MUSICIAN_OF);
		twoWay(map, ContentHelper.IX_ACTORACTRESS, ContentHelper.ACTORACTRESS_OF);
		twoWay(map, ContentHelper.IX_PRINTER, ContentHelper.PRINTER_OF);
		twoWay(map, ContentHelper.IX_SIGNER, ContentHelper.SIGNER_OF);
		twoWay(map, ContentHelper.IX_FINDER, ContentHelper.FINDER_OF);
		twoWay(map, ContentHelper.IX_ABANDONEE, ContentHelper.ABANDONEE_OF);
		twoWay(map, ContentHelper.IX_INTERMEDIARY, ContentHelper.INTERMEDIARY_OF);
		twoWay(map, ContentHelper.IX_BUYER, ContentHelper.BUYER_OF);
		twoWay(map, ContentHelper.IX_SELLER, ContentHelper.SELLER_OF);
		twoWay(map, ContentHelper.IX_GENERALAGENT, ContentHelper.GENERALAGENT_OF);
		twoWay(map, ContentHelper.IX_DONOR, ContentHelper.DONOR_OF);
		twoWay(map, ContentHelper.IX_DEPOSITOR, ContentHelper.DEPOSITOR_OF);
		twoWay(map, ContentHelper.IX_RESELLER, ContentHelper.RESELLER_OF);
		twoWay(map, ContentHelper.IX_INVENTORYTAKER, ContentHelper.INVENTORYTAKER_OF);
		twoWay(map, ContentHelper.IX_EXCAVATOR, ContentHelper.EXCAVATOR_OF);
		twoWay(map, ContentHelper.IX_EXAMINATOR, ContentHelper.EXAMINATOR_OF);
		twoWay(map, ContentHelper.IX_CONSERVATOR, ContentHelper.CONSERVATOR_OF);
		twoWay(map, ContentHelper.IX_ARCHIVECONTRIBUTOR, ContentHelper.ARCHIVECONTRIBUTOR_OF);
		twoWay(map, ContentHelper.IX_INTERVIEWER, ContentHelper.INTERVIEWER_OF);
		twoWay(map, ContentHelper.IX_INFORMANT, ContentHelper.INFORMANT_OF);
		twoWay(map, ContentHelper.IX_PATENTHOLDER, ContentHelper.PATENTHOLDER_OF);
		twoWay(map, ContentHelper.IX_USER, ContentHelper.USER_OF);
		twoWay(map, ContentHelper.IX_SCANNEROPERATOR, ContentHelper.SCANNEROPERATOR_OF);
		twoWay(map, ContentHelper.IX_PICTUREEDITOR, ContentHelper.PICTUREEDITOR_OF);
		twoWay(map, ContentHelper.IX_EMPLOYER, ContentHelper.EMPLOYER_OF);

		// enkelriktade
		map.put(ContentHelper.IX_HASBEENUSEDIN, ContentHelper.IX_ISRELATEDTO);
		map.put(ContentHelper.IX_HASIMAGE, ContentHelper.IX_ISRELATEDTO);
		map.put(ContentHelper.IX_CONTAINSINFORMATIONABOUT, ContentHelper.IX_ISMENTIONEDBY);
		relationOneWay = Collections.unmodifiableList(Arrays.asList(
				ContentHelper.IX_CONTAINSINFORMATIONABOUT, ContentHelper.IX_HASBEENUSEDIN,
				ContentHelper.IX_HASIMAGE, ContentHelper.IX_FATHER, ContentHelper.IX_MOTHER));

		// samma i bägge riktningarna
		map.put(ContentHelper.IX_ISRELATEDTO, ContentHelper.IX_ISRELATEDTO);
		map.put(ContentHelper.IX_SAMEAS, ContentHelper.IX_SAMEAS);
		map.put(ContentHelper.IX_MARRIEDTO, ContentHelper.IX_MARRIEDTO);

		relationXlate = Collections.unmodifiableMap(map);
	}

	// lägger till relationerna åt bägge håll
	private static void twoWay(Map<String, String> map, String relA, String relB) {
		map.put(relA, relB);
		map.put(relB, relA);
	}

	/**
	 * Skapa ny instans.
	 * @param serviceProvider tjänstetillhandahållare
	 * @param out writer
	 * @param params parametrar
	 * @throws DiagnosticException 
	 */
	public GetRelations(APIServiceProvider serviceProvider, OutputStream out, Map<String, String> params) throws DiagnosticException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
		super.extractParameters();
		relation = getMandatoryParameterValue(RELATION_PARAMETER, "GetRelations.extractParameters", null);
		isAll = RELATION_ALL.equals(relation);
		if (!isAll && !relationXlate.containsKey(relation)) {
			throw new BadParameterException("Värdet för parametern " + RELATION_PARAMETER + " är ogiltigt",
					"GetRelations.extractParameters", null, false);
		}
		partialIdentifier = getMandatoryParameterValue(IDENTIFIER_PARAMETER, "GetRelations.extractParameters", null);
		String maxCountStr = getOptionalParameterValue(MAXCOUNT_PARAMETER, "GetRelations.extractParameters", null);
		if (maxCountStr != null) {
			try {
				maxCount = Integer.parseInt(maxCountStr); 
			} catch (Exception e) {
				throw new BadParameterException("Värdet för parametern " + MAXCOUNT_PARAMETER + " är ogiltigt",
						"GetRelations.extractParameters", null, false);
			}
		} else {
			// Om ingen maxCount är satt så kan man få oändliga sökningar i solr-index vilket för att man till slut för en out-of-memory i jvm
			maxCount = 10000;
		}
		String inferSameAsStr = getOptionalParameterValue(INFERSAMEAS_PARAMETER, "GetRelations.extractParameters", null);
		if (inferSameAsStr != null) {
			try {
				inferSameAs = InferSameAs.valueOf(inferSameAsStr);
			} catch (Exception e) {
				throw new BadParameterException("Värdet för parametern " + INFERSAMEAS_PARAMETER + " är ogiltigt",
						"GetRelations.extractParameters", null, false);
			}
		} else {
			inferSameAs = InferSameAs.no;
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
		Set<String> itemUris = new HashSet<String>();
		itemUris.add(uri);

		String escapedUri = ClientUtils.escapeQueryChars(uri);
		SolrQuery query = new SolrQuery();
		query.setRows(maxCount > 0 ? maxCount : Integer.MAX_VALUE); // TODO: kan det bli för många?

		// TODO: algoritmen kan behöva finslipas och optimeras tex för poster med många relaterade objekt
		// algoritmen ser fn ut så här - inferSameAs styr steg 1 och 3, default är att inte utföra dem
		// 1. hämta ev post för att få tag på postens sameAs
		// 2. sök fram källpost(er) och alla relaterade poster (post + ev alla sameAs och deras relaterade)
		// 3. hämta ev de relaterades sameAs och lägg till dessa som relationer

		// hämta uri och relationer
		query.addField(ContentHelper.I_IX_RELATIONS);
		query.addField(ContentHelper.IX_ITEMID);
		try {
			QueryResponse qr;
			SolrDocumentList docs;
			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.sourceOnly) {
				// hämta andra poster som är samma som denna och lägg till dem som "källposter"
				query.setQuery(ContentHelper.IX_ITEMID + ":"+ escapedUri);
				qr = searchService.query(query);
				docs = qr.getResults();
				for (SolrDocument doc: docs) {
					String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
					Collection<Object> values = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
					if (values != null) {
						for (Object value: values) {
							String parts[] = ((String) value).split("\\|");
							if (parts.length != 2) {
								logger.error("Fel på värde för relationsindex för " + itemId + ", ej på korrekt format: " + value);
								continue;
							}
							String typePart = parts[0];
							String uriPart = parts[1];
							if (ContentHelper.IX_SAMEAS.equals(typePart)) {
								itemUris.add(uriPart);
							}
						}
					}
				}
			}
			// bygg söksträng mh källposten/alla källposter
			StringBuilder searchStr = new StringBuilder();
			for (String itemId: itemUris) {
				String escapedItemId = ClientUtils.escapeQueryChars(itemId);
				if (searchStr.length() > 0) {
					searchStr.append(" OR ");
				}
				searchStr.append(ContentHelper.IX_ITEMID + ":").append(escapedItemId).append(" OR ").append(ContentHelper.IX_RELURI).append(":").append(escapedItemId);
			}
			// sök fram källposten/-erna och alla som har relation till den/dem
			query.setQuery(searchStr.toString());

			qr = searchService.query(query);
			docs = qr.getResults();
			relations = new HashSet<Relation>();
			for (SolrDocument doc: docs) {
				String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
				boolean isSourceDoc = itemUris.contains(itemId);
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
							if (!itemUris.contains(uriPart)) {
								// inte för aktuellt objekt
								continue;
							}
							orgTypePart = typePart;
							typePart = relationXlate.get(typePart);
							uriPart = itemId;

							// försök ta bort de som är onödiga, dvs de som redan har en envägs-relation
							// deras invers är inte intressant att ha med då den inte säger nåt
							if (ContentHelper.IX_ISRELATEDTO.equals(typePart)) {
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
			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.targetsOnly) {
				// sökning på same as för träffarnas uri:er och skapa relation till dessa också
				query.setFields(ContentHelper.IX_ITEMID); // bara itemId här
				for (Relation rel: new HashSet<Relation>(relations)) {
					query.setQuery(ContentHelper.IX_SAMEAS + ":"+ ClientUtils.escapeQueryChars(rel.getTargetUri()));
					qr = searchService.query(query);
					docs = qr.getResults();
					for (SolrDocument doc: docs) {
						String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
						// ta inte med min uri
						if (itemUris.contains(itemId)) {
							continue;
						}
						if (!relations.add(new Relation(rel.getRelationType(), itemId, rel.getSource(), rel.getOriginalRelationType()))) {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel (from same as) " + rel);
							}
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
	protected void generateDocument() {
		Element result = super.generateBaseDocument();
		// Relations
		Element relationsElement = doc.createElement("relations");
		relationsElement.setAttribute("count", Integer.toString(relations.size(),10));
		result.appendChild(relationsElement);
		for (Relation rel : relations){
			// Relation
			Element relationElement = doc.createElement("relation");
			if (rel.getSource() != null){
				relationElement.setAttribute("source", rel.getSource());
			}
			relationElement.setAttribute("type", rel.getRelationType());
			relationElement.appendChild(doc.createTextNode(rel.getTargetUri()));
			relationsElement.appendChild(relationElement);
		}
		
		//Echo
		 Element echo = doc.createElement("echo");
		 Element method = doc.createElement(METHOD);
		 method.appendChild(doc.createTextNode(METHOD_NAME));
		 echo.appendChild(method);
		 
		 Element relationEl = doc.createElement(RELATION_PARAMETER);
		 relationEl.appendChild(doc.createTextNode(relation));
		 echo.appendChild(relationEl);
		 
		 Element objectId = doc.createElement(IDENTIFIER_PARAMETER);
		 objectId.appendChild(doc.createTextNode(partialIdentifier));
		 echo.appendChild(objectId);
		 
		 Element maxCountEl = doc.createElement(MAXCOUNT_PARAMETER);
		 maxCountEl.appendChild(doc.createTextNode(Integer.toString(maxCount)));
		 echo.appendChild(maxCountEl);
		 
		 Element inferSameAsEl = doc.createElement(INFERSAMEAS_PARAMETER);
		 inferSameAsEl.appendChild(doc.createTextNode(inferSameAs.name()));
		 echo.appendChild(inferSameAsEl);
		 
		 result.appendChild(echo);
	}
}
