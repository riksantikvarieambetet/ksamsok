package se.raa.ksamsok.lucene;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SamsokProtocolHandler_1_1 extends SamsokProtocolHandler_0_TO_1_0 {

	private static final Logger classLogger = getClassLogger();

	// map med uri -> värde för indexering
	private static final Map<String,String> uriValues_1_1_TO;
	// relationsmap
	private static final Map<String, URI> relationsMap_1_1_TO;
	// kontextrelationsmap
	protected static final Map<String, URI> contextRelationsMap_1_1_TO;

	// kontexttyper
	private static final Map<String, String> contextTypes_1_1_TO;
	// superkontexttyper
	private static final Map<String, String> superContextTypes_1_1_TO;

	static {
		final Map<String,String> contextTypeValues = new HashMap<>();
		RDFUtil.readURIValueResource(PATH + "contexttype_1.1.rdf", SamsokProtocol.uri_rContextLabel, contextTypeValues);
		contextTypes_1_1_TO = Collections.unmodifiableMap(contextTypeValues);

		// kontextsupertyper
		final Map<String,String> contextSuperTypeValues = new HashMap<>();
		RDFUtil.readURIValueResource(PATH + "contextsupertype_1.1.rdf", SamsokProtocol.uri_r__Name, contextSuperTypeValues);
		superContextTypes_1_1_TO = Collections.unmodifiableMap(contextSuperTypeValues);

		Map<String,String> values = new HashMap<>();
		// läs in uri-värden för uppslagning
		RDFUtil.readURIValueResource(PATH + "entitytype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "entitysupertype_1.1.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "subject.rdf", SamsokProtocol.uri_r__Name, values);
		RDFUtil.readURIValueResource(PATH + "dataquality.rdf", SamsokProtocol.uri_r__Name, values);
		values.putAll(contextTypes_1_1_TO);
		values.putAll(superContextTypes_1_1_TO);

		uriValues_1_1_TO = Collections.unmodifiableMap(values);

		// utgå från tidigare version och lägg till de nytillkomna
		Map<String, URI> relMap = new HashMap<>(relationsMap_0_TO_1_0);

		// hämta ut is mentioned by (0M)
		relMap.put(ContentHelper.IX_ISMENTIONEDBY, SamsokProtocol.uri_rIsMentionedBy);
		// hämta ut replaces (0M)
		relMap.put(ContentHelper.IX_REPLACES, SamsokProtocol.uri_rReplaces);
		// hämta ut mentions (0M)
		relMap.put(ContentHelper.IX_MENTIONS, SamsokProtocol.uri_rMentions);
		// hämta ut is contained in (0M)
		relMap.put(ContentHelper.IX_ISCONTAINEDIN, SamsokProtocol.uri_rIsContainedIn);
		relMap.put(ContentHelper.IX_ISOBJECTEXAMPLEFOR, SamsokProtocol.uri_rIsObjectExampleFor);
		relMap.put(ContentHelper.IX_DESCRIBES, SamsokProtocol.uri_rDescribes);

		// cidoc
		relMap.put(ContentHelper.IX_PARTICIPATEDIN, SamsokProtocol.uri_cidoc_P11B_participated_in);
		relMap.put(ContentHelper.IX_HADPARTICIPANT, SamsokProtocol.uri_cidoc_P11F_had_participant);
		relMap.put(ContentHelper.IX_WASPRESENTAT, SamsokProtocol.uri_cidoc_P12B_was_present_at);
		relMap.put(ContentHelper.IX_OCCUREDINTHEPRESENCEOF, SamsokProtocol.uri_cidoc_P12F_occurred_in_the_presence_of);
		relMap.put(ContentHelper.IX_ISCURRENTORFORMERMEMBEROF, SamsokProtocol.uri_cidoc_P107B_is_current_or_former_member_of);
		relMap.put(ContentHelper.IX_HASCURRENTORFORMERMEMBER, SamsokProtocol.uri_cidoc_P107F_has_current_or_former_member);

		// bio
		relMap.put(ContentHelper.IX_CHILD, SamsokProtocol.uri_bio_child);
		relMap.put(ContentHelper.IX_PARENT, SamsokProtocol.uri_bio_parent);
		relMap.put(ContentHelper.IX_MOTHER, SamsokProtocol.uri_bio_mother);
		relMap.put(ContentHelper.IX_FATHER, SamsokProtocol.uri_bio_father);

		relationsMap_1_1_TO = Collections.unmodifiableMap(relMap);

		// kontextrelationerna
		Map<String, URI> contextRelMap = new HashMap<>();

		contextRelMap.put(ContentHelper.IX_ISFORMERORCURRENTKEEPEROF, SamsokProtocol.uri_cidoc_P49B_is_former_or_current_keeper_of);
		contextRelMap.put(ContentHelper.IX_HASFORMERORCURRENTKEEPER, SamsokProtocol.uri_cidoc_P49F_has_former_or_current_keeper);
		contextRelMap.put(ContentHelper.IX_ISFORMERORCURRENTOWNEROF, SamsokProtocol.uri_cidoc_P51B_is_former_or_current_owner_of);
		contextRelMap.put(ContentHelper.IX_HASFORMERORCURRENTOWNER, SamsokProtocol.uri_cidoc_P51F_has_former_or_current_owner);
		contextRelMap.put(ContentHelper.IX_WASCREATEDBY, SamsokProtocol.uri_cidoc_P94B_was_created_by);
		contextRelMap.put(ContentHelper.IX_HASCREATED, SamsokProtocol.uri_cidoc_P94F_has_created);
		contextRelMap.put(ContentHelper.IX_HASRIGHTON, SamsokProtocol.uri_cidoc_P105B_has_right_on);
		contextRelMap.put(ContentHelper.IX_RIGHTHELDBY, SamsokProtocol.uri_cidoc_P105F_right_held_by);
		contextRelMap.put(ContentHelper.IX_CLIENT, SamsokProtocol.uri_rClient);
		contextRelMap.put(ContentHelper.IX_CLIENT_OF, SamsokProtocol.uri_rClientOf);
		contextRelMap.put(ContentHelper.IX_AUTHOR, SamsokProtocol.uri_rAuthor);
		contextRelMap.put(ContentHelper.IX_AUTHOR_OF, SamsokProtocol.uri_rAuthorOf);
		contextRelMap.put(ContentHelper.IX_ARCHITECT, SamsokProtocol.uri_rArchitect);
		contextRelMap.put(ContentHelper.IX_ARCHITECT_OF, SamsokProtocol.uri_rArchitectOf);
		contextRelMap.put(ContentHelper.IX_INVENTOR, SamsokProtocol.uri_rInventor);
		contextRelMap.put(ContentHelper.IX_INVENTOR_OF, SamsokProtocol.uri_rInventorOf);
		contextRelMap.put(ContentHelper.IX_SCENOGRAPHER, SamsokProtocol.uri_rScenographer);
		contextRelMap.put(ContentHelper.IX_SCENOGRAPHER_OF, SamsokProtocol.uri_rScenographerOf);
		contextRelMap.put(ContentHelper.IX_DESIGNER, SamsokProtocol.uri_rDesigner);
		contextRelMap.put(ContentHelper.IX_DESIGNER_OF, SamsokProtocol.uri_rDesignerOf);
		contextRelMap.put(ContentHelper.IX_PRODUCER, SamsokProtocol.uri_rProducer);
		contextRelMap.put(ContentHelper.IX_PRODUCER_OF, SamsokProtocol.uri_rProducerOf);
		contextRelMap.put(ContentHelper.IX_ORGANIZER, SamsokProtocol.uri_rOrganizer);
		contextRelMap.put(ContentHelper.IX_ORGANIZER_OF, SamsokProtocol.uri_rOrganizerOf);
		contextRelMap.put(ContentHelper.IX_DIRECTOR, SamsokProtocol.uri_rDirector);
		contextRelMap.put(ContentHelper.IX_DIRECTOR_OF, SamsokProtocol.uri_rDirectorOf);
		contextRelMap.put(ContentHelper.IX_PHOTOGRAPHER, SamsokProtocol.uri_rPhotographer);
		contextRelMap.put(ContentHelper.IX_PHOTOGRAPHER_OF, SamsokProtocol.uri_rPhotographerOf);
		contextRelMap.put(ContentHelper.IX_PAINTER, SamsokProtocol.uri_rPainter);
		contextRelMap.put(ContentHelper.IX_PAINTER_OF, SamsokProtocol.uri_rPainterOf);
		contextRelMap.put(ContentHelper.IX_BUILDER, SamsokProtocol.uri_rBuilder);
		contextRelMap.put(ContentHelper.IX_BUILDER_OF, SamsokProtocol.uri_rBuilderOf);
		contextRelMap.put(ContentHelper.IX_MASTERBUILDER, SamsokProtocol.uri_rMasterBuilder);
		contextRelMap.put(ContentHelper.IX_MASTERBUILDER_OF, SamsokProtocol.uri_rMasterBuilderOf);
		contextRelMap.put(ContentHelper.IX_CONSTRUCTIONCLIENT, SamsokProtocol.uri_rConstructionClient);
		contextRelMap.put(ContentHelper.IX_CONSTRUCTIONCLIENT_OF, SamsokProtocol.uri_rConstructionClientOf);
		contextRelMap.put(ContentHelper.IX_ENGRAVER, SamsokProtocol.uri_rEngraver);
		contextRelMap.put(ContentHelper.IX_ENGRAVER_OF, SamsokProtocol.uri_rEngraverOf);
		contextRelMap.put(ContentHelper.IX_MINTMASTER, SamsokProtocol.uri_rMintmaster);
		contextRelMap.put(ContentHelper.IX_MINTMASTER_OF, SamsokProtocol.uri_rMintmasterOf);
		contextRelMap.put(ContentHelper.IX_ARTIST, SamsokProtocol.uri_rArtist);
		contextRelMap.put(ContentHelper.IX_ARTIST_OF, SamsokProtocol.uri_rArtistOf);
		contextRelMap.put(ContentHelper.IX_DESIGNENGINEER, SamsokProtocol.uri_rDesignEngineer);
		contextRelMap.put(ContentHelper.IX_DESIGNENGINEER_OF, SamsokProtocol.uri_rDesignEngineerOf);
		contextRelMap.put(ContentHelper.IX_CARPENTER, SamsokProtocol.uri_rCarpenter);
		contextRelMap.put(ContentHelper.IX_CARPENTER_OF, SamsokProtocol.uri_rCarpenterOf);
		contextRelMap.put(ContentHelper.IX_MASON, SamsokProtocol.uri_rMason);
		contextRelMap.put(ContentHelper.IX_MASON_OF, SamsokProtocol.uri_rMasonOf);
		contextRelMap.put(ContentHelper.IX_TECHNICIAN, SamsokProtocol.uri_rTechnician);
		contextRelMap.put(ContentHelper.IX_TECHNICIAN_OF, SamsokProtocol.uri_rTechnicianOf);
		contextRelMap.put(ContentHelper.IX_PUBLISHER, SamsokProtocol.uri_rPublisher);
		contextRelMap.put(ContentHelper.IX_PUBLISHER_OF, SamsokProtocol.uri_rPublisherOf);
		contextRelMap.put(ContentHelper.IX_PUBLICIST, SamsokProtocol.uri_rPublicist);
		contextRelMap.put(ContentHelper.IX_PUBLICIST_OF, SamsokProtocol.uri_rPublicistOf);
		contextRelMap.put(ContentHelper.IX_MUSICIAN, SamsokProtocol.uri_rMusician);
		contextRelMap.put(ContentHelper.IX_MUSICIAN_OF, SamsokProtocol.uri_rMusicianOf);
		contextRelMap.put(ContentHelper.IX_ACTORACTRESS, SamsokProtocol.uri_rActorActress);
		contextRelMap.put(ContentHelper.IX_ACTORACTRESS_OF, SamsokProtocol.uri_rActorActressOf);
		contextRelMap.put(ContentHelper.IX_PRINTER, SamsokProtocol.uri_rPrinter);
		contextRelMap.put(ContentHelper.IX_PRINTER_OF, SamsokProtocol.uri_rPrinterOf);
		contextRelMap.put(ContentHelper.IX_SIGNER, SamsokProtocol.uri_rSigner);
		contextRelMap.put(ContentHelper.IX_SIGNER_OF, SamsokProtocol.uri_rSignerOf);
		contextRelMap.put(ContentHelper.IX_FINDER, SamsokProtocol.uri_rFinder);
		contextRelMap.put(ContentHelper.IX_FINDER_OF, SamsokProtocol.uri_rFinderOf);
		contextRelMap.put(ContentHelper.IX_ABANDONEE, SamsokProtocol.uri_rAbandonee);
		contextRelMap.put(ContentHelper.IX_ABANDONEE_OF, SamsokProtocol.uri_rAbandoneeOf);
		contextRelMap.put(ContentHelper.IX_INTERMEDIARY, SamsokProtocol.uri_rIntermediary);
		contextRelMap.put(ContentHelper.IX_INTERMEDIARY_OF, SamsokProtocol.uri_rIntermediaryOf);
		contextRelMap.put(ContentHelper.IX_BUYER, SamsokProtocol.uri_rBuyer);
		contextRelMap.put(ContentHelper.IX_BUYER_OF, SamsokProtocol.uri_rBuyerOf);
		contextRelMap.put(ContentHelper.IX_SELLER, SamsokProtocol.uri_rSeller);
		contextRelMap.put(ContentHelper.IX_SELLER_OF, SamsokProtocol.uri_rSellerOf);
		contextRelMap.put(ContentHelper.IX_GENERALAGENT, SamsokProtocol.uri_rGeneralAgent);
		contextRelMap.put(ContentHelper.IX_GENERALAGENT_OF, SamsokProtocol.uri_rGeneralAgentOf);
		contextRelMap.put(ContentHelper.IX_DONOR, SamsokProtocol.uri_rDonor);
		contextRelMap.put(ContentHelper.IX_DONOR_OF, SamsokProtocol.uri_rDonorOf);
		contextRelMap.put(ContentHelper.IX_DEPOSITOR, SamsokProtocol.uri_rDepositor);
		contextRelMap.put(ContentHelper.IX_DEPOSITOR_OF, SamsokProtocol.uri_rDepositorOf);
		contextRelMap.put(ContentHelper.IX_RESELLER, SamsokProtocol.uri_rReseller);
		contextRelMap.put(ContentHelper.IX_RESELLER_OF, SamsokProtocol.uri_rResellerOf);
		contextRelMap.put(ContentHelper.IX_INVENTORYTAKER, SamsokProtocol.uri_rInventoryTaker);
		contextRelMap.put(ContentHelper.IX_INVENTORYTAKER_OF, SamsokProtocol.uri_rInventoryTakerOf);
		contextRelMap.put(ContentHelper.IX_EXCAVATOR, SamsokProtocol.uri_rExcavator);
		contextRelMap.put(ContentHelper.IX_EXCAVATOR_OF, SamsokProtocol.uri_rExcavatorOf);
		contextRelMap.put(ContentHelper.IX_EXAMINATOR, SamsokProtocol.uri_rExaminator);
		contextRelMap.put(ContentHelper.IX_EXAMINATOR_OF, SamsokProtocol.uri_rExaminatorOf);
		contextRelMap.put(ContentHelper.IX_CONSERVATOR, SamsokProtocol.uri_rConservator);
		contextRelMap.put(ContentHelper.IX_CONSERVATOR_OF, SamsokProtocol.uri_rConservatorOf);
		contextRelMap.put(ContentHelper.IX_ARCHIVECONTRIBUTOR, SamsokProtocol.uri_rArchiveContributor);
		contextRelMap.put(ContentHelper.IX_ARCHIVECONTRIBUTOR_OF, SamsokProtocol.uri_rArchiveContributorOf);
		contextRelMap.put(ContentHelper.IX_INTERVIEWER, SamsokProtocol.uri_rInterviewer);
		contextRelMap.put(ContentHelper.IX_INTERVIEWER_OF, SamsokProtocol.uri_rInterviewerOf);
		contextRelMap.put(ContentHelper.IX_INFORMANT, SamsokProtocol.uri_rInformant);
		contextRelMap.put(ContentHelper.IX_INFORMANT_OF, SamsokProtocol.uri_rInformantOf);
		contextRelMap.put(ContentHelper.IX_PATENTHOLDER, SamsokProtocol.uri_rPatentHolder);
		contextRelMap.put(ContentHelper.IX_PATENTHOLDER_OF, SamsokProtocol.uri_rPatentHolderOf);
		contextRelMap.put(ContentHelper.IX_USER, SamsokProtocol.uri_rUser);
		contextRelMap.put(ContentHelper.IX_USER_OF, SamsokProtocol.uri_rUserOf);
		contextRelMap.put(ContentHelper.IX_SCANNEROPERATOR, SamsokProtocol.uri_rScannerOperator);
		contextRelMap.put(ContentHelper.IX_SCANNEROPERATOR_OF, SamsokProtocol.uri_rScannerOperatorOf);
		contextRelMap.put(ContentHelper.IX_PICTUREEDITOR, SamsokProtocol.uri_rPictureEditor);
		contextRelMap.put(ContentHelper.IX_PICTUREEDITOR_OF, SamsokProtocol.uri_rPictureEditorOf);
		contextRelMap.put(ContentHelper.IX_EMPLOYER, SamsokProtocol.uri_rEmployer);
		contextRelMap.put(ContentHelper.IX_EMPLOYER_OF, SamsokProtocol.uri_rEmployerOf);
		contextRelMap.put(ContentHelper.IX_MARRIEDTO, SamsokProtocol.uri_rMarriedTo);

		contextRelationsMap_1_1_TO = Collections.unmodifiableMap(contextRelMap);
	}

	protected SamsokProtocolHandler_1_1(Model model, Resource subject) {
		super(model, subject);
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
		if (SamsokProtocol.uri_rSameAs.toString().equals(refUri)) {
			relationType = ContentHelper.IX_SAMEAS;
		} else if (SamsokProtocol.uri_rReplaces.toString().equals(refUri)) {
			relationType = ContentHelper.IX_REPLACES;
		} else {
			relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefixKSamsok));
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
		String superType = RDFUtil.extractSingleValue(model, subject,
				getURIRef(SamsokProtocol.uri_rItemSuperType), ip);
		if (superType == null) {
			throw new Exception("No item supertype for item with identifier " + subject.toString());
		}
		// TODO: göra några obligatoriska eller varna om de saknas för agenter
		//       (supertype==agent ovan kan tex användas)
		// nya index för agenter på toppnivå

		// TODO: foaf:name innehåller även alternativa namn men man kanske vill ha ett separat
		//       index för detta? foaf innehåller inget sånt tyvärr så det var därför jag stoppade
		//       in alternativa namn i namn-fältet enligt http://viaf.org/viaf/59878606/rdf.xml
		//       skos har alternativt namn som man skulle kunna använda men egentligen berör ju det
		//       koncept, men det kommer vi ju också lägga in framöver så..
		ip.setCurrent(ContentHelper.IX_NAME);
		RDFUtil.extractValue(model, subject, getURIRef(SamsokProtocol.uri_rName), null, ip);
		ip.setCurrent(ContentHelper.IX_FIRSTNAME);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rFirstName), ip);
		ip.setCurrent(ContentHelper.IX_SURNAME);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rSurname), ip);
		ip.setCurrent(ContentHelper.IX_FULLNAME);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rFullName), ip);
		ip.setCurrent(ContentHelper.IX_GENDER);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rGender), ip);
		ip.setCurrent(ContentHelper.IX_TITLE);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rTitle), ip);
		ip.setCurrent(ContentHelper.IX_ORGANIZATION);
		RDFUtil.extractSingleValue(model, subject, getURIRef(SamsokProtocol.uri_rOrganization), ip);

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
	protected String[] extractContextTypeAndLabelInformation(Resource cS, String identifier) throws Exception {

		// TODO: kontrollera hierarkin också (att produce bara får finnas under create tex)?
		String contextSuperTypeURI = RDFUtil.extractSingleValue(model, cS,
				getURIRef(SamsokProtocol.uri_rContextSuperType), null);
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
		String contextTypeURI = RDFUtil.extractSingleValue(model, cS,
				getURIRef(SamsokProtocol.uri_rContextType), null);
		if (contextTypeURI != null) {
			String defaultLabel = contextTypes_1_1_TO.get(contextTypeURI);
			if (defaultLabel == null) {
				throw new Exception("The context type URI " + contextTypeURI + " is not valid for 1.1");
			}
			contextType = StringUtils.substringAfter(contextTypeURI, SamsokProtocol.context_pre);
			if (StringUtils.isEmpty(contextType)) {
				throw new Exception("The context type URI " + contextTypeURI +
						" does not start with " + SamsokProtocol.context_pre +
						" for item with identifier " + identifier);
			}
			ip.setCurrent(ContentHelper.IX_CONTEXTTYPE);
			ip.addToDoc(contextType);
			// ta först en inskickad label, och annars defaultvärdet
			String contextLabel = RDFUtil.extractSingleValue(model, cS,
					getURIRef(SamsokProtocol.uri_rContextLabel), ip);
			if (contextLabel == null) {
				contextLabel = defaultLabel;
			}
			ip.setCurrent(ContentHelper.IX_CONTEXTLABEL);
			ip.addToDoc(contextLabel);
		} else {
			throw new Exception("No context type for node " + cS + " for " + identifier);
		}
		return new String[] { contextSuperType, contextType };
	}

	@Override
	protected void extractContextActorInformation(Resource cS,
			String[] contextTypes, List<String> relations) throws Exception {
		super.extractContextActorInformation(cS, contextTypes, relations);
		// hantera relationer i kontexten
		extractContextRelationInformation(cS, contextTypes, relations);
	}

	protected void extractContextRelationInformation(Resource cS, String[] contextTypes,
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
	protected void extractContextLevelRelations(Resource cS, List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getContextLevelRelationsMap();
		extractRelationsFromNode(cS, relationsMap, relations);
	}

	@Override
	public Logger getLogger() {
		return classLogger;
	}
}
