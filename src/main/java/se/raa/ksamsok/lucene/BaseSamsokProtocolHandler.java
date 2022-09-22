package se.raa.ksamsok.lucene;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import se.raa.ksamsok.harvest.HarvestService;
import se.raa.ksamsok.harvest.OAIPMHHandler;
import se.raa.ksamsok.lucene.exception.SamsokProtocolException;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static se.raa.ksamsok.lucene.ContentHelper.IX_ADDEDTOINDEXDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_AGENT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_BUILD_DATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CADASTRALUNIT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COLLECTION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTEXTTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CONTINENTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTRY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTRYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_COUNTYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_CREATEDDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_DATAQUALITY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FIRSTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIOD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMTIME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FULLNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GENDER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GEODATAEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_HIGHRES_SOURCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCLASS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCLASSNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMCOLOR;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMDESCRIPTION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMKEYWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMLABEL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMLICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMMATERIAL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMNUMBER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMSPECIFICATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMSTYLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTECHNIQUE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTITLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ITEMTYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_LASTCHANGEDDATE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_LOWRES_SOURCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIATYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ORGANIZATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISHNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACETERM;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_RELURI;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICEORGANISATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SUBJECT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SURNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THEME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THUMBNAIL;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THUMBNAILEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THUMBNAIL_SOURCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TIMEINFOEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TITLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIOD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOTIME;
import static se.raa.ksamsok.lucene.ContentHelper.addProblemMessage;
import static se.raa.ksamsok.lucene.ContentHelper.formatDate;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.context_pre;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rBuilddDate;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCadastralUnit;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCollection;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContext;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContextType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rContinentName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCoordinates;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountry;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountryName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCounty;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCountyName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rCreatedDate;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rDataQuality;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rEventAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rEventName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFirstName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromPeriodId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromPeriodName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFromTime;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rFullName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rGender;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rHighresSource;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rImage;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemClass;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemClassName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemColor;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemDescription;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemKeyWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemLabel;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemMaterial;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemMotiveWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemNumber;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemSpecification;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemStyle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemTechnique;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemTitle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rItemType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rLastChangedDate;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rLowresSource;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMaterial;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaLicense;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaMotiveWord;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMediaType;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMunicipality;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rMunicipalityName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNameAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNameId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rNumber;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rOrganization;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rParish;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rParishName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPeriodAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceTermAuth;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rPlaceTermId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rProvince;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rProvinceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rServiceName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rServiceOrganization;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSubject;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rSurname;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rTheme;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rThumbnail;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rThumbnailSource;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rTitle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToTime;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Desc;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Name;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Spec;

public abstract class BaseSamsokProtocolHandler implements SamsokProtocolHandler, RelationToIndexMapper {

	protected final Logger logger;

	protected final Model model;
	protected final Resource subject;
	protected final IndexProcessor ip;
	protected final SolrInputDocument luceneDoc;

	/**
	 * Map som håller ev återkommande uri:er och uri-referenser för att slippa
	 * skapa dem flera gånger eller ha variabler
	 */
	protected Map<URI, Property> mapper = new HashMap<>();

	protected boolean timeInfoExists = false;
	protected boolean geoDataExists = false;

	boolean requireMediaLicense = true;

	protected BaseSamsokProtocolHandler(Model model, Resource subject) {
		logger = getLogger();
		this.model = model;
		this.subject = subject;
		this.luceneDoc = new SolrInputDocument();
		this.ip = new IndexProcessor(luceneDoc, getURIValues(), this);
	}


	/**
	 * Ger map med värden nycklade på uri.
	 * 
	 * @return map men uri/värde-par
	 */
	protected abstract Map<String,String> getURIValues();

	@Override
	public String lookupURIValue(String uri) {
		return getURIValues().get(uri);
	}

	/**
	 * Skapar en URIReference för aktuell graf och cachar den.
	 * @param uri uri
	 * @return en URIReference
	 */
	protected Property getURIRef(URI uri){
		Property ref = mapper.get(uri);
		if (ref == null) {
			ref = ResourceFactory.createProperty(uri.toString());
			mapper.put(uri, ref);
		}
		return ref;
	}

	@Override
	public SolrInputDocument handle(HarvestService service, Date added,
			List<String> relations, List<String> gmlGeometries)
			throws Exception {

		String identifier = subject.toString();

		extractServiceInformation();

		// ta hand om "system"-datum
		extractAndHandleIndexDates(identifier, service, added);

		// item
		extractItemInformation();

		// klassificeringar
		extractClassificationInformation();

		// extrahera topnivå-relationer (objekt-objekt, ej i kontext)
		extractTopLevelRelations(relations);

		// hämta ut diverse data ur en kontext-nod
		// värden från kontexten indexeras dels i angivet index och dels i
		// ett index per kontexttyp genom att skicka in ett prefix till ip.setCurrent()
		extractContextNodes(identifier, relations, gmlGeometries);

		// läs in värden från Image-noder (ska försvinna på sikt och ersättas med media-noder)
		extractImageNodes();

		// läs in värden från media-noder (from 1.11+, ska ersätta image-noderna)
		extractMediaNodes();

		// lägg in specialindex
		addSpecialIndices();

		return luceneDoc;
	}

	/**
	 * Extraherar och indexerar information som berör tjänsten posten kommer ifrån.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractServiceInformation() throws Exception{
		ip.setCurrent(IX_SERVICENAME);
		extractSingleValue(model, subject, getURIRef(uri_rServiceName), ip);
		// hämta ut serviceOrganization (01, fast 11 egentligen?)
		ip.setCurrent(IX_SERVICEORGANISATION);
		extractSingleValue(model, subject, getURIRef(uri_rServiceOrganization), ip);
	}

	/**
	 * Extraherar och indexerar specialindex, dvs "exists"-index.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void addSpecialIndices() throws Exception {
		// lägg in specialindex
		luceneDoc.addField(IX_GEODATAEXISTS, geoDataExists ? "j" : "n");
		luceneDoc.addField(IX_TIMEINFOEXISTS, timeInfoExists ? "j" : "n");
		// lägg till specialindex för om tumnagel existerar eller ej (j/n), IndexType.TOLOWERCASE
		final String thumbnail = extractSingleValue(model, subject, getURIRef(uri_rThumbnail), null);
		boolean thumbnailExists = thumbnail != null;
		luceneDoc.addField(IX_THUMBNAILEXISTS, thumbnailExists ? "j" : "n");
		luceneDoc.addField(IX_THUMBNAIL, thumbnail);
	}

	/**
	 * Extraherar, ev behandlar och indexerar indexdatum.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param identifier identifierare
	 * @param service tjänst
	 * @param added datum när posten först lades till
	 * @throws Exception vid fel
	 */
	protected void extractAndHandleIndexDates(String identifier, HarvestService service, Date added) throws Exception {
		// hämta ut createdDate (01, fast 11 egentligen? speciellt om man vill ha ut info
		// om nya objekt i indexet)
		Date created = null;
		String createdDate = extractSingleValue(model, subject, getURIRef(uri_rCreatedDate), null);
		if (createdDate != null) {
			created = TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_CREATEDDATE, createdDate, ip);
		} else {
			addProblemMessage("Value for '" + IX_CREATEDDATE +
					// troligen saknas det på alla så identifier inte med tillsvidare
					"' is missing"); //  för " + identifier);
		}
		// lite logik för att sätta datum då posten först lades till i indexet
		// i normala fall är added != null då den sätts när poster läggs till, men
		// det kan finnas gammalt data i repot som inte har nåt värde och då använder
		// vi den gamla logiken från innan databaskolumnen added fanns
		Date addedToIndex = added != null ? added: calculateAddedToIndex(service.getFirstIndexDate(), created);
		ip.setCurrent(IX_ADDEDTOINDEXDATE);
		ip.addToDoc(formatDate(addedToIndex, false));
		// hämta ut lastChangedDate (01, fast 11 egentligen?)
		String lastChangedDate = extractSingleValue(model, subject, getURIRef(uri_rLastChangedDate), null);
		if (lastChangedDate != null) {
			TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_LASTCHANGEDDATE, lastChangedDate, ip);
		} else {
			// lastChanged är inte lika viktig som createdDate så den varnar vi inte för tills vidare
			// addProblemMessage("Värde för '" + IX_LASTCHANGEDDATE +
			//		"' saknas för " + identifier);
		}

		String buildDate = extractSingleValue(model, subject, getURIRef(uri_rBuilddDate), null);
		if (buildDate != null) {
			TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_BUILD_DATE, buildDate, ip);
		}

	}

	/**
	 * Extraherar och indexerar information som berör klassificeringar.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractClassificationInformation() throws Exception {
		// TODO: subject inte rätt, är bara en uri-pekare nu(?)
		// hämta ut subject (0m)
		ip.setCurrent(IX_SUBJECT);
		extractValue(model, subject, getURIRef(uri_rSubject), null, ip);
		// hämta ut collection (0m)
		ip.setCurrent(IX_COLLECTION);
		extractValue(model, subject, getURIRef(uri_rCollection), null, ip);
		// hämta ut dataQuality (1)
		ip.setCurrent(IX_DATAQUALITY);
		extractSingleValue(model, subject, getURIRef(uri_rDataQuality), ip);
		// hämta ut mediaType (0n)
		ip.setCurrent(IX_MEDIATYPE);
		extractValue(model, subject, getURIRef(uri_rMediaType), null, ip);
		// hämta ut tema (0n)
		ip.setCurrent(IX_THEME);
		extractValue(model, subject, getURIRef(uri_rTheme), null, ip);
	}

	/**
	 * Extraherar och indexerar information som berör "item"-index, dvs huvuddata.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractItemInformation() throws Exception {
		// hämta ut itemTitle (0m)
		ip.setCurrent(IX_ITEMTITLE);
		extractValue(model, subject, getURIRef(uri_rItemTitle), null, ip);
		// hämta ut itemLabel (11)
		ip.setCurrent(IX_ITEMLABEL);
		extractSingleValue(model, subject, getURIRef(uri_rItemLabel), ip);
		// hämta ut itemType (1)
		ip.setCurrent(IX_ITEMTYPE);
		extractSingleValue(model, subject, getURIRef(uri_rItemType), ip);
		// hämta ut itemClass (0m)
		ip.setCurrent(IX_ITEMCLASS, false); // slå inte upp uri
		extractValue(model, subject, getURIRef(uri_rItemClass), null, ip);
		// hämta ut itemClassName (0m)
		ip.setCurrent(IX_ITEMCLASSNAME);
		extractValue(model, subject, getURIRef(uri_rItemClassName), null, ip);
		// hämta ut itemName (1m)
		ip.setCurrent(IX_ITEMNAME);
		extractValue(model, subject, getURIRef(uri_rItemName), getURIRef(uri_r__Name), ip);
		// hämta ut itemSpecification (0m)
		ip.setCurrent(IX_ITEMSPECIFICATION);
		extractValue(model, subject, getURIRef(uri_rItemSpecification), getURIRef(uri_r__Spec), ip);
		// hämta ut itemKeyWord (0m)
		ip.setCurrent(IX_ITEMKEYWORD);
		extractValue(model, subject, getURIRef(uri_rItemKeyWord), null, ip);
		// hämta ut itemMotiveWord (0m)
		ip.setCurrent(IX_ITEMMOTIVEWORD);
		extractValue(model, subject, getURIRef(uri_rItemMotiveWord), null, ip);
		// hämta ut itemMaterial (0m)
		ip.setCurrent(IX_ITEMMATERIAL);
		extractValue(model, subject, getURIRef(uri_rItemMaterial), getURIRef(uri_rMaterial), ip);
		// hämta ut itemTechnique (0m)
		ip.setCurrent(IX_ITEMTECHNIQUE);
		extractValue(model, subject, getURIRef(uri_rItemTechnique), null, ip);
		// hämta ut itemStyle (0m)
		ip.setCurrent(IX_ITEMSTYLE);
		extractValue(model, subject, getURIRef(uri_rItemStyle), null, ip);
		// hämta ut itemColor (0m)
		ip.setCurrent(IX_ITEMCOLOR);
		extractValue(model, subject, getURIRef(uri_rItemColor), null, ip);
		// hämta ut itemNumber (0m)
		ip.setCurrent(IX_ITEMNUMBER);
		extractValue(model, subject, getURIRef(uri_rItemNumber), getURIRef(uri_rNumber), ip);

		// hämta ut itemDescription, resursnod (0m)
		ip.setCurrent(IX_ITEMDESCRIPTION); // fritext
		extractValue(model, subject, getURIRef(uri_rItemDescription), getURIRef(uri_r__Desc), ip);
		// hämta ut itemLicense (01)
		ip.setCurrent(IX_ITEMLICENSE, false); // uri, ingen uppslagning fn
		extractSingleValue(model, subject, getURIRef(uri_rItemLicense), ip);


	}

	/**
	 * Tar bildnoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 *
	 * @throws Exception vid fel
	 */
	protected void extractImageNodes() throws Exception {
		// läs in värden från Image-noder
		Selector selector = new SimpleSelector(subject, getURIRef(uri_rImage), (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			Statement s = iter.next();
			if (s.getObject().isResource()){
				extractImageNodeInformation(s.getObject().asResource());
			}
		}
	}




	/**
	 * Extraherar och indexerar information ur en bildnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS bildnod
	 * @throws Exception vid fel
	 */
	protected void extractImageNodeInformation(Resource cS) throws Exception {
		extractMediaLicense(cS);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(model, cS, getURIRef(uri_rMediaMotiveWord), null, ip);
		ip.setCurrent(IX_THUMBNAIL_SOURCE, false);
		extractValue(model, cS, getURIRef(uri_rThumbnailSource), null, ip);
		ip.setCurrent(IX_LOWRES_SOURCE, false);
		extractValue(model, cS, getURIRef(uri_rLowresSource), null, ip);
		ip.setCurrent(IX_HIGHRES_SOURCE, false);
		extractValue(model, cS, getURIRef(uri_rHighresSource), null, ip);
	}

	void extractMediaLicense(Resource cS) throws Exception {
		ip.setCurrent(IX_MEDIALICENSE, false); // uri, ingen uppslagning fn
		final String mediaLicense = extractValue(model, cS, getURIRef(uri_rMediaLicense), null, ip);
		if (requireMediaLicense && mediaLicense == null) {
			throw new SamsokProtocolException("Missing mediaLicense","for identifier " + subject.toString());
		}
	}

	/**
	 * Platshållare för metod som tar medianoder och extraherar och indexerar information ur dem.
	 * Basmetoden gör inget utan måste överlagras i subklasser då medianoder introducerades
	 * iom version 1.2, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @throws Exception vid fel
	 */
	protected void extractMediaNodes() throws Exception {
	}

	/**
	 * Tar kontextnoder och extraherar och indexerar information ur dem.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param identifier identifierare
	 * @param relations relationslista
	 * @param gmlGeometries gml-lista
	 * @throws Exception vid fel
	 */
	protected void extractContextNodes(String identifier, List<String> relations, List<String> gmlGeometries) throws Exception {
		Selector selector = new SimpleSelector(subject, getURIRef(uri_rContext), (RDFNode) null);
		StmtIterator iter = model.listStatements(selector);
		while (iter.hasNext()){
			Statement s = iter.next();
			if (s.getObject().isResource()){
				extractContextNodeInformation(s.getObject().asResource(), identifier, relations, gmlGeometries);
			}else {
				logger.warn("context borde vara en blank-nod? Ingen context-info utläst");
			}
		}
	}

	/**
	 * Extraherar och indexerar information ur en kontextnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @param relations relationslista
	 * @param gmlGeometries gml-lista
	 * @throws Exception vid fel
	 */
	protected void extractContextNodeInformation(Resource cS, String identifier, List<String> relations, List<String> gmlGeometries) throws Exception {
		// hämta ut vilket kontext vi är i mm
		String[] contextTypes = extractContextTypeAndLabelInformation(cS, identifier);
		// place
		extractContextPlaceInformation(cS, contextTypes, gmlGeometries);
		// actor
		extractContextActorInformation(cS, contextTypes, relations);
		// time
		extractContextTimeInformation(cS, contextTypes);
	}

	/**
	 * Extraherar och indexerar typinformation ur en kontextnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param identifier identifierare
	 * @return kontexttyp, kortnamn
	 * @throws Exception vid fel
	 */
	protected String[] extractContextTypeAndLabelInformation(Resource cS, String identifier) throws Exception {
		// hämta ut vilket kontext vi är i
		// OBS! Använder inte contexttype.rdf för uppslagning av denna utan lägger
		// det uppslagna värde i contextLabel
		String contextType = extractSingleValue(model, cS, getURIRef(uri_rContextType), null);
		if (contextType != null) {
			String contextLabel = lookupURIValue(contextType);
			contextType = restIfStartsWith(contextType, context_pre);
			// TODO: verifiera från lista istället
			if (contextType != null) {
				if (contextType.contains("#")) {
					// börjar den inte med rätt prefix och är en uri kan vi
					// lika gärna strunta i den...
					if (logger.isDebugEnabled()) {
						logger.debug("contextType med felaktig uri för " + identifier +
								": " + contextType);
					}
					contextType = null;
				} else {
					ip.setCurrent(IX_CONTEXTTYPE);
					ip.addToDoc(contextType);
				}
			}
			if (contextLabel != null) {
				ip.setCurrent(IX_CONTEXTLABEL);
				ip.addToDoc(contextLabel);
			} else {
				ip.setCurrent(IX_CONTEXTLABEL);
				extractSingleValue(model, cS, getURIRef(uri_rContextLabel), ip);
			}
		}
		return contextType != null ? new String[] { contextType } : null;
	}

	/**
	 * Extraherar och indexerar platsinformation ur en kontextnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextPlaceInformation(Resource cS, String[] contextTypes, List<String> gmlGeometries) throws Exception {
		// place

		// 0-m
		ip.setCurrent(IX_PLACENAME, contextTypes);
		extractValue(model, cS, getURIRef(uri_rPlaceName), null, ip);

		ip.setCurrent(IX_CADASTRALUNIT, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rCadastralUnit), ip);


		// Vi vill inte använda placeTermId/placeTermAuth längre, utan slå ihop dem till IX_PLACETERM
		// För att inte spara dem i dokumentet skickar vi inte med ip
		String placeTermId = extractSingleValue(model, cS, getURIRef(uri_rPlaceTermId), null);
		String placeTermAuth = extractSingleValue(model, cS, getURIRef(uri_rPlaceTermAuth), null);

		// Slå ihop dem och lägg till i doc
		// Note: vi vet inte hur de ser ut på riktigt, vi antar att de har slash på slutet
		if (placeTermAuth != null) {
			if (!placeTermAuth.endsWith("/")) {
				placeTermAuth += ("/");
			}
			if (placeTermId != null) {
				String placeTerm = placeTermAuth + placeTermId;
				ip.addToDoc(IX_PLACETERM, placeTerm);

				// lägg också till dem i relUri, eftersom de nu är en relationsUri
				ip.addToDoc(IX_RELURI, placeTerm);
			}
		}

		ip.setCurrent(IX_CONTINENTNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rContinentName), ip);

		ip.setCurrent(IX_COUNTRYNAME, contextTypes);
		extractValue(model, cS, getURIRef(uri_rCountryName), ip);

		ip.setCurrent(IX_COUNTYNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rCountyName), ip);

		ip.setCurrent(IX_MUNICIPALITYNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rMunicipalityName), ip);

		ip.setCurrent(IX_PROVINCENAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rProvinceName), ip);

		ip.setCurrent(IX_PARISHNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rParishName), ip);

		ip.setCurrent(IX_COUNTRY, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rCountry), ip);

		ip.setCurrent(IX_COUNTY, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rCounty), ip);

		ip.setCurrent(IX_MUNICIPALITY, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rMunicipality), ip);

		ip.setCurrent(IX_PROVINCE, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rProvince), ip);

		ip.setCurrent(IX_PARISH, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rParish), ip);

		// hämta ut gml
		String gml = extractSingleValue(model, cS, getURIRef(uri_rCoordinates), null);
		if (gml != null && gml.length() > 0) {
			gmlGeometries.add(gml);
			// flagga att det finns geodata
			geoDataExists = true;
		}
	}

	/**
	 * Extraherar och indexerar agentinformation ur en kontextnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextActorInformation(Resource cS, String[] contextTypes, List<String> relations) throws Exception {
		// actor

		ip.setCurrent(IX_FIRSTNAME, contextTypes);
		String firstName = extractSingleValue(model, cS, getURIRef(uri_rFirstName), ip);

		ip.setCurrent(IX_SURNAME, contextTypes);
		String lastName = extractSingleValue(model, cS, getURIRef(uri_rSurname), ip);

		ip.setCurrent(IX_FULLNAME, contextTypes);
		String fullName = extractSingleValue(model, cS, getURIRef(uri_rFullName), ip);

		// om vi inte har fått ett fullName men har ett förnamn och ett efternamn så lägger vi in det i IX_FULLNAME
		if (fullName == null && firstName != null && lastName != null) {
			ip.setCurrent(IX_FULLNAME, contextTypes);
			ip.addToDoc(firstName + " " + lastName);
		}

		ip.setCurrent(IX_NAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rName), ip);

		// TODO: bara vissa värden? http://xmlns.com/foaf/spec/#term_gender:
		// "In most cases the value will be the string 'female' or 'male' (in
		//  lowercase without surrounding quotes or spaces)."
		ip.setCurrent(IX_GENDER, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rGender), ip);

		ip.setCurrent(IX_ORGANIZATION, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rOrganization), ip);

		ip.setCurrent(IX_TITLE, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rTitle), ip);

	 	// Vi vill inte använda nameId/nameAuth längre, utan slå ihop dem till IX_AGENT
		// För att inte spara dem i dokumentet skickar vi inte med ip
		String nameId = extractSingleValue(model, cS, getURIRef(uri_rNameId), null);
		String nameAuth = extractSingleValue(model, cS, getURIRef(uri_rNameAuth), null);

		// Slå ihop dem och lägg till i doc
		if (nameAuth != null && nameId != null) {
			String agent = null;
			if (nameAuth.startsWith(OAIPMHHandler.LIBRIS_AUTH_URI)) {
				// slå ihop med id-content
				agent = OAIPMHHandler.fixContent(nameAuth, OAIPMHHandler.SLASH, nameId);
			} else if (nameAuth.equals(OAIPMHHandler.KUNGLIGA_BIBLIOTEKET)) {
				// Byt ut mot libris och slå ihop med id-content
				agent = OAIPMHHandler.fixContent(OAIPMHHandler.LIBRIS_AUTH_URI, OAIPMHHandler.SLASH, nameId);
			} else if (nameAuth.equals(OAIPMHHandler.VIAF)) {
				// Byt ut mot viaf-url och slå ihop med id-content
				agent = OAIPMHHandler.fixContent(OAIPMHHandler.VIAF_AUTH_URI, OAIPMHHandler.SLASH, nameId);
			}
			if (agent != null) {
				ip.addToDoc(IX_AGENT, agent);

				// lägg också till dem i relUri, eftersom de nu är en relationsUri
				ip.addToDoc(IX_RELURI, agent);
			}
		} 
	}

	/**
	 * Extraherar och indexerar tidsinformation ur en kontextnod.
	 * Hanterar de index som gällde för protokollversioner till och med 1.0, se dok.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param cS kontextnod
	 * @param contextTypes kontexttypnamn
	 * @throws Exception vid fel
	 */
	protected void extractContextTimeInformation(Resource cS, String[] contextTypes) throws Exception {
		// time

		ip.setCurrent(IX_FROMTIME, contextTypes);
		String fromTime = extractSingleValue(model, cS, getURIRef(uri_rFromTime), ip);

		ip.setCurrent(IX_TOTIME, contextTypes);
		String toTime = extractSingleValue(model, cS, getURIRef(uri_rToTime), ip);


		// I can't for the life of me understand why fromTime and toTime becomes "null" instead of null when there
		// are empty time tags
		if ("null".equals(fromTime)) {
			fromTime = null;
		}

		if ("null".equals(toTime)) {
			toTime = null;
		}

		// hantera ? i tidsfälten
		if (fromTime != null && fromTime.startsWith("?")) {
			fromTime = null;
		}
		if (toTime != null && toTime.startsWith("?")) {
			toTime = null;
		}
		// flagga om vi har tidsinfo
		if (fromTime != null || toTime != null) {
			timeInfoExists = true;
		}

		// hantera årtionden och århundraden
		TimeUtil.expandDecadeAndCentury(fromTime, toTime, contextTypes, ip);

		ip.setCurrent(IX_FROMPERIODNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rFromPeriodName), ip);

		ip.setCurrent(IX_TOPERIODNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rToPeriodName), ip);


		// Vi vill inte använda fromPeriodId/toPeriodId/periodAuth längre, utan slå ihop dem till IX_FROMPERIOD respektive IX_TOPERIOD
		// För att inte spara dem i dokumentet skickar vi inte med ip
		String fromPeriodId = extractSingleValue(model, cS, getURIRef(uri_rFromPeriodId), null);
		String toPeriodId = extractSingleValue(model, cS, getURIRef(uri_rToPeriodId), null);
		String periodAuth = extractSingleValue(model, cS, getURIRef(uri_rPeriodAuth), null);

		// Slå ihop dem och spara dem i doc
		if (periodAuth != null) {
			String fromPeriod = null;
			String toPeriod = null;
			if (periodAuth.startsWith(OAIPMHHandler.KULTURARVSDATA_PERIOD_AUTH_URI)) {
				// de här ska behållas som de är
				fromPeriod = periodAuth;
				toPeriod = periodAuth;
			} else if (periodAuth.startsWith(OAIPMHHandler.MIS_AUTH_URI)) {
				// Slå ihop med id-content
				fromPeriod = OAIPMHHandler.fixContent(periodAuth, OAIPMHHandler.HASHTAG, fromPeriodId);
				toPeriod = OAIPMHHandler.fixContent(periodAuth, OAIPMHHandler.HASHTAG, toPeriodId);
			}
			if (fromPeriod != null) {
				ip.addToDoc(IX_FROMPERIOD, fromPeriod);

				// lägg också till dem i relUri, eftersom de nu är en relationsUri
				ip.addToDoc(IX_RELURI, fromPeriod);
			}
			if (toPeriod != null) {
				ip.addToDoc(IX_TOPERIOD, toPeriod);

				// lägg också till dem i relUri, eftersom de nu är en relationsUri
				ip.addToDoc(IX_RELURI, toPeriod);
			}
		} 
		
		ip.setCurrent(IX_EVENTNAME, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rEventName), ip);

		// Vi vill översätta eventauth till event, så vi skickar med IX_EVENT här istället för IX_EVENTAUTH
		ip.setCurrent(IX_EVENT, contextTypes);
		extractSingleValue(model, cS, getURIRef(uri_rEventAuth), ip);
	}

	/**
	 * Ger map med giltiga toppnivårelationer nycklat på indexnamn.
	 * 
	 * Överlagra i subklasser vid behov.
	 * @return map med toppnivårelationer
	 */
	protected abstract Map<String, URI> getTopLevelRelationsMap();

	/**
	 * Extraherar och indexerar toppnivårelationer som hämtas via
	 * {@linkplain #getTopLevelRelationsMap()}.
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param relations lista med relationer för specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractTopLevelRelations(List<String> relations) throws Exception {
		Map<String, URI> relationsMap = getTopLevelRelationsMap();
		extractRelationsFromNode(subject, relationsMap, relations);
	}

	/**
	 * Extraherar och indexerar relationer från mappen från inskickad nod.  
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param relations lista med relationer för specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractRelationsFromNode(Resource subjectNode,
			Map<String, URI> relationsMap, List<String> relations) throws Exception {
		// relationer, in i respektive index + i IX_RELURI
		final String[] relIx = new String[] { null, IX_RELURI };
		for (Entry<String, URI> entry: relationsMap.entrySet()) {
			relIx[0] = entry.getKey();
			ip.setCurrent(relIx, false);
			extractValue(model, subjectNode, getURIRef(entry.getValue()), null, ip, relations);
		}
	}

	/**
	 * Beräknar när posten först lades till indexet, används för att få fram listningar
	 * på nya objekt i indexet. Hänsyn tas till när tjänsten först indexerades och när
	 * posten skapades för att få fram ett någorlunda bra datum, se kommentar nedan.
	 * Beräknas i praktiken som max(firstIndexed, recordCreated).
	 * 
	 * @param firstIndexed när tjänsten först indexerades i k-samsök, om känt
	 * @param recordCreated när tjänsten säger att posten skapades
	 * @return beräknat datum för när posten först indexerades i k-samsök
	 */
	static Date calculateAddedToIndex(Date firstIndexed, Date recordCreated) {
		Date addedToIndex;
		if (firstIndexed != null) {
			// tjänsten har redan indexerats (minst) en gång
			if (recordCreated != null) {
				if (recordCreated.after(firstIndexed)) {
					// nytt objekt i redan indexerad tjänst
					// OBS:  detta datum är inte riktigt 100% sant egentligen utan det
					//       beror också på med vilken frekvens tjänsten skördas, tex
					//       om tjänsten skördas var tredje dag så kan det skilja på
					//       två dagar när objektet egentligen först dök upp i indexet
					//       och vilket värde som används, så fn får det ses som en uppskattning
					addedToIndex = recordCreated;
				} else {
					// "gammalt" objekt i redan indexerad tjänst
					addedToIndex = firstIndexed;
				}
			} else {
				// objekt med okänt skapad-datum i redan indexerad tjänst
				// TODO: sätta "nu" istället eller är detta ok? kan vara nytt, kan vara
				//       gammalt men då vi inte vet kanske det ska få vara gammalt?
				addedToIndex = firstIndexed;
			}
		} else {
			// ny tjänst -> nytt objekt
			// TODO: kanske alltid ska ha samma värde? kan gå över dygngräns
			//       egentligen vill man alltid ha datumet då indexeringen lyckades (= gick
			//       klart ok) men det vet man ju aldrig här (varken om eller när)...
			addedToIndex = new Date();
		}
		return addedToIndex;
	}

	// hjälpmetod som tar ut suffixet ur strängen om den startar med inskickad startsträng
	static String restIfStartsWith(String str, String start) {
		return restIfStartsWith(str, start, false);
	}

	// hjälpmetod som tar ut suffixet ur strängen om den startar med inskickad startsträng
	// och försöker tolka värdet som ett heltal om asNumber är sant
	static String restIfStartsWith(String str, String start, boolean asNumber) {
		String value = null;
		if (str.startsWith(start)) {
			value = str.substring(start.length());
			if (asNumber) {
				try {
					value = Long.valueOf(value).toString();
				} catch (NumberFormatException nfe) {
					ContentHelper.addProblemMessage("Could not interpret the end of " + str + " (" + value + ") as a digit");
				}
			}
		}
		return value;
	}


	@Override
	public void setRequireMediaLicense(boolean requireMediaLicense) {
		this.requireMediaLicense = requireMediaLicense;
	}

	/**
	 * NOTE: Apache 2.0 licens, så kopiering är ok.
	 * 
	 * Returns a Log4J logger configured as the calling class. This ensures copy-paste safe code to get a logger instance,
	 * an ensures the logger is always fetched in a consistent manner. <br>
	 * <b>usage:</b><br>
	 * 
	 * <pre>
	 * private final static Logger log = LoggerHelper.getLogger();
	 * </pre>
	 * 
	 * Since the logger is found by accessing the call stack it is important, that references are static.
	 * <p>
	 * The code is JDK1.4 compatible
	 * 
	 * @since 0.05
	 * @return log4j logger instance for the calling class
	 * @author Kasper B. Graversen
	 */
	public static Logger getClassLogger() {
		final Throwable t = new Throwable();
		t.fillInStackTrace();
		return LogManager.getLogger(t.getStackTrace()[1].getClassName());
	}

}
