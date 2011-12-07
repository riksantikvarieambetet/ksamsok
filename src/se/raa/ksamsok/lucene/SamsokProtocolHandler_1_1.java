package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_CHILD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FATHER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASCREATED;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTKEEPER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASFORMERORCURRENTOWNER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HASRIGHTON;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISFORMERORCURRENTKEEPEROF;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ISFORMERORCURRENTOWNEROF;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MOTHER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARENT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_RIGHTHELDBY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_WASCREATEDBY;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.context_pre;
import static se.raa.ksamsok.lucene.SamsokProtocol.uriPrefixKSamsok;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_child;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_father;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_mother;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_bio_parent;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P105B_has_right_on;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P105F_right_held_by;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P49B_is_former_or_current_keeper_of;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P49F_has_former_or_current_keeper;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P51B_is_former_or_current_owner_of;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P51F_has_former_or_current_owner;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P94B_was_created_by;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_cidoc_P94F_has_created;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSameAs;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jrdf.graph.Graph;
import org.jrdf.graph.SubjectNode;

public class SamsokProtocolHandler_1_1 extends SamsokProtocolHandler_0_TO_1_0 {

	private static final Logger classLogger = getClassLogger();

	// map med uri -> värde för indexering
	private static final Map<String,String> uriValues_1_1_TO;
	// relationsmap
	private static final Map<String, URI> relationsMap_1_1_TO;
	// kontextrelationsmap
	private static final Map<String, URI> contextRelationsMap_1_1_TO;

	// kontexttyper
	private static final Map<String, String> contextTypes_1_1_TO;
	// superkontexttyper
	private static final Map<String, String> superContextTypes_1_1_TO;

	static {
		//Object o = new String[][] { { "", "" } };
		// TODO: hantera nya kontexttyperna
		final Map<String,String> contextTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextTypeValues);
		contextTypes_1_1_TO = Collections.unmodifiableMap(contextTypeValues);

		// kontextsupertyper
		final Map<String,String> contextSuperTypeValues = new HashMap<String,String>();
		RDFUtil.readURIValueResource(PATH + "contextsupertype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextSuperTypeValues);
		superContextTypes_1_1_TO = Collections.unmodifiableMap(contextSuperTypeValues);

		Map<String,String> values = new HashMap<String,String>();
		// läs in uri-värden för uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "entitysupertype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		values.putAll(contextTypes_1_1_TO);
		values.putAll(superContextTypes_1_1_TO);
		//RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, values);

		uriValues_1_1_TO = Collections.unmodifiableMap(values);

		// utgå från tidigare version
		Map<String, URI> relMap = new HashMap<String, URI>(relationsMap_0_TO_1_0);
		// TODO: tillåta dessa åt det här hållet? Kanske bättre att de bara finns i kontextet åt andra hållet?
		// cidoc-crm
		// hämta ut has created (0M)
		relMap.put(IX_HASCREATED, uri_cidoc_P94F_has_created);
		// hämta ut current or former owner of (0M)
		relMap.put(IX_ISFORMERORCURRENTOWNEROF, uri_cidoc_P51B_is_former_or_current_owner_of);
		// hämta ut has former or current keeper of (0M)
		relMap.put(IX_ISFORMERORCURRENTKEEPEROF, uri_cidoc_P49B_is_former_or_current_keeper_of);
		// hämta ut has right on (0M)
		relMap.put(IX_HASRIGHTON, uri_cidoc_P105B_has_right_on);

		// hämta ut had participant (0M)
		relMap.put(ContentHelper.IX_HADPARTICIPANT, SamsokProtocol.uri_cidoc_P11F_had_participant);
		// hämta ut participated in (0M)
		relMap.put(ContentHelper.IX_PARTICIPATEDIN, SamsokProtocol.uri_cidoc_P11B_participated_in);
		// hämta ut was present at (0M)
		relMap.put(ContentHelper.IX_WASPRESENTAT, SamsokProtocol.uri_cidoc_P12B_was_present_at);
		// hämta ut occured in the presence of (0M)
		relMap.put(ContentHelper.IX_OCCUREDINTHEPRESENCEOF, SamsokProtocol.uri_cidoc_P12F_occurred_in_the_presence_of);

		// bio
		// hämta ut child (01)
		relMap.put(IX_CHILD, uri_bio_child);
		// hämta ut parent (01)
		relMap.put(IX_PARENT, uri_bio_parent);
		// hämta ut mother (01)
		relMap.put(IX_MOTHER, uri_bio_mother);
		// hämta ut father (01)
		relMap.put(IX_FATHER, uri_bio_father);

		relationsMap_1_1_TO = Collections.unmodifiableMap(relMap);

		// kontextrelationerna
		Map<String, URI> contextRelMap = new HashMap<String, URI>();

		contextRelMap.put(IX_HASFORMERORCURRENTKEEPER, uri_cidoc_P49F_has_former_or_current_keeper);
		contextRelMap.put(IX_HASFORMERORCURRENTOWNER, uri_cidoc_P51F_has_former_or_current_owner);
		contextRelMap.put(IX_WASCREATEDBY, uri_cidoc_P94B_was_created_by);
		contextRelMap.put(IX_RIGHTHELDBY, uri_cidoc_P105F_right_held_by);

		contextRelationsMap_1_1_TO = Collections.unmodifiableMap(contextRelMap);
	}

	protected SamsokProtocolHandler_1_1(Graph graph, SubjectNode s) {
		super(graph, s);
	}

	@Override
	protected Map<String, String> getURIValues() {
		return uriValues_1_1_TO;
	}

	@Override
	protected Map<String, URI> getTopLevelRelationsMap() {
		return relationsMap_1_1_TO;
	}

	@Override
	public String getRelationTypeNameFromURI(String refUri) {
		// specialhantering av relationer
		String relationType;
		if (uri_rSameAs.toString().equals(refUri)) {
			relationType = ContentHelper.IX_SAMEAS;
		} else {
			relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, uriPrefixKSamsok));
			// TODO: fixa bättre/validera lite
			if (relationType == null) {
				// testa cidoc
				relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_cidoc_crm));
				if (relationType != null) {
					// strippa sifferdelen
					relationType = StringUtils.trimToNull(StringUtils.substringAfter(relationType, "."));
				} else {
					// bios
					relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_bio));
				}
			}
		}
		return relationType;
	}

	@Override
	protected void extractItemInformation() throws Exception {
		super.extractItemInformation();
		// TODO: kontrollera hierarkin också?
		ip.setCurrent(ContentHelper.IX_ITEMSUPERTYPE, true);
		String superType = extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rItemSuperType), ip);
		if (superType == null) {
			throw new Exception("No item supertype for item with identifier " + s.toString());
		}
		// TODO: göra några obligatoriska eller varna om de saknas för agenter
		//       (supertype==agent ovan kan tex användas)
		// nya index för agenter på toppnivå
		ip.setCurrent(ContentHelper.IX_NAMEAUTH);
		extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rNameAuth), ip);
		ip.setCurrent(ContentHelper.IX_NAMEID);
		extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rNameId), ip);
		// TODO: foaf:name innehåller även alternativa namn men man kanske vill ha ett separat
		//       index för detta? foaf innehåller inget sånt tyvärr så det var därför jag stoppade
		//       in alternativa namn i namn-fältet enligt http://viaf.org/viaf/59878606/rdf.xml
		//       skos har alternativt namn som man skulle kunna använda men egentligen berör ju det
		//       koncept, men det kommer vi ju också lägga in framöver så..
		ip.setCurrent(ContentHelper.IX_NAME);
		RDFUtil.extractValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rName), null, ip);
		ip.setCurrent(ContentHelper.IX_FIRSTNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rFirstName), ip);
		ip.setCurrent(ContentHelper.IX_SURNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rSurname), ip);
		ip.setCurrent(ContentHelper.IX_FULLNAME);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rFullName), ip);
		ip.setCurrent(ContentHelper.IX_GENDER);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rGender), ip);
		ip.setCurrent(ContentHelper.IX_TITLE);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rTitle), ip);
		ip.setCurrent(ContentHelper.IX_ORGANIZATION);
		RDFUtil.extractSingleValue(graph, s, getURIRef(elementFactory, SamsokProtocol.uri_rOrganization), ip);
	}
	/**
	 * Extraherar och indexerar typinformation ur en kontextnod.
	 * Hanterar de index som gäller för protokollversion 1.1, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @return kontexttyp, kortnamn
	 * @throws Exception vid fel
	 */
	@Override
	protected String[] extractContextTypeAndLabelInformation(SubjectNode cS, String identifier) throws Exception {

		// TODO: kontrollera hierarkin också (att produce bara får finnas under create tex)?
		String contextSuperTypeURI = extractSingleValue(graph, cS, getURIRef(elementFactory, SamsokProtocol.uri_rContextSuperType), null);
		if (contextSuperTypeURI == null) {
			throw new Exception("No supertype for context for item with identifier " + identifier);
		}
		String contextSuperType = StringUtils.substringAfter(contextSuperTypeURI, SamsokProtocol.contextsuper_pre);
		if (StringUtils.isEmpty(contextSuperType)) {
			throw new Exception("The context supertype URI " + contextSuperTypeURI +
					" does not start with " + SamsokProtocol.contextsuper_pre +
					" for item with identifier " + identifier);
		}
		ip.setCurrent(ContentHelper.IX_CONTEXTSUPERTYPE);
		ip.addToDoc(contextSuperType);

		// hämta ut vilket kontext vi är i 
		String contextType;
		String contextTypeURI = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextType), null);
		if (contextTypeURI != null) {
			String defaultLabel = contextTypes_1_1_TO.get(contextTypeURI);
			if (defaultLabel == null) {
				throw new Exception("The context type URI " + contextTypeURI + " is not valid for 1.1");
			}
			contextType = StringUtils.substringAfter(contextTypeURI, context_pre);
			if (StringUtils.isEmpty(contextType)) {
				throw new Exception("The context type URI " + contextTypeURI +
						" does not start with " + context_pre +
						" for item with identifier " + identifier);
			}
			ip.setCurrent(IX_CONTEXTTYPE);
			ip.addToDoc(contextType);
			// ta först en inskickad label, och annars defaultvärdet
			String contextLabel = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextLabel), ip);
			if (contextLabel == null) {
				contextLabel = defaultLabel;
			}
			ip.setCurrent(IX_CONTEXTLABEL);
			ip.addToDoc(contextLabel);
		} else {
			throw new Exception("No context type for node " + cS + " for " + identifier);
		}
		return new String[] { contextSuperType, contextType };
	}

	@Override
	protected void extractContextActorInformation(SubjectNode cS,
			String[] contextTypes, List<String> relations) throws Exception {
		super.extractContextActorInformation(cS, contextTypes, relations);
		// hantera relationer i kontexten
		extractContextRelationInformation(cS, contextTypes, relations);
	}

	protected void extractContextRelationInformation(SubjectNode cS, String[] contextTypes,
			List<String> relations) throws Exception {
		extractContextLevelRelations(cS, relations);
	}

	/**
	 * Ger map med giltiga toppnivårelationer nycklat på indexnamn.
	 * 
	 * Överlagra i subklasser vid behov.
	 * @return map med toppnivårelationer
	 */
	protected Map<String, URI> getContextLevelRelationsMap() {
		return contextRelationsMap_1_1_TO;
	}

	/**
	 * Extraherar och indexerar kontextnivårelationer som hämtas via
	 * {@linkplain #getContextLevelRelationsMap()}.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param relations lista med relationer för specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractContextLevelRelations(SubjectNode cS, List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getContextLevelRelationsMap();
		extractRelationsFromNode(cS, relationsMap, relations);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}
}
