package se.raa.ksamsok.lucene;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import se.raa.ksamsok.harvest.ExtractedInfo;
import se.raa.ksamsok.harvest.HarvestService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Basklass för innehållshantering av tjänstedata och index samt diverse hjälpmetoder.
 */
public abstract class ContentHelper {

	// TODO: inför info om kardinalitet och uri:s ska slås upp eller ej... och automatifiera ip mfl
	// mer?

	// diverse systemtermer
	public static final String CONTEXT_SET_SAMSOK = "samsok";
	public static final String CONTEXT_SET_SAMSOK_IDENTIFIER = "http://kulturarvsdata.se/resurser/contextSets/samsok/1.0/";
	public static final String CONTEXT_SET_REC = "rec";
	public static final String CONTEXT_SET_REC_IDENTIFIER = "info:srw/cql-context-set/2/rec-1.1";
	public static final String IX_REC_IDENTIFIER = "identifier"; // identifierare sru1.2 samma som
																	// itemId

	// systeminterna indexnamn

	public static final String I_IX_PRES = "_pres";
	public static final String I_IX_RDF = "_rdf";
	public static final String I_IX_SERVICE = "_service";
	public static final String I_IX_HTML_URL = "_htmlurl";
	public static final String I_IX_MUSEUMDAT_URL = "_museumdaturl";
	public static final String I_IX_LON = "_lon";
	public static final String I_IX_LAT = "_lat";
	public static final String I_IX_RELATIONS = "_relations";

	// generella

	public static final String IX_TEXT = "text"; // fritext för "alla" fält
	public static final String IX_STRICT = "strict"; // fritext för "alla" fält, ostammat
	public static final String IX_SERVICENAME = "serviceName";
	public static final String IX_SERVICEORGANISATION = "serviceOrganization";
	public static final String IX_CREATEDDATE = "createdDate";
	public static final String IX_LASTCHANGEDDATE = "lastChangedDate";
	public static final String IX_ADDEDTOINDEXDATE = "addedToIndexDate"; // special
	public static final String IX_PROTOCOLVERSION = "stringProtocolVersion";

	// entitet

	public static final String IX_ITEM = "item"; // fritext för alla item-fält
	public static final String IX_ITEMID = "itemId"; // identifierare, rdf:about
	public static final String IX_SUBJECT = "subject";
	public static final String IX_COLLECTION = "collection";
	public static final String IX_MEDIATYPE = "mediaType";
	public static final String IX_DATAQUALITY = "dataQuality";
	public static final String IX_ITEMTYPE = "itemType";
	public static final String IX_ITEMSUPERTYPE = "itemSuperType";
	public static final String IX_ITEMCLASS = "itemClass";
	public static final String IX_ITEMCLASSNAME = "itemClassName";
	public static final String IX_ITEMNAME = "itemName";
	public static final String IX_ITEMSPECIFICATION = "itemSpecification";
	public static final String IX_ITEMTITLE = "itemTitle";
	public static final String IX_ITEMLABEL = "itemLabel";
	public static final String IX_ITEMMARK = "itemMark";
	public static final String IX_ITEMINSCRIPTION = "itemInscription";
	public static final String IX_ITEMDESCRIPTION = "itemDescription";
	public static final String IX_ITEMKEYWORD = "itemKeyWord";
	public static final String IX_ITEMMOTIVEWORD = "itemMotiveWord";
	public static final String IX_ITEMMATERIAL = "itemMaterial";
	public static final String IX_ITEMTECHNIQUE = "itemTechnique";
	public static final String IX_ITEMSTYLE = "itemStyle";
	public static final String IX_ITEMCOLOR = "itemColor";
	public static final String IX_ITEMNUMBER = "itemNumber";
	public static final String IX_ITEMLICENSE = "itemLicense";
	public static final String IX_ITEMMEASUREMENT = "itemMeasurement";
	public static final String IX_THEME = "theme";
	public static final String IX_BUILD_DATE = "buildDate";
	public static final String IX_THUMBNAIL = "thumbnail";

	// tider, platser, personer
	// Sammanhang enligt ändlig lista. Sammanhanget gäller för tider, platser och
	// personer/organisationer. Listan på sammanhang hittar du här:
	// http://kulturarvsdata.se/resurser/Context
	public static final String IX_CONTEXTLABEL = "contextLabel";
	public static final String IX_CONTEXTTYPE = "contextType";
	public static final String IX_CONTEXTSUPERTYPE = "contextSuperType";

	// tider
	public static final String IX_TIME = "time"; // fritext i alla tidsfält
	public static final String IX_FROMTIME = "fromTime";
	public static final String IX_TOTIME = "toTime";
	public static final String IX_FROMPERIOD = "fromPeriod";
	public static final String IX_TOPERIOD = "toPeriod";
	public static final String IX_FROMPERIODNAME = "fromPeriodName";
	public static final String IX_TOPERIODNAME = "toPeriodName";
	public static final String IX_FROMPERIODID = "fromPeriodId";
	public static final String IX_TOPERIODID = "toPeriodId";
	public static final String IX_PERIODAUTH = "periodAuth";
	public static final String IX_EVENTNAME = "eventName";
	public static final String IX_EVENTAUTH = "eventAuth";
	public static final String IX_EVENT = "event";
	// public static final String IX_TIMETEXT = "timeText";

	// platser
	public static final String IX_PLACE = "place"; // fritext i alla platsfält
	public static final String IX_CONTINENTNAME = "continentName";
	public static final String IX_COUNTRY = "country";
	public static final String IX_COUNTY = "county";
	public static final String IX_PROVINCE = "province";
	public static final String IX_MUNICIPALITY = "municipality";
	public static final String IX_PARISH = "parish";
	public static final String IX_COUNTRYNAME = "countryName";
	public static final String IX_COUNTYNAME = "countyName";
	public static final String IX_PROVINCENAME = "provinceName";
	public static final String IX_MUNICIPALITYNAME = "municipalityName";
	public static final String IX_PARISHNAME = "parishName";
	public static final String IX_PLACENAME = "placeName";
	public static final String IX_CADASTRALUNIT = "cadastralUnit";
	public static final String IX_PLACETERMID = "placeTermId";
	public static final String IX_PLACETERMAUTH = "placeTermAuth";
	public static final String IX_PLACETERM = "placeTerm";

	// public static final String IX_COORDINATES = "coordinates";

	// personer
	public static final String IX_AGENT = "agent";
	public static final String IX_ACTOR = "actor"; // alla fält sammanslagna (ej fritext dock!)
	public static final String IX_FIRSTNAME = "firstName";
	public static final String IX_SURNAME = "surname";
	public static final String IX_NAME = "name";
	public static final String IX_FULLNAME = "fullName";
	public static final String IX_GENDER = "gender";
	public static final String IX_ORGANIZATION = "organization";
	public static final String IX_TITLE = "title";
	public static final String IX_NAMEID = "nameId";
	public static final String IX_NAMEAUTH = "nameAuth";

	// relationer
	public static final String IX_RELURI = "relUri"; // slask-indexet
	public static final String IX_SAMEAS = "sameAs"; // owl:sameAs
	public static final String IX_ISRELATEDTO = "isRelatedTo";
	public static final String IX_CONTAINSOBJECT = "containsObject";
	public static final String IX_ISCONTAINEDIN = "isContainedIn";
	public static final String IX_HASBEENUSEDIN = "hasBeenUsedIn";
	public static final String IX_HASCHILD = "hasChild";
	public static final String IX_HASPARENT = "hasParent";
	public static final String IX_HASFIND = "hasFind";
	public static final String IX_ISFOUNDIN = "isFoundIn";
	public static final String IX_HASIMAGE = "hasImage";
	public static final String IX_HASOBJECTEXAMPLE = "hasObjectExample";
	public static final String IX_ISOBJECTEXAMPLEFOR = "isObjectExampleFor";
	public static final String IX_HASPART = "hasPart";
	public static final String IX_ISPARTOF = "isPartOf";
	public static final String IX_ISDESCRIBEDBY = "isDescribedBy";
	public static final String IX_DESCRIBES = "describes";
	public static final String IX_ISVISUALIZEDBY = "isVisualizedBy";
	public static final String IX_VISUALIZES = "visualizes";
	public static final String IX_CONTAINSINFORMATIONABOUT = "containsInformationAbout";
	public static final String IX_ISMENTIONEDBY = "isMentionedBy";
	public static final String IX_MENTIONS = "mentions";
	public static final String IX_REPLACES = "multipleReplaces";
	public static final String IX_ISREPLACEDBY = "isReplacedBy";

	// cidoc-crm
	public static final String IX_PARTICIPATEDIN = "participated_in";
	public static final String IX_HADPARTICIPANT = "had_participant";
	public static final String IX_WASPRESENTAT = "was_present_at";
	public static final String IX_OCCUREDINTHEPRESENCEOF = "occurred_in_the_presence_of";
	public static final String IX_HASFORMERORCURRENTKEEPER = "has_former_or_current_keeper";
	public static final String IX_ISFORMERORCURRENTKEEPEROF = "is_former_or_current_keeper_of";
	public static final String IX_HASFORMERORCURRENTOWNER = "has_former_or_current_owner";
	public static final String IX_ISFORMERORCURRENTOWNEROF = "is_former_or_current_owner_of";
	public static final String IX_HASCREATED = "has_created";
	public static final String IX_WASCREATEDBY = "was_created_by";
	public static final String IX_HASRIGHTON = "has_right_on";
	public static final String IX_RIGHTHELDBY = "right_held_by";
	public static final String IX_ISCURRENTORFORMERMEMBEROF = "is_current_or_former_member_of";
	public static final String IX_HASCURRENTORFORMERMEMBER = "has_current_or_former_member";

	// bio
	public static final String IX_CHILD = "child";
	public static final String IX_PARENT = "parent";
	public static final String IX_MOTHER = "mother";
	public static final String IX_FATHER = "father";

	// roller
	public static final String IX_CLIENT = "client";
	public static final String IX_COMPOSER = "composer";
	public static final String IX_AUTHOR = "author";
	public static final String IX_ARCHITECT = "architect";
	public static final String IX_INVENTOR = "inventor";
	public static final String IX_SCENOGRAPHER = "scenographer";
	public static final String IX_DESIGNER = "designer";
	public static final String IX_PRODUCER = "producer";
	public static final String IX_ORGANIZER = "organizer";
	public static final String IX_DIRECTOR = "director";
	public static final String IX_PHOTOGRAPHER = "photographer";
	public static final String IX_PAINTER = "painter";
	public static final String IX_BUILDER = "builder";
	public static final String IX_MASTERBUILDER = "masterBuilder";
	public static final String IX_CONSTRUCTIONCLIENT = "constructionClient";
	public static final String IX_ENGRAVER = "engraver";
	public static final String IX_MINTMASTER = "mintmaster";
	public static final String IX_ARTIST = "artist";
	public static final String IX_DESIGNENGINEER = "designEngineer";
	public static final String IX_CARPENTER = "carpenter";
	public static final String IX_MASON = "mason";
	public static final String IX_TECHNICIAN = "technician";
	public static final String IX_PUBLISHER = "publisher";
	public static final String IX_PUBLICIST = "publicist";
	public static final String IX_MUSICIAN = "musician";
	public static final String IX_ACTORACTRESS = "actorActress"; // ACTOR fanns redan så detta är en
																	// specialare...
	public static final String IX_PRINTER = "printer";
	public static final String IX_SIGNER = "signer";
	public static final String IX_FINDER = "finder";
	public static final String IX_ABANDONEE = "abandonee";
	public static final String IX_INTERMEDIARY = "intermediary";
	public static final String IX_BUYER = "buyer";
	public static final String IX_SELLER = "seller";
	public static final String IX_GENERALAGENT = "generalAgent";
	public static final String IX_DONOR = "donor";
	public static final String IX_DEPOSITOR = "depositor";
	public static final String IX_RESELLER = "reseller";
	public static final String IX_INVENTORYTAKER = "inventoryTaker";
	public static final String IX_EXCAVATOR = "excavator";
	public static final String IX_EXAMINATOR = "examinator";
	public static final String IX_CONSERVATOR = "conservator";
	public static final String IX_ARCHIVECONTRIBUTOR = "archiveContributor";
	public static final String IX_INTERVIEWER = "interviewer";
	public static final String IX_INFORMANT = "informant";
	public static final String IX_PATENTHOLDER = "patentHolder";
	public static final String IX_USER = "user";
	public static final String IX_SCANNEROPERATOR = "scannerOperator";
	public static final String IX_PICTUREEDITOR = "pictureEditor";
	public static final String IX_EMPLOYER = "employer";
	public static final String IX_MARRIEDTO = "marriedTo";

	// roller inverser
	public static final String IX_CLIENT_OF = "clientOf";
	public static final String IX_COMPOSER_OF = "composerOf";
	public static final String IX_AUTHOR_OF = "authorOf";
	public static final String IX_ARCHITECT_OF = "architectOf";
	public static final String IX_INVENTOR_OF = "inventorOf";
	public static final String IX_SCENOGRAPHER_OF = "scenographerOf";
	public static final String IX_DESIGNER_OF = "designerOf";
	public static final String IX_PRODUCER_OF = "producerOf";
	public static final String IX_ORGANIZER_OF = "organizerOf";
	public static final String IX_DIRECTOR_OF = "directorOf";
	public static final String IX_PHOTOGRAPHER_OF = "photographerOf";
	public static final String IX_PAINTER_OF = "painterOf";
	public static final String IX_BUILDER_OF = "builderOf";
	public static final String IX_MASTERBUILDER_OF = "masterBuilderOf";
	public static final String IX_CONSTRUCTIONCLIENT_OF = "constructionClientOf";
	public static final String IX_ENGRAVER_OF = "engraverOf";
	public static final String IX_MINTMASTER_OF = "mintmasterOf";
	public static final String IX_ARTIST_OF = "artistOf";
	public static final String IX_DESIGNENGINEER_OF = "designEngineerOf";
	public static final String IX_CARPENTER_OF = "carpenterOf";
	public static final String IX_MASON_OF = "masonOf";
	public static final String IX_TECHNICIAN_OF = "technicianOf";
	public static final String IX_PUBLISHER_OF = "publisherOf";
	public static final String IX_PUBLICIST_OF = "publicistOf";
	public static final String IX_MUSICIAN_OF = "musicianOf";
	public static final String IX_ACTORACTRESS_OF = "actorActressOf";
	public static final String IX_PRINTER_OF = "printerOf";
	public static final String IX_SIGNER_OF = "signerOf";
	public static final String IX_FINDER_OF = "finderOf";
	public static final String IX_ABANDONEE_OF = "abandoneeOf";
	public static final String IX_INTERMEDIARY_OF = "intermediaryOf";
	public static final String IX_BUYER_OF = "buyerOf";
	public static final String IX_SELLER_OF = "sellerOf";
	public static final String IX_GENERALAGENT_OF = "generalAgentOf";
	public static final String IX_DONOR_OF = "donorOf";
	public static final String IX_DEPOSITOR_OF = "depositorOf";
	public static final String IX_RESELLER_OF = "resellerOf";
	public static final String IX_INVENTORYTAKER_OF = "inventoryTakerOf";
	public static final String IX_EXCAVATOR_OF = "excavatorOf";
	public static final String IX_EXAMINATOR_OF = "examinatorOf";
	public static final String IX_CONSERVATOR_OF = "conservatorOf";
	public static final String IX_ARCHIVECONTRIBUTOR_OF = "archiveContributorOf";
	public static final String IX_INTERVIEWER_OF = "interviewerOf";
	public static final String IX_INFORMANT_OF = "informantOf";
	public static final String IX_PATENTHOLDER_OF = "patentHolderOf";
	public static final String IX_USER_OF = "userOf";
	public static final String IX_SCANNEROPERATOR_OF = "scannerOperatorOf";
	public static final String IX_PICTUREEDITOR_OF = "pictureEditorOf";
	public static final String IX_EMPLOYER_OF = "employerOf";

	// media-index
	public static final String IX_MEDIALICENSE = "mediaLicense";
	public static final String IX_MEDIAMOTIVEWORD = "mediaMotiveWord";
	public static final String IX_BYLINE = "byline";
	public static final String IX_COPYRIGHT = "copyright";
	public static final String IX_THUMBNAIL_SOURCE = "thumbnailSource";
	public static final String IX_LOWRES_SOURCE = "lowresSource";
	public static final String IX_HIGHRES_SOURCE = "highresSource";

	// spatiala specialindex
	public static final String IX_BOUNDING_BOX = "boundingBox";
	public static final String IX_POINT_DISTANCE = "pointDistance";

	// spatiala koordinatkonstanter
	public static final String SWEREF99_3006 = "SWEREF99";
	public static final String RT90_3021 = "RT90";
	public static final String WGS84_4326 = "WGS84";

	// övriga specialindex
	public static final String IX_THUMBNAILEXISTS = "thumbnailExists";
	public static final String IX_GEODATAEXISTS = "geoDataExists";
	public static final String IX_TIMEINFOEXISTS = "timeInfoExists";
	public static final String IX_CENTURY = "century";
	public static final String IX_DECADE = "decade";


	// alla index
	private static final HashMap<String, Index> indices = new LinkedHashMap<>();
	// publika index
	private static final List<Index> publicIndices = new ArrayList<>();

	// meddelanden om eventuella problem vid tolkning av tjänsteinnehållet, tex att en konstant
	// inte kunde slås upp etc, och antal ggr problemet förekom - främst för utv/debug
	private static final ThreadLocal<Map<String, Integer>> problemMessages = new ThreadLocal<>();

	static {
		// implementerade index

		// de visas per default i denna ordning i SRU-explain via publicIndices
		addIndex(IX_ITEMID, "Identifierare", IndexType.VERBATIM);
		addIndex(IX_SERVICENAME, "Ursprungstjänst", IndexType.TOLOWERCASE);
		addIndex(IX_SERVICEORGANISATION, "Ursprungsorganisation", IndexType.TOLOWERCASE);
		addIndex(IX_TEXT, "Fritext, generellt", IndexType.ANALYZED, true, false);
		addIndex(IX_STRICT, "Fritext, generellt - ostammat", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_SUBJECT, "Ämnesavgränsning", IndexType.TOLOWERCASE, true, false); // uri
																						// (översatt)
		addIndex(IX_COLLECTION, "Namn på samlingen som objektet tillhör", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_DATAQUALITY, "Beskrivningsnivå", IndexType.TOLOWERCASE); // uri (översatt)
		addIndex(IX_MEDIATYPE, "Avgränsning av mediatyper", IndexType.TOLOWERCASE, true, false); // uri

		addIndex(IX_REPLACES, "Identifierare för objekt som detta objekt ersätter", IndexType.VERBATIM);


		addIndex(IX_PROTOCOLVERSION, "Protokollversion för posten", IndexType.VERBATIM, true, false); // string

		// objekt
		addIndex(IX_ITEM, "Fritext i entitetsfält", IndexType.ANALYZED);
		addIndex(IX_ITEMTYPE, "Typ av objekt", IndexType.TOLOWERCASE); // uri (översatt)
		addIndex(IX_ITEMSUPERTYPE, "Huvudtyp av objekt", IndexType.TOLOWERCASE); // uri (översatt)
		addIndex(IX_ITEMCLASS, "Objektets klass, kategorisering eller klassifikation", IndexType.TOLOWERCASE, true,
			false); // uri
		addIndex(IX_ITEMCLASSNAME, "Klass eller kategori, om det inte finns en klassificeringsresurs",
			IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMNAME, "Objektets huvudsakliga benämning eller sakord", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMSPECIFICATION, "Modellbeteckning eller liknande", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMTITLE, "Titel eller verksnamn", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMLABEL, "Huvudsaklig beskrivning av objektet - klassifikation, sakord el dyl",
			IndexType.TOLOWERCASE);
		addIndex(IX_ITEMMARK, "Fritext i item mark", IndexType.ANALYZED, true, false);
		addIndex(IX_ITEMINSCRIPTION, "Fritext i item inscription", IndexType.ANALYZED, true, false);
		addIndex(IX_ITEMDESCRIPTION, "Fritext i beskrivningsfält", IndexType.ANALYZED, true, false);
		addIndex(IX_ITEMKEYWORD, "Nyckelord", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMMOTIVEWORD, "Ord som förekommer som beskrivning av ett motiv i ett bild- eller målningsobjekt",
			IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMMATERIAL, "Material som objektet består av", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMTECHNIQUE, "Teknik för att producera objektet", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMSTYLE, "Stil som präglar objektet", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMCOLOR, "Färg som präglar objektet", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ITEMNUMBER, "Inventarienummer eller annan identifikation av objektet", IndexType.TOLOWERCASE, true,
			false);
		addIndex(IX_ITEMLICENSE, "Licens för användning av objektet (uri)", IndexType.TOLOWERCASE); // typ?
																									// uri
																									// ->
																									// verbatim,
																									// kod
																									// ->
																									// lowercase
		addIndex(IX_CREATEDDATE, "Datum då posten skapades i källsystemet (yyyy-mm-dd)", IndexType.VERBATIM);
		addIndex(IX_LASTCHANGEDDATE, "Datum då posten ändrades i källsystemet (yyyy-mm-dd)", IndexType.VERBATIM);
		addIndex(IX_THEME, "Tema", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_BUILD_DATE, "Datum då posten publicerades", IndexType.VERBATIM);

		// plats
		addIndex(IX_PLACENAME, "Annat platsnamn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_CADASTRALUNIT, "Fastighetsbeteckning [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PLACETERMID, "Plats-ID hos auktoritet [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PLACETERMAUTH, "Auktoritet för plats-ID [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PLACETERM, "Plats-ID (uri)", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_CONTINENTNAME, "Kontinent [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_COUNTRYNAME, "Land, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_COUNTYNAME, "Län, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_MUNICIPALITYNAME, "Kommun, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PROVINCENAME, "Landskap, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PARISHNAME, "Socken, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_COUNTRY, "Land, kod [*]", IndexType.TOLOWERCASE, true, false); // uri (översatt)
		addIndex(IX_COUNTY, "Län, kod [*]", IndexType.TOLOWERCASE, true, false); // uri (översatt)
		addIndex(IX_MUNICIPALITY, "Kommun, kod [*]", IndexType.TOLOWERCASE, true, false); // uri
		// (översatt)
		addIndex(IX_PROVINCE, "Landskap, kod [*]", IndexType.TOLOWERCASE, true, false); // uri
																						// (översatt)
		addIndex(IX_PARISH, "Socken, kod [*]", IndexType.TOLOWERCASE, true, false); // uri
																					// (översatt)
		addIndex(IX_PLACE, "Var - Fritext i geografiska data", IndexType.ANALYZED, true, false);

		// spatiala virtuella index
		String coordExplain = "koordinater separerade med mellanslag i (nästan) valfritt format " +
			"((EPSG:3006 (SWEREF99 TM) är default, OBS att x,y förutsätts! Giltiga värden förutom " + "EPSG:XXXX är '" +
			SWEREF99_3006 + "' (EPSG:3006 - SWEREF99 TM), " + "'" + RT90_3021 + "' (EPSG:3021 - RT90 2.5 gon V) och " +
			"'" + WGS84_4326 + "' (EPSG:4326))";
		addIndex(IX_BOUNDING_BOX,
			"Spatial sökning med omslutande rektangel, " + coordExplain + " - ex " + IX_BOUNDING_BOX +
				"=/EPSG:3021 \"1628000.0 6585000.0 1628490.368 6585865.547\" eller " + IX_BOUNDING_BOX + "=/" +
				RT90_3021 + "\"1628000.0 6585000.0 1628490.368 6585865.547\"",
			IndexType.SPATIAL_VIRTUAL, true, false);
		addIndex(IX_POINT_DISTANCE,
			"Spatial närhetssökning med angiven punkt och radie, " + coordExplain + " och radien i km - ex " +
				IX_POINT_DISTANCE + "=/EPSG:3021 \"1628000.0 6585000.0 3.5\" eller " + IX_POINT_DISTANCE + "=/" +
				RT90_3021 + " \"1628000.0 6585000.0 3.5\"",
			IndexType.SPATIAL_VIRTUAL, true, false);

		// person
		addIndex(IX_FIRSTNAME, "Förnamn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_SURNAME, "Efternamn [*]", IndexType.TOLOWERCASE, true, false);
		// addIndex(IX_NAME, "Namn", IndexType.TOLOWERCASE); // ej index, bara i med fritext
		addIndex(IX_GENDER, "Kön [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_FULLNAME, "Fullständigt namn ([förnamn] [efternamn]) [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ORGANIZATION, "Organisation [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_TITLE, "Titel (person) [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_NAMEID, "Auktoriserat ID [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_NAMEAUTH, "Auktoritet för namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_ACTOR, "Vem - Fritext i person- och organisationsdata", IndexType.ANALYZED, true, false);
		addIndex(IX_AGENT, "Agent, uri", IndexType.TOLOWERCASE, true, false);

		// tid
		addIndex(IX_FROMTIME, "Tidpunkt eller start på tidsintervall (årtal enligt ISO 8601) [*]",
			IndexType.ISO8601DATEYEAR, true, false);
		addIndex(IX_TOTIME, "Tidpunkt eller slut på tidsintervall (årtal enligt ISO 8601) [*]",
			IndexType.ISO8601DATEYEAR, true, false);
		addIndex(IX_FROMPERIODNAME, "Tidpunkt eller start på tidsintervall, namn [*]", IndexType.TOLOWERCASE, true,
			false);
		addIndex(IX_TOPERIODNAME, "Tidpunkt eller slut på tidsintervall, namn [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_FROMPERIODID, "Tidpunkt eller start på tidsintervall, kod [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_TOPERIODID, "Tidpunkt eller slut på tidsintervall, kod [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_FROMPERIOD, "Tidpunkt eller start på tidsintervall, uri", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_TOPERIOD, "Tidpunkt eller slut på tidsintervall, uri", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_PERIODAUTH, "Auktoritet för perioder [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_EVENTNAME, "Namn på en händelse [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_EVENTAUTH, "Auktoritet för händelser [*]", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_EVENT, "Händelse, uri", IndexType.TOLOWERCASE, true, false);
		// addIndex(IX_TIMETEXT, "Annan tidsuppgift [*]", IndexType.ANALYZED);
		addIndex(IX_TIME, "När - Fritext i tidsdata", IndexType.ANALYZED, true, false);

		// context
		addIndex(IX_CONTEXTLABEL, "Sammanhang enligt ändlig lista, beskrivning", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_CONTEXTTYPE, "Sammanhang enligt ändlig lista, nyckelvärde", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_CONTEXTSUPERTYPE, "Huvudsammanhang enligt ändlig lista, nyckelvärde", IndexType.TOLOWERCASE, true,
			false);

		// relationer TODO: fixa beskrivningstexterna när Börje har skickat dem
		addIndex(IX_RELURI, "Är relaterat på något sätt till annat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_CONTAINSINFORMATIONABOUT, "Har information om (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_CONTAINSOBJECT, "Innehåller objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISCONTAINEDIN, "Innehålls i objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASBEENUSEDIN, "Används i (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASCHILD, "Har underordnat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASFIND, "Har fynd (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASIMAGE, "Har bild (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASOBJECTEXAMPLE, "Har objektexempel (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISOBJECTEXAMPLEFOR, "Är objektexempel för (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASPARENT, "Har överordnat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HASPART, "Har del (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISDESCRIBEDBY, "Beskrivs av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DESCRIBES, "Beskriver (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISFOUNDIN, "Finns i (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISPARTOF, "Är del av annat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISRELATEDTO, "Är relaterat till annat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISVISUALIZEDBY, "Visualiseras av annat objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_SAMEAS, "Samma som (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_VISUALIZES, "Visualiserar objekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISMENTIONEDBY, "Nämns av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MENTIONS, "Nämner (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ADDEDTOINDEXDATE,
			"Datum posten lades till i indexet (yyyy-mm-dd) - " +
				"obs att detta datum är ungefärligt då det beror på skördefrekvens för " +
				"källtjänsten, beräknas som max(källtjänstens första indexeringsdatum, " + IX_CREATEDDATE + ")",
			IndexType.VERBATIM);

		// cidoc-crm
		addIndex(IX_HASFORMERORCURRENTOWNER, "Har nuvarande eller tidigare ägare (uri)", IndexType.VERBATIM, true,
			false);
		addIndex(IX_HASFORMERORCURRENTKEEPER, "Har nuvarande eller tidigare förvaltare (uri)", IndexType.VERBATIM, true,
			false);
		addIndex(IX_WASCREATEDBY, "Skapades av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_RIGHTHELDBY, "Rättigheter ägs av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_WASPRESENTAT, "Var närvarande vid händelse (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_OCCUREDINTHEPRESENCEOF, "Händelsen skedde i närvaro av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_HADPARTICIPANT, "Händelsen hade deltagare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PARTICIPATEDIN, "Deltog i händelse (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ISCURRENTORFORMERMEMBEROF, "Är eller var tidigare medlem av (uri)", IndexType.VERBATIM, true,
			false);
		addIndex(IX_HASCURRENTORFORMERMEMBER, "Har eller hade medlem (uri)", IndexType.VERBATIM, true, false);

		// bio
		addIndex(IX_CHILD, "Var förälder till (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PARENT, "Var barn till (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MOTHER, "Har mor (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_FATHER, "Har far (uri)", IndexType.VERBATIM, true, false);

		// roller
		addIndex(IX_CLIENT, "Beställare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_COMPOSER, "Kompositör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_AUTHOR, "Författare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ARCHITECT, "Arkitekt (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_INVENTOR, "Uppfinnare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_SCENOGRAPHER, "Scenograf (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DESIGNER, "Designer (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PRODUCER, "Producent (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ORGANIZER, "Arrangör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DIRECTOR, "Regissör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PHOTOGRAPHER, "Fotograf (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PAINTER, "Målare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_BUILDER, "Byggare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MASTERBUILDER, "Byggmästare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_CONSTRUCTIONCLIENT, "Byggherre (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ENGRAVER, "Gravör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MINTMASTER, "Myntmästare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ARTIST, "Konstnär (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DESIGNENGINEER, "Konstruktör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_CARPENTER, "Snickare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MASON, "Murare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_TECHNICIAN, "Tekniker (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PUBLISHER, "Förläggare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PUBLICIST, "Publicist (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MUSICIAN, "Musiker (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ACTORACTRESS, "Skådespelare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PRINTER, "Tryckare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_SIGNER, "Påskrift av (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_FINDER, "Upphittare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ABANDONEE, "Förvärvare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_INTERMEDIARY, "Förmedlare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_BUYER, "Köpare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_SELLER, "Säljare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_GENERALAGENT, "Generalagent (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DONOR, "Givare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_DEPOSITOR, "Deponent (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_RESELLER, "Återförsäljare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_INVENTORYTAKER, "Inventerare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_EXCAVATOR, "Grävare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_EXAMINATOR, "Undersökare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_CONSERVATOR, "Konservator (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_ARCHIVECONTRIBUTOR, "Arkivbildare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_INTERVIEWER, "Intervjuare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_INFORMANT, "Informant (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PATENTHOLDER, "Patentinnehavare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_USER, "Brukare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_SCANNEROPERATOR, "Skanneroperatör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_PICTUREEDITOR, "Bildredaktör (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_EMPLOYER, "Arbets- eller uppdragsgivare (uri)", IndexType.VERBATIM, true, false);
		addIndex(IX_MARRIEDTO, "Gift med (uri)", IndexType.VERBATIM, true, false);


		// media
		addIndex(IX_MEDIALICENSE, "Licens för ingående bild/media(uri)", IndexType.TOLOWERCASE, true, false);
		addIndex(IX_MEDIAMOTIVEWORD, "Bildmotiv för ingående bilder/media", IndexType.ANALYZED, true, false);
		addIndex(IX_BYLINE, "Byline för ingående bild/media(uri)", IndexType.TOLOWERCASE);
		addIndex(IX_COPYRIGHT, "Copyright för ingående bild/media(uri)", IndexType.TOLOWERCASE);
		addIndex(IX_THUMBNAIL_SOURCE, "Källa för tumnagel", IndexType.VERBATIM, true, false);
		addIndex(IX_LOWRES_SOURCE, "Källa för lågupplöst bild", IndexType.VERBATIM, true, false);
		addIndex(IX_HIGHRES_SOURCE, "Källa för högupplöst bild", IndexType.VERBATIM, true, false);

		// övriga
		addIndex(IX_THUMBNAILEXISTS, "Om objektet har en tumnagelbild (j/n)", IndexType.TOLOWERCASE);
		addIndex(IX_GEODATAEXISTS, "Om objektet har spatial data (j/n)", IndexType.TOLOWERCASE);
		addIndex(IX_TIMEINFOEXISTS, "Om objektet har tidsangivelse (i " + IX_FROMTIME + "/" + IX_TOTIME + ") (j/n)",
			IndexType.TOLOWERCASE);
		addIndex(IX_CENTURY, "De århundraden som objektet omfattar", IndexType.ISO8601DATEYEAR, true, false);
		addIndex(IX_DECADE, "De årtionden som objektet omfattar", IndexType.ISO8601DATEYEAR, true, false);

		addIndex(IX_THUMBNAIL, "url till tumnagel", IndexType.VERBATIM, true, false);

		// övriga, "interna"
		addIndex(I_IX_PRES, "presentationsblocket", IndexType.VERBATIM, false, false);
		addIndex(I_IX_RDF, "rdf", IndexType.VERBATIM, false, false);
		addIndex(I_IX_SERVICE, "tjänst", IndexType.VERBATIM, false, false);
		addIndex(I_IX_HTML_URL, "html-representation, url", IndexType.VERBATIM, false, false);
		addIndex(I_IX_MUSEUMDAT_URL, "museumdat-representation, url", IndexType.VERBATIM, false, false);
		addIndex(I_IX_LON, "longitud för centrumpunkt", IndexType.SPATIAL_COORDINATE, false, false);
		addIndex(I_IX_LAT, "latitud för centrumpunkt", IndexType.SPATIAL_COORDINATE, false, false);
		addIndex(I_IX_RELATIONS, "relationer", IndexType.VERBATIM, false, false);
		addIndex(CONTEXT_SET_REC + "." + IX_REC_IDENTIFIER, "identifierare", IndexType.VERBATIM, false, false);
		// addIndex(I_IX_RDF, "rdf", IndexType.VERBATIM, false);



		// publika
		for (Index index : indices.values()) {
			if (index.isPublic()) {
				publicIndices.add(index);
			}
		}
	}

	@SuppressWarnings("unused")
	private static final Logger logger = LogManager.getLogger(ContentHelper.class);

	public ContentHelper() {}

	// instansmetoder som måste implementeras i subklasser

	/**
	 * Extraherar identifierare mm ur xml-innehåll. För k-samsökstjänster ska identifieraren vara en
	 * URI och xml-innehållet är en post med k-samsöks-xml (rdf).
	 * 
	 * @param xmlContent xml-innehåll
	 * @return värdeböna, aldrig null
	 * @throws Exception vid problem
	 */
	public abstract ExtractedInfo extractInfo(String xmlContent) throws Exception;

	/**
	 * Skapar ett solr-dokument utifrån det inskickade xml-innehållet. För k-samsökstjänster är
	 * xml-innehållet en post med k-samsöks-xml (rdf). Om metoden ger null har tjänsten begärt att
	 * posten bara ska lagras och inte indexeras.
	 *
	 * @param service tjänst
	 * @param xmlContent xml-innehåll
	 * @param added datum posten först lades till i repot
	 * @return ett solr-dokument, eller null om inte posten ska indexeras
	 */
	public abstract SolrInputDocument createSolrDocument(HarvestService service, String xmlContent, Date added);

	// statiska metoder

	/**
	 * Formaterar ett datum i svenskt standardformat med eller utan tid.
	 * 
	 * @param date datum
	 * @param inclTime om tid ska med
	 * @return formaterad sträng
	 */
	public static String formatDate(Date date, boolean inclTime) {
		final String format;
		if (inclTime) {
			format = "yyyy-MM-dd HH:mm:ss";
		} else {
			format = "yyyy-MM-dd";
		}
		SimpleDateFormat df = new SimpleDateFormat(format);
		return df.format(date);
	}

	// hämtar ut ett konfat index, eller dess "pappa" för prefixade index
	// bör bara användas för att fastställa vilken typ av index det är
	private static IndexType getIndexType(String indexName) {
		int underScorePos;
		if (indexName != null && (underScorePos = indexName.indexOf("_")) > 0) {
			indexName = indexName.substring(underScorePos + 1);
		}
		Index index = indices.get(indexName);
		return index != null ? index.getIndexType() : null;
	}

	/**
	 * Ger sant om indexnamnet är ett analyserat index.
	 * 
	 * @param indexName indexnamn
	 * @return sant för analyserade indexnamn
	 */
	public static boolean isAnalyzedIndex(String indexName) {
		// analyserade index, dvs fritext
		return IndexType.ANALYZED == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet är ett verbatim-index, dvs om värdet ej ändras utan lagras exakt och
	 * måste sökas efter exakt.
	 * 
	 * @param indexName indexnamn
	 * @return sant för index som är verbatim
	 */
	public static boolean isVerbatimIndex(String indexName) {
		// de index som indexerats verbatim, typ identifierare, uris etc
		return IndexType.VERBATIM == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet är ett gemener-index, dvs om värdet görs om till gemener vid
	 * indexering och sökning.
	 * 
	 * @param indexName indexnamn
	 * @return sant för index vars innehåll görs om till gemener
	 */
	public static boolean isToLowerCaseIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.TOLOWERCASE == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet är ett spatialt virtuellt index. Denna typ är ett specialfall som
	 * hanteras olika beroende på indexnamn då de kan ha olika utseende på sina parametrar skapa
	 * frågor som söker i andra index etc.
	 * 
	 * @param indexName indexnamn
	 * @return sant för spatiala index
	 */
	public static boolean isSpatialVirtualIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.SPATIAL_VIRTUAL == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet är ett spatialt index vars värden är rena koordinater.
	 * 
	 * @param indexName indexnamn
	 * @return sant för spatiala index
	 */
	public static boolean isSpatialCoordinateIndex(String indexName) {
		// de index som indexerats med lower case
		return IndexType.SPATIAL_COORDINATE == getIndexType(indexName);
	}

	/**
	 * Ger sant om indexnamnet är ett iso8601-datum-index. Denna typ är ett specialfall då invärdet
	 * i från lokalnoderna kan vara hela datum men då enbart årtalet ska indexeras. Dvs görs värdet
	 * om till årtal och behandlas vid indexering med algoritm från solr:s NumberUtils så att lucene
	 * kan göra intervallsökningar även med negativa värden. Vid sökning appliceras samma algoritm
	 * på sökvärdet som då förutsätts vara ett årtal.
	 * 
	 * @param indexName indexnamn
	 * @return sant för index vars innehåll ska vara årtal och som görs om till strängvärden
	 */
	public static boolean isISO8601DateYearIndex(String indexName) {
		// de index som indexerats verbatim, typ identifierare, uris etc
		return IndexType.ISO8601DATEYEAR == getIndexType(indexName);
	}

	// lägger till index i index-listan, används bara vid init
	private static void addIndex(String indexName, String title, IndexType indexType) {
		addIndex(indexName, title, indexType, true, true);
	}

	private static void addIndex(String indexName, String title, IndexType indexType, boolean isPublic,
		boolean isSortable) {
		indices.put(indexName, new Index(indexName, title, indexType, isPublic, isSortable));
	}

	/**
	 * Kollar om inskickat index existerar. Om indexet är ett "kontext-index", dvs om det är på
	 * formen "[kontexttyp]_[indexnamn] kontrolleras endast att indexnamn är ok då dessa index är
	 * dynamiska och inget register finns för att kontrollera dessa.
	 * 
	 * @param indexName index att kontrollera
	 * @return sant om indexet finns eller om indexet är ett kontext-index och dess suffix är ett
	 *         giltigt index
	 */
	public static boolean indexExists(String indexName) {
		if (indexName == null) {
			return false;
		}
		// testa hela namnet först
		if (indices.containsKey(indexName)) {
			return true;
		}
		// sen om det (troligen) är ett kontextindex, "[contextType]_[indexName]"
		if (indexName.indexOf("_") > 0) {
			String[] parts = indexName.split("_");
			if (parts.length == 2) {
				return indices.containsKey(parts[1]);
			}
		}
		return false;
	}

	/**
	 * Kontrollerar om ett index kan användas för sortering (flervärda index, där en post kan ha
	 * flera värden, går inte att sortera på)
	 * 
	 * @param indexName index att kontrollera
	 * @return true om indexet kan användas för sortering, annars false
	 */
	public static boolean indexSortable(String indexName) {
		Index index = getIndex(indexName);
		return index != null && index.isSortable();
	}

	/**
	 * Ger index-instans för givet indexnamn. OBS! hanterar ej "kontext-index".
	 * 
	 * @param indexName indexnamn
	 * @return index-instans, eller null
	 */
	public static Index getIndex(String indexName) {
		return indices.get(indexName);
	}

	/**
	 * Hämtar lista med alla publika index.
	 * 
	 * @return lista med publikt sökbara index.
	 */
	public static List<Index> getPublicIndices() {
		return publicIndices;
	}

	/**
	 * Enum för indextyp.
	 */
	public enum IndexType {
		ANALYZED, VERBATIM, TOLOWERCASE, ISO8601DATEYEAR, SPATIAL_VIRTUAL, SPATIAL_COORDINATE
	}

	/**
	 * Klass som representerar ett index.
	 */
	public static final class Index {

		private final String index;
		private final String title;
		private final IndexType indexType;
		private final boolean isPublic;
		private final boolean sortable;

		Index(String index, String title, IndexType indexType) {
			this(index, title, indexType, true, true);
		}

		Index(String index, String title, IndexType indexType, boolean isPublic) {
			this(index, title, indexType, isPublic, true);
		}

		Index(String index, String title, IndexType indexType, boolean isPublic, boolean sortable) {
			this.index = index;
			this.title = title;
			this.indexType = indexType;
			this.isPublic = isPublic;
			this.sortable = sortable;
		}

		/**
		 * Hämtar indexnamnet.
		 * 
		 * @return indexnamn
		 */
		public String getIndex() {
			return index;
		}

		/**
		 * Hämtar beskrivande text.
		 * 
		 * @return beskrivande text
		 */
		public String getTitle() {
			return title;
		}

		/**
		 * Hämtar indextyp.
		 * 
		 * @return indextyp
		 */
		public IndexType getIndexType() {
			return indexType;
		}

		/**
		 * Ger sant om indexet är publikt.
		 * 
		 * @return sant för publika index
		 */
		public boolean isPublic() {
			return isPublic;
		}

		/**
		 * Sant om indexet kan användas för sortering
		 * 
		 * @return sant för sorteringsgrundande index
		 */
		public boolean isSortable() {
			return sortable;
		}

	}

	/**
	 * Initierar mappen med problemmeddelanden för denna tråd.
	 */
	public static void initProblemMessages() {
		// linked hashmap för att behålla ordningen
		problemMessages.set(new LinkedHashMap<>());
	}

	/**
	 * Hämtar och rensar mappen med problemmeddelanden för denna tråd.
	 * 
	 * @return problemmeddelanden eller null
	 */
	public static Map<String, Integer> getAndClearProblemMessages() {
		Map<String, Integer> map = problemMessages.get();
		problemMessages.remove();
		return map;
	}

	/**
	 * Lägger till ett problemmeddelande för denna tråd/detta jobb.
	 * 
	 * @param message meddelande
	 */
	public static void addProblemMessage(String message) {
		final int maxSize = 200;
		final String xMessage = "There are more error messages, just listing max " + maxSize + " different";
		Map<String, Integer> map = problemMessages.get();
		if (map != null) {
			Integer c = map.get(message);
			if (c == null) {
				c = 0;
				if (map.size() >= maxSize && !xMessage.equals(message)) {
					addProblemMessage(xMessage);
					return;
				}
			}
			map.put(message, c + 1);
		}
	}

	/**
	 * Formaterar körtid/deltatid på formatet hh:mm:ss.
	 * 
	 * @param durationMillis körtid i millisekunder
	 * @return sträng med körtid
	 */
	public static String formatRunTime(long durationMillis) {
		return DurationFormatUtils.formatDuration(durationMillis, "HH:mm:ss");
	}

	/**
	 * Beräknar ungefärlig återstående tid utifrån hur lång tid det hittills har tagit för en
	 * delmängd. Beräkningen antar att förloppet sker med konstant hastighet.
	 * 
	 * @param deltaMillis tid det tog för delmängd att bli klar
	 * @param deltaCount antal i delmängd
	 * @param fullCount antal totalt
	 * @return ungefärlig återstående tid i millisekunder
	 */
	public static long getRemainingRunTimeMillis(long deltaMillis, int deltaCount, int fullCount) {
		long remainingTimeMillis = -1;
		if (fullCount > 0 && deltaCount > 0) {
			long aproxRunTimeMillis = Math.round(fullCount * deltaMillis * 1.0 / deltaCount);
			remainingTimeMillis = aproxRunTimeMillis - deltaMillis;
		}
		return remainingTimeMillis;
	}

	/**
	 * Formaterar antal per sekund.
	 * 
	 * @param count antal
	 * @param durationMillis körtid i millisekunder
	 * @return sträng med antal per sekund med en nogrannhet på en decimal
	 */
	public static String formatSpeedPerSec(long count, long durationMillis) {
		double perSec = getSpeedPerSec(count, durationMillis);
		return "ca " + perSec + "/s";
	}

	/**
	 * Beräknar antal per sekund.
	 * 
	 * @param count antal
	 * @param durationMillis körtid i millisekunder
	 * @return antal per sekund med 1 decimal
	 */
	public static double getSpeedPerSec(long count, long durationMillis) {
		return Math.round(10.0 * count / Math.max(durationMillis / 1000, 1)) / 10.0;
	}

	//

	/**
	 * Hjälpmetod som extraherar parametrar kodade mha utf-8 från query-strängen, krävs bla för
	 * sru/cql.
	 * 
	 * @param qs querysträng
	 * @return map med avkodade parametrar och värden
	 */
	public static Map<String, String> extractUTF8Params(String qs) throws UnsupportedEncodingException {
		HashMap<String, String> params = new HashMap<>();
		if (qs != null && qs.length() > 0) {
			StringTokenizer tok = new StringTokenizer(qs, "&");
			while (tok.hasMoreTokens()) {
				String[] par = tok.nextToken().split("=");
				if (par.length > 1 && par[1].length() > 0) {
					if (par.length == 2) {
						params.put(par[0], URLDecoder.decode(par[1], StandardCharsets.UTF_8));
					} else {
						// vi är snälla och tillåter = okodat i parametrar för att enklare
						// kunna testa
						StringBuilder pVal = new StringBuilder();
						pVal.append(par[1]);
						for (int i = 2; i < par.length; ++i) {
							pVal.append("=").append(par[i]);
						}
						params.put(par[0], URLDecoder.decode(pVal.toString(), StandardCharsets.UTF_8));
					}
				}
			}
		}
		return params;
	}
}
