package se.raa.ksamsok.lucene;

import java.net.URI;

public class SamsokProtocol {

	static final String uriPrefix = "http://kulturarvsdata.se/";
	static final String uriPrefixKSamsok = uriPrefix + "ksamsok#";
	
	static final String uriPrefixFoaf = "http://xmlns.com/foaf/0.1/";
	static final String uriPrefixMindswap = "http://www.mindswap.org/2003/owl/geo/geoFeatures20040307.owl#";

	static final String uriPrefixDcterms = "http://purl.org/dc/terms/";

	static final URI uri_rdfType = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	static final URI uri_samsokEntity = URI.create(uriPrefixKSamsok + "Entity");

	// protokollversion
	static final URI uri_rKsamsokVersion = URI.create(uriPrefixKSamsok + "ksamsokVersion");

	// "tagnamn"
	static final URI uri_rServiceName = URI.create(uriPrefixKSamsok + "serviceName");
	static final URI uri_rServiceOrganization = URI.create(uriPrefixKSamsok + "serviceOrganization");
	static final URI uri_rCreatedDate = URI.create(uriPrefixKSamsok + "createdDate");
	static final URI uri_rBuilddDate = URI.create(uriPrefixKSamsok + "buildDate");
	static final URI uri_rLastChangedDate = URI.create(uriPrefixKSamsok + "lastChangedDate");
	static final URI uri_rItemTitle = URI.create(uriPrefixKSamsok + "itemTitle");
	static final URI uri_rItemLabel = URI.create(uriPrefixKSamsok + "itemLabel");
	static final URI uri_rItemType = URI.create(uriPrefixKSamsok + "itemType");
	static final URI uri_rItemSuperType = URI.create(uriPrefixKSamsok + "itemSuperType");
	static final URI uri_rItemClass = URI.create(uriPrefixKSamsok + "itemClass");
	static final URI uri_rItemClassName = URI.create(uriPrefixKSamsok + "itemClassName");
	static final URI uri_rItemName = URI.create(uriPrefixKSamsok + "itemName");
	static final URI uri_rItemSpecification = URI.create(uriPrefixKSamsok + "itemSpecification");
	static final URI uri_rItemKeyWord = URI.create(uriPrefixKSamsok + "itemKeyWord");
	static final URI uri_rItemMotiveWord = URI.create(uriPrefixKSamsok + "itemMotiveWord");
	static final URI uri_rItemMaterial = URI.create(uriPrefixKSamsok + "itemMaterial");
	static final URI uri_rItemTechnique = URI.create(uriPrefixKSamsok + "itemTechnique");
	static final URI uri_rItemStyle = URI.create(uriPrefixKSamsok + "itemStyle");
	static final URI uri_rItemColor = URI.create(uriPrefixKSamsok + "itemColor");
	static final URI uri_rItemNumber = URI.create(uriPrefixKSamsok + "itemNumber");
	static final URI uri_rItemMark = URI.create(uriPrefixKSamsok + "itemMark");
	static final URI uri_rItemInscription = URI.create(uriPrefixKSamsok + "itemInscription");
	static final URI uri_rItemDescription = URI.create(uriPrefixKSamsok + "itemDescription");
	static final URI uri_rItemLicense = URI.create(uriPrefixKSamsok + "itemLicense");
	static final URI uri_rSubject = URI.create(uriPrefixKSamsok + "subject");
	static final URI uri_rCollection = URI.create(uriPrefixKSamsok + "collection");
	static final URI uri_rDataQuality = URI.create(uriPrefixKSamsok + "dataQuality");
	static final URI uri_rMediaType = URI.create(uriPrefixKSamsok + "mediaType");
	static final URI uri_r__Form = URI.create(uriPrefixKSamsok + "form");
	static final URI uri_r__Text = URI.create(uriPrefixKSamsok + "text");
	static final URI uri_r__Desc = URI.create(uriPrefixKSamsok + "desc");
	static final URI uri_r__Name = URI.create(uriPrefixKSamsok + "name");
	static final URI uri_r__Spec = URI.create(uriPrefixKSamsok + "spec");
	static final URI uri_rMaterial = URI.create(uriPrefixKSamsok + "material");
	static final URI uri_rNumber = URI.create(uriPrefixKSamsok + "number");
	static final URI uri_rPres = URI.create(uriPrefixKSamsok + "presentation");
	static final URI uri_rContext = URI.create(uriPrefixKSamsok + "context");
	static final URI uri_rContextType = URI.create(uriPrefixKSamsok + "contextType");
	static final URI uri_rContextSuperType = URI.create(uriPrefixKSamsok + "contextSuperType");
	static final URI uri_rContextLabel = URI.create(uriPrefixKSamsok + "contextLabel");
	static final URI uri_rURL = URI.create(uriPrefixKSamsok + "url");
	static final URI uri_rMuseumdatURL = URI.create(uriPrefixKSamsok + "museumdatUrl");
	static final URI uri_rTheme = URI.create(uriPrefixKSamsok + "theme");

	static final URI uri_rReplaces = URI.create(uriPrefixDcterms + "replaces");

	// special
	static final URI uri_rItemForIndexing = URI.create(uriPrefixKSamsok + "itemForIndexing");

	// relationer
	static final URI uri_rSameAs = URI.create("http://www.w3.org/2002/07/owl#sameAs"); // obs, owl
	static final URI uri_rIsRelatedTo = URI.create(uriPrefixKSamsok + "isRelatedTo");
	static final URI uri_rContainsObject = URI.create(uriPrefixKSamsok + "containsObject");
	static final URI uri_rIsContainedIn = URI.create(uriPrefixKSamsok + "isContainedIn");
	static final URI uri_rHasBeenUsedIn = URI.create(uriPrefixKSamsok + "hasBeenUsedIn");
	static final URI uri_rHasChild = URI.create(uriPrefixKSamsok + "hasChild");
	static final URI uri_rHasParent = URI.create(uriPrefixKSamsok + "hasParent");
	static final URI uri_rHasFind = URI.create(uriPrefixKSamsok + "hasFind");
	static final URI uri_rIsFoundIn = URI.create(uriPrefixKSamsok + "isFoundIn");
	static final URI uri_rHasImage = URI.create(uriPrefixKSamsok + "hasImage");
	static final URI uri_rHasObjectExample = URI.create(uriPrefixKSamsok + "hasObjectExample");
	static final URI uri_rIsObjectExampleFor = URI.create(uriPrefixKSamsok + "isObjectExampleFor");
	static final URI uri_rHasPart = URI.create(uriPrefixKSamsok + "hasPart");
	static final URI uri_rIsPartOf = URI.create(uriPrefixKSamsok + "isPartOf");
	static final URI uri_rIsDescribedBy = URI.create(uriPrefixKSamsok + "isDescribedBy");
	static final URI uri_rDescribes = URI.create(uriPrefixKSamsok + "describes");
	static final URI uri_rIsVisualizedBy = URI.create(uriPrefixKSamsok + "isVisualizedBy");
	static final URI uri_rVisualizes = URI.create(uriPrefixKSamsok + "visualizes");
	static final URI uri_rContainsInformationAbout = URI.create(uriPrefixKSamsok + "containsInformationAbout");
	static final URI uri_rIsMentionedBy = URI.create(uriPrefixKSamsok + "isMentionedBy");
	static final URI uri_rMentions = URI.create(uriPrefixKSamsok + "mentions");

	// relationer, cidoc-crm
	static final String uriPrefix_cidoc_crm = "http://www.cidoc-crm.org/rdfs/cidoc-crm#";
	static final URI uri_cidoc_P11B_participated_in = URI.create(uriPrefix_cidoc_crm + "P11B.participated_in");
	static final URI uri_cidoc_P11F_had_participant = URI.create(uriPrefix_cidoc_crm + "P11F.had_participant");
	static final URI uri_cidoc_P12B_was_present_at = URI.create(uriPrefix_cidoc_crm + "P12B.was_present_at");
	static final URI uri_cidoc_P12F_occurred_in_the_presence_of = URI.create(uriPrefix_cidoc_crm + "P12F.occurred_in_the_presence_of");
	static final URI uri_cidoc_P49B_is_former_or_current_keeper_of = URI.create(uriPrefix_cidoc_crm + "P49B.is_former_or_current_keeper_of");
	static final URI uri_cidoc_P49F_has_former_or_current_keeper = URI.create(uriPrefix_cidoc_crm + "P49F.has_former_or_current_keeper");
	static final URI uri_cidoc_P51B_is_former_or_current_owner_of = URI.create(uriPrefix_cidoc_crm + "P51B.is_former_or_current_owner_of");
	static final URI uri_cidoc_P51F_has_former_or_current_owner = URI.create(uriPrefix_cidoc_crm + "P51F.has_former_or_current_owner");
	static final URI uri_cidoc_P94B_was_created_by = URI.create(uriPrefix_cidoc_crm + "P94B.was_created_by");
	static final URI uri_cidoc_P94F_has_created = URI.create(uriPrefix_cidoc_crm + "P94F.has_created");
	static final URI uri_cidoc_P105B_has_right_on = URI.create(uriPrefix_cidoc_crm + "P105B.has_right_on");
	static final URI uri_cidoc_P105F_right_held_by = URI.create(uriPrefix_cidoc_crm + "P105F.right_held_by");
	static final URI uri_cidoc_P107B_is_current_or_former_member_of = URI.create(uriPrefix_cidoc_crm + "P107B.is_current_or_former_member_of");
	static final URI uri_cidoc_P107F_has_current_or_former_member = URI.create(uriPrefix_cidoc_crm + "P107F.has_current_or_former_member");

	// relationer, bio
	static final String uriPrefix_bio = "http://purl.org/vocab/bio/0.1/";
	static final URI uri_bio_child = URI.create(uriPrefix_bio + "child");
	static final URI uri_bio_parent = URI.create(uriPrefix_bio + "parent");
	static final URI uri_bio_mother = URI.create(uriPrefix_bio + "mother");
	static final URI uri_bio_father = URI.create(uriPrefix_bio + "father");

	// roller (relationer i kontext som berör agenter) enl protokolldokument v0.6
	static final URI uri_rClient = URI.create(uriPrefixKSamsok + "client"); // Beställare
	static final URI uri_rClientOf = URI.create(uriPrefixKSamsok + "clientOf"); // Beställare 
	static final URI uri_rComposer = URI.create(uriPrefixKSamsok + "composer"); // Kompositör
	static final URI uri_rComposerOf = URI.create(uriPrefixKSamsok + "composerOf"); // Kompositör 
	static final URI uri_rAuthor = URI.create(uriPrefixKSamsok + "author"); // Författare
	static final URI uri_rAuthorOf = URI.create(uriPrefixKSamsok + "authorOf"); // Författare 
	static final URI uri_rArchitect = URI.create(uriPrefixKSamsok + "architect"); // Arkitekt
	static final URI uri_rArchitectOf = URI.create(uriPrefixKSamsok + "architectOf"); // Arkitekt 
	static final URI uri_rInventor = URI.create(uriPrefixKSamsok + "inventor"); // Uppfinnare
	static final URI uri_rInventorOf = URI.create(uriPrefixKSamsok + "inventorOf"); // Uppfinnare av
	static final URI uri_rScenographer = URI.create(uriPrefixKSamsok + "scenographer"); // Scenograf
	static final URI uri_rScenographerOf = URI.create(uriPrefixKSamsok + "scenographerOf"); // Scenograf av
	static final URI uri_rDesigner = URI.create(uriPrefixKSamsok + "designer"); // Formgivare
	static final URI uri_rDesignerOf = URI.create(uriPrefixKSamsok + "designerOf"); // Formgivare av
	static final URI uri_rProducer = URI.create(uriPrefixKSamsok + "producer"); // Producent
	static final URI uri_rProducerOf = URI.create(uriPrefixKSamsok + "producerOf"); // Producent av
	static final URI uri_rOrganizer = URI.create(uriPrefixKSamsok + "organizer"); // Arrangör
	static final URI uri_rOrganizerOf = URI.create(uriPrefixKSamsok + "organizerOf"); // Arrangör av
	static final URI uri_rDirector = URI.create(uriPrefixKSamsok + "director"); // Regissör
	static final URI uri_rDirectorOf = URI.create(uriPrefixKSamsok + "directorOf"); // Regissör av

	static final URI uri_rPhotographer = URI.create(uriPrefixKSamsok + "photographer"); // Fotograf
	static final URI uri_rPhotographerOf = URI.create(uriPrefixKSamsok + "photographerOf"); // Fotograf av
	static final URI uri_rPainter = URI.create(uriPrefixKSamsok + "painter"); // Målare
	static final URI uri_rPainterOf = URI.create(uriPrefixKSamsok + "painterOf"); // Målare av
	static final URI uri_rBuilder = URI.create(uriPrefixKSamsok + "builder"); // Byggare
	static final URI uri_rBuilderOf = URI.create(uriPrefixKSamsok + "builderOf"); // Byggare av
	static final URI uri_rMasterBuilder = URI.create(uriPrefixKSamsok + "masterBuilder"); // Byggmästare
	static final URI uri_rMasterBuilderOf = URI.create(uriPrefixKSamsok + "masterBuilderOf"); // Byggmästare av
	static final URI uri_rConstructionClient = URI.create(uriPrefixKSamsok + "constructionClient"); // Byggherre
	static final URI uri_rConstructionClientOf = URI.create(uriPrefixKSamsok + "constructionClientOf"); // Byggherre av
	static final URI uri_rEngraver = URI.create(uriPrefixKSamsok + "engraver"); // Gravör
	static final URI uri_rEngraverOf = URI.create(uriPrefixKSamsok + "engraverOf"); // Gravör av
	static final URI uri_rMintmaster = URI.create(uriPrefixKSamsok + "mintmaster"); // Myntmästare
	static final URI uri_rMintmasterOf = URI.create(uriPrefixKSamsok + "mintmasterOf"); // Myntmästare av
	static final URI uri_rArtist = URI.create(uriPrefixKSamsok + "artist"); // Konstnär
	static final URI uri_rArtistOf = URI.create(uriPrefixKSamsok + "artistOf"); // Konstnär av
	static final URI uri_rDesignEngineer = URI.create(uriPrefixKSamsok + "designEngineer"); // Konstruktör
	static final URI uri_rDesignEngineerOf = URI.create(uriPrefixKSamsok + "designEngineerOf"); // Konstruktör av
	static final URI uri_rCarpenter = URI.create(uriPrefixKSamsok + "carpenter"); // Snickare
	static final URI uri_rCarpenterOf = URI.create(uriPrefixKSamsok + "carpenterOf"); // Snickare av
	static final URI uri_rMason = URI.create(uriPrefixKSamsok + "mason"); // Murare
	static final URI uri_rMasonOf = URI.create(uriPrefixKSamsok + "masonOf"); // Murare av
	static final URI uri_rTechnician = URI.create(uriPrefixKSamsok + "technician"); // Tekniker
	static final URI uri_rTechnicianOf = URI.create(uriPrefixKSamsok + "technicianOf"); // Tekniker av
	static final URI uri_rPublisher = URI.create(uriPrefixKSamsok + "publisher"); // Förläggare
	static final URI uri_rPublisherOf = URI.create(uriPrefixKSamsok + "publisherOf"); // Förläggare av
	static final URI uri_rPublicist = URI.create(uriPrefixKSamsok + "publicist"); // Publicist
	static final URI uri_rPublicistOf = URI.create(uriPrefixKSamsok + "publicistOf"); // Publicist av
	static final URI uri_rMusician = URI.create(uriPrefixKSamsok + "musician"); // Musiker
	static final URI uri_rMusicianOf = URI.create(uriPrefixKSamsok + "musicianOf"); // Musiker av
	static final URI uri_rActorActress = URI.create(uriPrefixKSamsok + "actorActress"); // Skådespelare
	static final URI uri_rActorActressOf = URI.create(uriPrefixKSamsok + "actorActressOf"); // Skådespelare av - ACTOR finns redan så det fick bli så här...
	static final URI uri_rPrinter = URI.create(uriPrefixKSamsok + "printer"); // Tryckare
	static final URI uri_rPrinterOf = URI.create(uriPrefixKSamsok + "printerOf"); // Tryckare av
	static final URI uri_rSigner = URI.create(uriPrefixKSamsok + "signer"); // TODO: av egentligen: Påskrift av - Signature by
	static final URI uri_rSignerOf = URI.create(uriPrefixKSamsok + "signerOf");
	static final URI uri_rFinder = URI.create(uriPrefixKSamsok + "finder"); // Upphittare
	static final URI uri_rFinderOf = URI.create(uriPrefixKSamsok + "finderOf"); // Upphittare av
	static final URI uri_rAbandonee = URI.create(uriPrefixKSamsok + "abandonee"); // Förvärvare
	static final URI uri_rAbandoneeOf = URI.create(uriPrefixKSamsok + "abandoneeOf"); // Förvärvare av
	static final URI uri_rIntermediary = URI.create(uriPrefixKSamsok + "intermediary"); // Förmedlare
	static final URI uri_rIntermediaryOf = URI.create(uriPrefixKSamsok + "intermediaryOf"); // Förmedlare av
	static final URI uri_rBuyer = URI.create(uriPrefixKSamsok + "buyer"); // Köpare
	static final URI uri_rBuyerOf = URI.create(uriPrefixKSamsok + "buyerOf"); // Köpare av
	static final URI uri_rSeller = URI.create(uriPrefixKSamsok + "seller"); // Säljare
	static final URI uri_rSellerOf = URI.create(uriPrefixKSamsok + "sellerOf"); // Säljare av
	static final URI uri_rGeneralAgent = URI.create(uriPrefixKSamsok + "generalAgent"); // Generalagent
	static final URI uri_rGeneralAgentOf = URI.create(uriPrefixKSamsok + "generalAgentOf"); // Generalagent av
	static final URI uri_rDonor = URI.create(uriPrefixKSamsok + "donor"); // Givare
	static final URI uri_rDonorOf = URI.create(uriPrefixKSamsok + "donorOf"); // Givare av
	static final URI uri_rDepositor = URI.create(uriPrefixKSamsok + "depositor"); // Deponent
	static final URI uri_rDepositorOf = URI.create(uriPrefixKSamsok + "depositorOf"); // Deponent av
	static final URI uri_rReseller = URI.create(uriPrefixKSamsok + "reseller"); // Återförsäljare
	static final URI uri_rResellerOf = URI.create(uriPrefixKSamsok + "resellerOf"); // Återförsäljare av
	static final URI uri_rInventoryTaker = URI.create(uriPrefixKSamsok + "inventoryTaker"); // Inventerare
	static final URI uri_rInventoryTakerOf = URI.create(uriPrefixKSamsok + "inventoryTakerOf"); // Inventerare av
	static final URI uri_rExcavator = URI.create(uriPrefixKSamsok + "excavator"); // Grävare
	static final URI uri_rExcavatorOf = URI.create(uriPrefixKSamsok + "excavatorOf"); // Grävare av
	static final URI uri_rExaminator = URI.create(uriPrefixKSamsok + "examinator"); // Undersökare
	static final URI uri_rExaminatorOf = URI.create(uriPrefixKSamsok + "examinatorOf"); // Undersökare av
	static final URI uri_rConservator = URI.create(uriPrefixKSamsok + "conservator"); // Konservator
	static final URI uri_rConservatorOf = URI.create(uriPrefixKSamsok + "conservatorOf"); // Konservator av
	static final URI uri_rArchiveContributor = URI.create(uriPrefixKSamsok + "archiveContributor"); // Arkivbildare
	static final URI uri_rArchiveContributorOf = URI.create(uriPrefixKSamsok + "archiveContributorOf"); // Arkivbildare av
	static final URI uri_rInterviewer = URI.create(uriPrefixKSamsok + "interviewer"); // Intervjuare
	static final URI uri_rInterviewerOf = URI.create(uriPrefixKSamsok + "interviewerOf"); // Intervjuare av
	static final URI uri_rInformant = URI.create(uriPrefixKSamsok + "informant"); // Informant
	static final URI uri_rInformantOf = URI.create(uriPrefixKSamsok + "informantOf"); // Informant av

	// NOTE: Ägare - Owner hanteras av cidoc-crm has...owner
	// NOTE: Upphovsrättsägare - Copyright holder hanteras av cidoc-crm rights held by
	static final URI uri_rPatentHolder = URI.create(uriPrefixKSamsok + "patentHolder"); // Patentinnehavare
	static final URI uri_rPatentHolderOf = URI.create(uriPrefixKSamsok + "patentHolderOf"); // Patentinnehavare av
	static final URI uri_rUser = URI.create(uriPrefixKSamsok + "user"); // Brukare
	static final URI uri_rUserOf = URI.create(uriPrefixKSamsok + "userOf"); // Brukare av
	static final URI uri_rScannerOperator = URI.create(uriPrefixKSamsok + "scannerOperator"); // Skanneroperatör
	static final URI uri_rScannerOperatorOf = URI.create(uriPrefixKSamsok + "scannerOperatorOf"); // Skanneroperatör av
	static final URI uri_rPictureEditor = URI.create(uriPrefixKSamsok + "picureEditor"); // Bildredaktör
	static final URI uri_rPictureEditorOf = URI.create(uriPrefixKSamsok + "picureEditorOf"); // Bildredaktör av

	//	TODO: byta nån? samma: Uppdragsgivare - Employer
	//	                       Arbetsgivare - Employer
	static final URI uri_rEmployer = URI.create(uriPrefixKSamsok + "employer"); // Arbetsgivare/uppdragsgivare
	static final URI uri_rEmployerOf = URI.create(uriPrefixKSamsok + "employerOf");
	static final URI uri_rMarriedTo = URI.create(uriPrefixKSamsok + "marriedTo"); // samma invers

	// var-kontext
	static final URI uri_rContinentName = URI.create(uriPrefixKSamsok + "continentName");
	static final URI uri_rCountryName = URI.create(uriPrefixKSamsok + "countryName");
	static final URI uri_rPlaceName = URI.create(uriPrefixKSamsok + "placeName");
	static final URI uri_rCadastralUnit = URI.create(uriPrefixKSamsok + "cadastralUnit");
	static final URI uri_rPlaceTermId = URI.create(uriPrefixKSamsok + "placeTermId");
	static final URI uri_rPlaceTermAuth = URI.create(uriPrefixKSamsok + "placeTermAuth");
	static final URI uri_rPlaceTerm = URI.create(uriPrefixKSamsok + "placeTerm");
	static final URI uri_rCountyName = URI.create(uriPrefixKSamsok + "countyName");
	static final URI uri_rMunicipalityName = URI.create(uriPrefixKSamsok + "municipalityName");
	static final URI uri_rProvinceName = URI.create(uriPrefixKSamsok + "provinceName");
	static final URI uri_rParishName = URI.create(uriPrefixKSamsok + "parishName");
	static final URI uri_rCountry = URI.create(uriPrefixMindswap + "country");
	static final URI uri_rCounty = URI.create(uriPrefixMindswap + "county");
	static final URI uri_rMunicipality = URI.create(uriPrefixMindswap + "municipality");
	static final URI uri_rProvince = URI.create(uriPrefixMindswap + "province");
	static final URI uri_rParish = URI.create(uriPrefixMindswap + "parish");
	static final URI uri_rCoordinates = URI.create(uriPrefixKSamsok + "coordinates");
	
	static final URI uri_rFirstName = URI.create(uriPrefixFoaf + "firstName");
	static final URI uri_rSurname = URI.create(uriPrefixFoaf + "surname");
	static final URI uri_rFullName = URI.create(uriPrefixFoaf + "fullName");
	static final URI uri_rName = URI.create(uriPrefixFoaf + "name");
	static final URI uri_rGender = URI.create(uriPrefixFoaf + "gender");
	static final URI uri_rOrganization = URI.create(uriPrefixFoaf + "organization");
	static final URI uri_rTitle = URI.create(uriPrefixFoaf + "title");
	static final URI uri_rNameId = URI.create(uriPrefixKSamsok + "nameId");
	static final URI uri_rNameAuth = URI.create(uriPrefixKSamsok + "nameAuth");
	static final URI uri_rAgent = URI.create(uriPrefixKSamsok + "agent");

	// när-kontext
	static final URI uri_rFromTime = URI.create(uriPrefixKSamsok + "fromTime");
	static final URI uri_rToTime = URI.create(uriPrefixKSamsok + "toTime");
	static final URI uri_rFromPeriodName = URI.create(uriPrefixKSamsok + "fromPeriodName");
	static final URI uri_rToPeriodName = URI.create(uriPrefixKSamsok + "toPeriodName");
	public static final URI uri_rFromPeriodId = URI.create(uriPrefixKSamsok + "fromPeriodId");
	static final URI uri_rToPeriodId = URI.create(uriPrefixKSamsok + "toPeriodId");
	static final URI uri_rFromPeriod = URI.create(uriPrefixKSamsok + "fromPeriod");
	static final URI uri_rToPeriod = URI.create(uriPrefixKSamsok + "toPeriod");
	static final URI uri_rPeriodAuth = URI.create(uriPrefixKSamsok + "periodAuth");
	static final URI uri_rEventName = URI.create(uriPrefixKSamsok + "eventName");
	static final URI uri_rEventAuth = URI.create(uriPrefixKSamsok + "eventAuth");
	static final URI uri_rEvent = URI.create(uriPrefixKSamsok + "event");
	//static final URI uri_rTimeText = URI.create(uriPrefixKSamsok + "timeText");

	// övriga
	static final URI uri_rThumbnail = URI.create(uriPrefixKSamsok + "thumbnail");
	static final URI uri_rImage = URI.create(uriPrefixKSamsok + "image");
	static final URI uri_rMedia = URI.create(uriPrefixKSamsok + "media"); // 1.11
	static final URI uri_rMediaLicense = URI.create(uriPrefixKSamsok + "mediaLicense");
	static final URI uri_rMediaLicenseUrl = URI.create(uriPrefixKSamsok + "mediaLicenseUrl"); // 1.11
	static final URI uri_rMediaMotiveWord = URI.create(uriPrefixKSamsok + "mediaMotiveWord");
	static final URI uri_rThumbnailSource = URI.create(uriPrefixKSamsok + "thumbnailSource");
	static final URI uri_rLowresSource = URI.create(uriPrefixKSamsok + "lowresSource");
	static final URI uri_rHighresSource = URI.create(uriPrefixKSamsok + "highresSource");
	
	static final URI uri_rByline = URI.create(uriPrefixKSamsok + "byline");
	static final URI uri_rCopyright = URI.create(uriPrefixKSamsok + "copyright");

	// geo
	static final String aukt_country_pre = uriPrefix + "resurser/aukt/geo/country#";
	static final String aukt_county_pre = uriPrefix + "resurser/aukt/geo/county#";
	static final String aukt_municipality_pre = uriPrefix + "resurser/aukt/geo/municipality#";
	static final String aukt_province_pre = uriPrefix + "resurser/aukt/geo/province#";
	static final String aukt_parish_pre = uriPrefix + "resurser/aukt/geo/parish#";

	// context
	static final String context_pre = uriPrefix + "resurser/ContextType#";
	static final String contextsuper_pre = uriPrefix + "resurser/ContextSuperType#";

}
