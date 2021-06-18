package se.raa.ksamsok.api.method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
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

import java.io.IOException;
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
		Map<String, String> map = new HashMap<>();
		// dubbelriktade
		twoWay(map, ContentHelper.IX_ISPARTOF, ContentHelper.IX_HASPART);
		twoWay(map, ContentHelper.IX_CONTAINSOBJECT, ContentHelper.IX_ISCONTAINEDIN);
		twoWay(map, ContentHelper.IX_ISFOUNDIN, ContentHelper.IX_HASFIND);
		twoWay(map, ContentHelper.IX_HASCHILD, ContentHelper.IX_HASPARENT);
		twoWay(map, ContentHelper.IX_VISUALIZES, ContentHelper.IX_ISVISUALIZEDBY);
		twoWay(map, ContentHelper.IX_ISDESCRIBEDBY, ContentHelper.IX_DESCRIBES);
		twoWay(map, ContentHelper.IX_HASOBJECTEXAMPLE, ContentHelper.IX_ISOBJECTEXAMPLEFOR);
		twoWay(map, ContentHelper.IX_ISMENTIONEDBY, ContentHelper.IX_MENTIONS);
		twoWay(map, ContentHelper.IX_REPLACES, ContentHelper.IX_ISREPLACEDBY);

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
		twoWay(map, ContentHelper.IX_CLIENT, ContentHelper.IX_CLIENT_OF);
		twoWay(map, ContentHelper.IX_COMPOSER, ContentHelper.IX_COMPOSER_OF);
		twoWay(map, ContentHelper.IX_AUTHOR, ContentHelper.IX_AUTHOR_OF);
		twoWay(map, ContentHelper.IX_ARCHITECT, ContentHelper.IX_ARCHITECT_OF);
		twoWay(map, ContentHelper.IX_INVENTOR, ContentHelper.IX_INVENTOR_OF);
		twoWay(map, ContentHelper.IX_SCENOGRAPHER, ContentHelper.IX_SCENOGRAPHER_OF);
		twoWay(map, ContentHelper.IX_DESIGNER, ContentHelper.IX_DESIGNER_OF);
		twoWay(map, ContentHelper.IX_PRODUCER, ContentHelper.IX_PRODUCER_OF);
		twoWay(map, ContentHelper.IX_ORGANIZER, ContentHelper.IX_ORGANIZER_OF);
		twoWay(map, ContentHelper.IX_DIRECTOR, ContentHelper.IX_DIRECTOR_OF);
		twoWay(map, ContentHelper.IX_PHOTOGRAPHER, ContentHelper.IX_PHOTOGRAPHER_OF);
		twoWay(map, ContentHelper.IX_PAINTER, ContentHelper.IX_PAINTER_OF);
		twoWay(map, ContentHelper.IX_BUILDER, ContentHelper.IX_BUILDER_OF);
		twoWay(map, ContentHelper.IX_MASTERBUILDER, ContentHelper.IX_MASTERBUILDER_OF);
		twoWay(map, ContentHelper.IX_CONSTRUCTIONCLIENT, ContentHelper.IX_CONSTRUCTIONCLIENT_OF);
		twoWay(map, ContentHelper.IX_ENGRAVER, ContentHelper.IX_ENGRAVER_OF);
		twoWay(map, ContentHelper.IX_MINTMASTER, ContentHelper.IX_MINTMASTER_OF);
		twoWay(map, ContentHelper.IX_ARTIST, ContentHelper.IX_ARTIST_OF);
		twoWay(map, ContentHelper.IX_DESIGNENGINEER, ContentHelper.IX_DESIGNENGINEER_OF);
		twoWay(map, ContentHelper.IX_CARPENTER, ContentHelper.IX_CARPENTER_OF);
		twoWay(map, ContentHelper.IX_MASON, ContentHelper.IX_MASON_OF);
		twoWay(map, ContentHelper.IX_TECHNICIAN, ContentHelper.IX_TECHNICIAN_OF);
		twoWay(map, ContentHelper.IX_PUBLISHER, ContentHelper.IX_PUBLISHER_OF);
		twoWay(map, ContentHelper.IX_PUBLICIST, ContentHelper.IX_PUBLICIST_OF);
		twoWay(map, ContentHelper.IX_MUSICIAN, ContentHelper.IX_MUSICIAN_OF);
		twoWay(map, ContentHelper.IX_ACTORACTRESS, ContentHelper.IX_ACTORACTRESS_OF);
		twoWay(map, ContentHelper.IX_PRINTER, ContentHelper.IX_PRINTER_OF);
		twoWay(map, ContentHelper.IX_SIGNER, ContentHelper.IX_SIGNER_OF);
		twoWay(map, ContentHelper.IX_FINDER, ContentHelper.IX_FINDER_OF);
		twoWay(map, ContentHelper.IX_ABANDONEE, ContentHelper.IX_ABANDONEE_OF);
		twoWay(map, ContentHelper.IX_INTERMEDIARY, ContentHelper.IX_INTERMEDIARY_OF);
		twoWay(map, ContentHelper.IX_BUYER, ContentHelper.IX_BUYER_OF);
		twoWay(map, ContentHelper.IX_SELLER, ContentHelper.IX_SELLER_OF);
		twoWay(map, ContentHelper.IX_GENERALAGENT, ContentHelper.IX_GENERALAGENT_OF);
		twoWay(map, ContentHelper.IX_DONOR, ContentHelper.IX_DONOR_OF);
		twoWay(map, ContentHelper.IX_DEPOSITOR, ContentHelper.IX_DEPOSITOR_OF);
		twoWay(map, ContentHelper.IX_RESELLER, ContentHelper.IX_RESELLER_OF);
		twoWay(map, ContentHelper.IX_INVENTORYTAKER, ContentHelper.IX_INVENTORYTAKER_OF);
		twoWay(map, ContentHelper.IX_EXCAVATOR, ContentHelper.IX_EXCAVATOR_OF);
		twoWay(map, ContentHelper.IX_EXAMINATOR, ContentHelper.IX_EXAMINATOR_OF);
		twoWay(map, ContentHelper.IX_CONSERVATOR, ContentHelper.IX_CONSERVATOR_OF);
		twoWay(map, ContentHelper.IX_ARCHIVECONTRIBUTOR, ContentHelper.IX_ARCHIVECONTRIBUTOR_OF);
		twoWay(map, ContentHelper.IX_INTERVIEWER, ContentHelper.IX_INTERVIEWER_OF);
		twoWay(map, ContentHelper.IX_INFORMANT, ContentHelper.IX_INFORMANT_OF);
		twoWay(map, ContentHelper.IX_PATENTHOLDER, ContentHelper.IX_PATENTHOLDER_OF);
		twoWay(map, ContentHelper.IX_USER, ContentHelper.IX_USER_OF);
		twoWay(map, ContentHelper.IX_SCANNEROPERATOR, ContentHelper.IX_SCANNEROPERATOR_OF);
		twoWay(map, ContentHelper.IX_PICTUREEDITOR, ContentHelper.IX_PICTUREEDITOR_OF);
		twoWay(map, ContentHelper.IX_EMPLOYER, ContentHelper.IX_EMPLOYER_OF);

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
	 */
	public GetRelations(APIServiceProvider serviceProvider, OutputStream out, Map<String, String> params) throws DiagnosticException {
		super(serviceProvider, out, params);
	}

	@Override
	protected void extractParameters() throws MissingParameterException,
			BadParameterException {
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
		relations = new HashSet<>();
		Set<String> itemUrisSet = new HashSet<>();
		final String uri = URI_PREFIX + partialIdentifier;
		getRelationsTransitively(itemUrisSet, uri);
	}

	private void getRelationsTransitively(Set<String> itemUrisSet, String uri) throws DiagnosticException {
		SearchService searchService = serviceProvider.getSearchService();

		// TODO: algoritmen kan behöva finslipas och optimeras tex för poster med många relaterade objekt
		// algoritmen ser fn ut så här - inferSameAs styr steg 1 och 3, default är att inte utföra dem
		// 1. hämta ev post för att få tag på postens sameAs och replaces/isReplacedBy. Kör rekursivt på alla sameAs och replaces/isReplacedBy
		// 2. sök fram källpost(er) och alla relaterade poster (post + ev alla sameAs och deras relaterade)
		// 3. hämta ev de relaterades sameAs och replaces/isReplacedBy och lägg till dessa som relationer

		try {

			// 1)
			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.sourceOnly) {
				getSourceIds(itemUrisSet, uri);
			} else {

				// we can't add uri to itemUrisSet before running getSourceIds,
				// since it will return immediately if the set contains uri,
				// so let's add it here afterwards instead
				itemUrisSet.add(uri);
			}

			// 2)
			SolrQuery query = new SolrQuery();
			query.setRows(maxCount > 0 ? maxCount : Integer.MAX_VALUE); // TODO: kan det bli för många?
			// hämta uri och relationer
			query.addField(ContentHelper.I_IX_RELATIONS);
			query.addField(ContentHelper.IX_ITEMID);
			// bygg söksträng mh källposten/alla källposter
			StringBuilder searchStr = new StringBuilder();
			for (String itemId : itemUrisSet) {
				String escapedItemId = ClientUtils.escapeQueryChars(itemId);
				if (searchStr.length() > 0) {
					searchStr.append(" OR ");
				}
				searchStr.append(ContentHelper.IX_ITEMID + ":").append(escapedItemId).append(" OR ").append(ContentHelper.IX_RELURI).append(":").append(escapedItemId);
			}
			// sök fram källposten/-erna och alla som har relation till den/dem
			query.setQuery(searchStr.toString());

			QueryResponse qr;
			SolrDocumentList docs;
			qr = searchService.query(query);
			docs = qr.getResults();
			for (SolrDocument doc : docs) {
				String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
				boolean isSourceDoc = itemUrisSet.contains(itemId);
				Collection<Object> values = doc.getFieldValues(ContentHelper.I_IX_RELATIONS);
				if (values != null) {
					for (Object value : values) {
						String[] parts = ((String) value).split("\\|");
						if (parts.length != 2) {
							logger.error("Fel på värde för relationsindex för " + itemId + ", ej på korrekt format: " + value);
							continue;
						}
						String orgTypePart = null; // håller orginaltypen om vi gör en inversupplagning
						String typePart = parts[0];
						String uriPart = parts[1];

						if ((ContentHelper.IX_SAMEAS.equals(typePart) || ContentHelper.IX_REPLACES.equals(typePart)) && uriPart.equals(uri)) {
							//läge där sameas/replaces-länken pekar på "det här" objektet, vi måste vända på den
							isSourceDoc = false;
						}

						if (!isSourceDoc) {
							// "bakvänd" länk, eller läge där sameas/replaces-länken pekar på "det här" objektet, vi måste vända på den
							if (!itemUrisSet.contains(uriPart)) {
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
								for (String owRel : relationOneWay) {
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

						// vi vill inte ha med relationer som pekar på "det här" objektet
						if (!uriPart.equals(uri)) {
							String source = isSourceDoc ? SOURCE_DIRECT : SOURCE_REVERSE;

							// protokollet specar att vi har en relation som heter replaces,
							// men i datat står det "multipleReplaces" eftersom solr inte vill
							// låta oss göra om det existerande indexet "replaces" till ett flervärt index,
							// så vi fick skapa ett nytt index med det nya namnet multipleReplaces
							// Här byter vi tillbaka alla multipleReplaces till replaces innan vi skickar
							// listan till api-anroparen.
							typePart = (ContentHelper.IX_REPLACES.equals(typePart) ? "replaces" : typePart);
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
			}

			if (inferSameAs == InferSameAs.yes || inferSameAs == InferSameAs.targetsOnly) {
				// 3)
				// sökning på sameAs/replaces för träffarnas uri:er och skapa relation till dessa också
				for (Relation rel : new HashSet<>(relations)) {
					getTargetRelations(itemUrisSet, uri, rel);
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
		} catch (SolrServerException|IOException e ) {
			e.printStackTrace();
			throw new DiagnosticException("Fel vid metodanrop", "GetRelations.performMethodLogic", e.getMessage(), true);

		}
	}

	private void getTargetRelations(Set<String> itemUrisSet, String sourceUri,  Relation rel) throws SolrServerException, IOException {
		QueryResponse qr;
		SolrDocumentList docs;
		SearchService searchService = serviceProvider.getSearchService();
		SolrQuery query = new SolrQuery();
		final String escapedTargetUri = ClientUtils.escapeQueryChars(rel.getTargetUri());

		// handle sameas and replaces/isReplacedBy
		query.setFields(ContentHelper.IX_ITEMID); // bara itemId här
		query.setQuery(ContentHelper.IX_SAMEAS + ":" + escapedTargetUri + " OR " + ContentHelper.IX_REPLACES + ":" + escapedTargetUri);
		qr = searchService.query(query);
		docs = qr.getResults();

		for (SolrDocument doc : docs) {
			String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
			// ta inte med min uri
			if (itemUrisSet.contains(itemId)) {
				continue;
			}
			final Relation newRelation = new Relation(rel.getRelationType(), itemId, rel.getSource(), rel.getOriginalRelationType());
			if (relations.add(newRelation)) {
				// run this recursively
				getTargetRelations(itemUrisSet, sourceUri, newRelation);
			} else {
				// this is already present, don't run again
				if (logger.isDebugEnabled()) {
					logger.debug("duplicate rel (from same as) " + rel);
				}
			}
		}

		// Ta fram de objekt som träffarna pekar ut med sameAs och replaces/isReplacedBy
		query.setFields(ContentHelper.IX_SAMEAS, ContentHelper.IX_REPLACES);
		query.setQuery(ContentHelper.IX_ITEMID + ":" + escapedTargetUri);

		qr = searchService.query(query);
		docs = qr.getResults();


		for (SolrDocument doc : docs) {
			Collection<Object> sameAsIds = doc.getFieldValues(ContentHelper.IX_SAMEAS);
			if (sameAsIds != null) {
				for (Object sameAsId : sameAsIds) {
					// ta inte med mig själv
					if (!sameAsId.equals(sourceUri)) {
						final Relation newRelation = new Relation(rel.getRelationType(), (String) sameAsId, rel.getSource(), rel.getOriginalRelationType());
						if (relations.add(newRelation)) {
							// new one, let's run this recursively
							getTargetRelations(itemUrisSet, sourceUri, newRelation);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel (from same as part 2) " + rel);
							}
						}
					}
				}
			}

			Collection<Object> replacesIds = doc.getFieldValues(ContentHelper.IX_REPLACES);
			if (replacesIds != null) {
				for (Object replacesId : replacesIds) {
					// ta inte med mig själv
					if (!replacesId.equals(sourceUri)) {
						final Relation newRelation = new Relation(rel.getRelationType(), (String) replacesId, rel.getSource(), rel.getOriginalRelationType());
						if (!relations.add(newRelation)) {
							// new one, let's run this recursively
							getTargetRelations(itemUrisSet, sourceUri, newRelation);
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("duplicate rel (from replaces) " + rel);
							}
						}
					}
				}
			}
		}
	}

	private void getSourceIds(Set<String> itemUrisSet,  String uri) throws SolrServerException, IOException {

		// don't run the same uri more than once
		if (itemUrisSet.contains(uri)) {
			return;
		}
		itemUrisSet.add(uri);
		String escapedUri = ClientUtils.escapeQueryChars(uri);

		SearchService searchService = serviceProvider.getSearchService();
		QueryResponse qr;
		SolrDocumentList docs;

			// 1)
			// hämta andra poster som är samma som denna och lägg till dem som "källposter"
			SolrQuery sameAsQuery = new SolrQuery();
			sameAsQuery.setRows(maxCount > 0 ? maxCount : Integer.MAX_VALUE); // TODO: kan det bli för många?

			sameAsQuery.setFields(ContentHelper.IX_SAMEAS, ContentHelper.IX_REPLACES);
			sameAsQuery.setQuery(ContentHelper.IX_ITEMID + ":" + escapedUri);
			qr = searchService.query(sameAsQuery);
			docs = qr.getResults();

			for (SolrDocument doc : docs) {
				Collection<Object> sameAsIds = doc.getFieldValues(ContentHelper.IX_SAMEAS);
				if (sameAsIds != null) {
					for (Object sameAsId : sameAsIds) {
						getSourceIds(itemUrisSet, (String) sameAsId);
					}
				}
				Collection<Object> replacesIds = doc.getFieldValues(ContentHelper.IX_REPLACES);
				if (replacesIds != null) {
					for (Object replacesId : replacesIds) {
						getSourceIds(itemUrisSet, (String) replacesId);
					}
				}
			}

			// hämta andra poster som säger att de är samma som denna (eller replaces) och lägg till dem som "källposter"
			sameAsQuery.setFields(ContentHelper.IX_ITEMID);
			sameAsQuery.setQuery(ContentHelper.IX_SAMEAS + ":" + escapedUri + " OR " + ContentHelper.IX_REPLACES + ":" + escapedUri);
			qr = searchService.query(sameAsQuery);
			docs = qr.getResults();
			for (SolrDocument doc : docs) {
				String itemId = (String) doc.getFieldValue(ContentHelper.IX_ITEMID);
				getSourceIds(itemUrisSet, itemId);
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
