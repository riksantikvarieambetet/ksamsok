package se.raa.ksamsok.lucene;

import static se.raa.ksamsok.lucene.ContentHelper.IX_ADDEDTOINDEXDATE;
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
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENTAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_EVENTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FIRSTNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIODID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FROMTIME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_FULLNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GENDER;
import static se.raa.ksamsok.lucene.ContentHelper.IX_GEODATAEXISTS;
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
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIALICENSE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIAMOTIVEWORD;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MEDIATYPE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITY;
import static se.raa.ksamsok.lucene.ContentHelper.IX_MUNICIPALITYNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAMEAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_NAMEID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_ORGANIZATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PARISHNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PERIODAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACETERMAUTH;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PLACETERMID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_PROVINCENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_RELURI;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICENAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SERVICEORGANISATION;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SUBJECT;
import static se.raa.ksamsok.lucene.ContentHelper.IX_SURNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THEME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_THUMBNAILEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TIMEINFOEXISTS;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TITLE;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODID;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOPERIODNAME;
import static se.raa.ksamsok.lucene.ContentHelper.IX_TOTIME;
import static se.raa.ksamsok.lucene.ContentHelper.addProblemMessage;
import static se.raa.ksamsok.lucene.ContentHelper.formatDate;
import static se.raa.ksamsok.lucene.RDFUtil.extractSingleValue;
import static se.raa.ksamsok.lucene.RDFUtil.extractValue;
import static se.raa.ksamsok.lucene.SamsokProtocol.context_pre;
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
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rTitle;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodId;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToPeriodName;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_rToTime;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Desc;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Name;
import static se.raa.ksamsok.lucene.SamsokProtocol.uri_r__Spec;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;

import se.raa.ksamsok.harvest.HarvestService;

public abstract class BaseSamsokProtocolHandler implements SamsokProtocolHandler, RelationToIndexMapper {

	protected final Logger logger;

	protected final Graph graph;
	protected final GraphElementFactory elementFactory;
	protected final SubjectNode s;
	protected final IndexProcessor ip;
	protected final SolrInputDocument luceneDoc;

	/**
	 * Map som håller ev återkommande uri:er och uri-referenser för att slippa
	 * skapa dem flera gånger eller ha variabler
	 */
	protected Map<URI, URIReference> mapper = new HashMap<URI, URIReference>();

	protected boolean timeInfoExists = false;
	protected boolean geoDataExists = false;

	protected BaseSamsokProtocolHandler(Graph graph, SubjectNode s) {
		logger = getLogger();
		this.graph = graph;
		this.elementFactory = graph.getElementFactory();
		this.s = s;
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
	 * @param elementFactory factory
	 * @param uri uri
	 * @return en URIReference
	 * @throws GraphException vid fel
	 */
	protected URIReference getURIRef(GraphElementFactory elementFactory, URI uri) throws GraphException {
		URIReference ref = mapper.get(uri);
		if (ref == null) {
			ref = elementFactory.createURIReference(uri);
			mapper.put(uri, ref);
		}
		return ref;
	}

	@Override
	public SolrInputDocument handle(HarvestService service, Date added,
			List<String> relations, List<String> gmlGeometries)
			throws Exception {

		String identifier = s.toString();

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

		// läs in värden från Image-noder
		extractImageNodes();

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
	protected void extractServiceInformation() throws Exception {
		ip.setCurrent(IX_SERVICENAME);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rServiceName), ip);
		// hämta ut serviceOrganization (01, fast 11 egentligen?)
		ip.setCurrent(IX_SERVICEORGANISATION);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rServiceOrganization), ip);
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
		boolean thumbnailExists = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rThumbnail), null) != null;
		luceneDoc.addField(IX_THUMBNAILEXISTS, thumbnailExists ? "j" : "n");

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
		String createdDate = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rCreatedDate), null);
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
		String lastChangedDate = extractSingleValue(graph, s, getURIRef(elementFactory, uri_rLastChangedDate), null);
		if (lastChangedDate != null) {
			TimeUtil.parseAndIndexISO8601DateAsDate(identifier, IX_LASTCHANGEDDATE, lastChangedDate, ip);
		} else {
			// lastChanged är inte lika viktig som createdDate så den varnar vi inte för tills vidare
			// addProblemMessage("Värde för '" + IX_LASTCHANGEDDATE +
			//		"' saknas för " + identifier);
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
		extractValue(graph, s, getURIRef(elementFactory, uri_rSubject), null, ip);
		// hämta ut collection (0m)
		ip.setCurrent(IX_COLLECTION);
		extractValue(graph, s, getURIRef(elementFactory, uri_rCollection), null, ip);
		// hämta ut dataQuality (1)
		ip.setCurrent(IX_DATAQUALITY);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rDataQuality), ip);
		// hämta ut mediaType (0n)
		ip.setCurrent(IX_MEDIATYPE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rMediaType), null, ip);
		// hämta ut tema (0n)
		ip.setCurrent(IX_THEME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rTheme), null, ip);
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
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemTitle), null, ip);
		// hämta ut itemLabel (11)
		ip.setCurrent(IX_ITEMLABEL);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemLabel), ip);
		// hämta ut itemType (1)
		ip.setCurrent(IX_ITEMTYPE);
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemType), ip);
		// hämta ut itemClass (0m)
		ip.setCurrent(IX_ITEMCLASS, false); // slå inte upp uri
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemClass), null, ip);
		// hämta ut itemClassName (0m)
		ip.setCurrent(IX_ITEMCLASSNAME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemClassName), null, ip);
		// hämta ut itemName (1m)
		ip.setCurrent(IX_ITEMNAME);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemName), getURIRef(elementFactory, uri_r__Name), ip);
		// hämta ut itemSpecification (0m)
		ip.setCurrent(IX_ITEMSPECIFICATION);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemSpecification), getURIRef(elementFactory, uri_r__Spec), ip);
		// hämta ut itemKeyWord (0m)
		ip.setCurrent(IX_ITEMKEYWORD);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemKeyWord), null, ip);
		// hämta ut itemMotiveWord (0m)
		ip.setCurrent(IX_ITEMMOTIVEWORD);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemMotiveWord), null, ip);
		// hämta ut itemMaterial (0m)
		ip.setCurrent(IX_ITEMMATERIAL);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemMaterial), getURIRef(elementFactory, uri_rMaterial), ip);
		// hämta ut itemTechnique (0m)
		ip.setCurrent(IX_ITEMTECHNIQUE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemTechnique), null, ip);
		// hämta ut itemStyle (0m)
		ip.setCurrent(IX_ITEMSTYLE);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemStyle), null, ip);
		// hämta ut itemColor (0m)
		ip.setCurrent(IX_ITEMCOLOR);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemColor), null, ip);
		// hämta ut itemNumber (0m)
		ip.setCurrent(IX_ITEMNUMBER);
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemNumber), getURIRef(elementFactory, uri_rNumber), ip);
		// hämta ut itemDescription, resursnod (0m)
		ip.setCurrent(IX_ITEMDESCRIPTION); // fritext
		extractValue(graph, s, getURIRef(elementFactory, uri_rItemDescription), getURIRef(elementFactory, uri_r__Desc), ip);
		// hämta ut itemLicense (01)
		ip.setCurrent(IX_ITEMLICENSE, false); // uri, ingen uppslagning fn
		extractSingleValue(graph, s, getURIRef(elementFactory, uri_rItemLicense), ip);
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
		for (Triple triple: graph.find(s, getURIRef(elementFactory, uri_rImage), AnyObjectNode.ANY_OBJECT_NODE)) {
			if (triple.getObject() instanceof SubjectNode) {
				SubjectNode cS = (SubjectNode) triple.getObject();

				extractImageNodeInformation(cS);
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
	protected void extractImageNodeInformation(SubjectNode cS) throws Exception {
		ip.setCurrent(IX_MEDIALICENSE, false); // uri, ingen uppslagning fn
		extractValue(graph, cS, getURIRef(elementFactory, uri_rMediaLicense), null, ip);
		ip.setCurrent(IX_MEDIAMOTIVEWORD);
		extractValue(graph, cS, getURIRef(elementFactory, uri_rMediaMotiveWord), null, ip);
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
		for (Triple triple: graph.find(s, getURIRef(elementFactory, uri_rContext), AnyObjectNode.ANY_OBJECT_NODE)) {
			if (triple.getObject() instanceof SubjectNode) {
				SubjectNode cS = (SubjectNode) triple.getObject();

				extractContextNodeInformation(cS, identifier, relations, gmlGeometries);
			} else {
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
	protected void extractContextNodeInformation(SubjectNode cS, String identifier, List<String> relations, List<String> gmlGeometries) throws Exception {
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
	protected String[] extractContextTypeAndLabelInformation(SubjectNode cS, String identifier) throws Exception {
		// hämta ut vilket kontext vi är i
		// OBS! Använder inte contexttype.rdf för uppslagning av denna utan lägger
		// det uppslagna värde i contextLabel
		String contextType = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextType), null);
		if (contextType != null) {
			String contextLabel = lookupURIValue(contextType);
			contextType = restIfStartsWith(contextType, context_pre);
			// TODO: verifiera från lista istället
			if (contextType != null) {
				if (contextType.indexOf("#") >= 0) {
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
				extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContextLabel), ip);
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
	protected void extractContextPlaceInformation(SubjectNode cS, String[] contextTypes, List<String> gmlGeometries) throws Exception {
		// place

		// 0-m
		ip.setCurrent(IX_PLACENAME, contextTypes);
		extractValue(graph, cS, getURIRef(elementFactory, uri_rPlaceName), null, ip);

		ip.setCurrent(IX_CADASTRALUNIT, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCadastralUnit), ip);

		ip.setCurrent(IX_PLACETERMID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPlaceTermId), ip);

		ip.setCurrent(IX_PLACETERMAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPlaceTermAuth), ip);

		ip.setCurrent(IX_CONTINENTNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rContinentName), ip);

		ip.setCurrent(IX_COUNTRYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountryName), ip);

		ip.setCurrent(IX_COUNTYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountyName), ip);

		ip.setCurrent(IX_MUNICIPALITYNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rMunicipalityName), ip);

		ip.setCurrent(IX_PROVINCENAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rProvinceName), ip);

		ip.setCurrent(IX_PARISHNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rParishName), ip);

		ip.setCurrent(IX_COUNTRY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCountry), ip);

		ip.setCurrent(IX_COUNTY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCounty), ip);

		ip.setCurrent(IX_MUNICIPALITY, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rMunicipality), ip);

		ip.setCurrent(IX_PROVINCE, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rProvince), ip);

		ip.setCurrent(IX_PARISH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rParish), ip);

		// hämta ut gml
		String gml = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rCoordinates), null);
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
	protected void extractContextActorInformation(SubjectNode cS, String[] contextTypes, List<String> relations) throws Exception {
		// actor

		ip.setCurrent(IX_FIRSTNAME, contextTypes);
		String firstName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFirstName), ip);

		ip.setCurrent(IX_SURNAME, contextTypes);
		String lastName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rSurname), ip);

		ip.setCurrent(IX_FULLNAME, contextTypes);
		String fullName = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFullName), ip);

		// om vi inte har fått ett fullName men har ett förnamn och ett efternamn så lägger vi in det i IX_FULLNAME
		if (fullName == null && firstName != null && lastName != null) {
			ip.setCurrent(IX_FULLNAME, contextTypes);
			ip.addToDoc(firstName + " " + lastName);
		}

		ip.setCurrent(IX_NAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rName), ip);

		// TODO: bara vissa värden? http://xmlns.com/foaf/spec/#term_gender:
		// "In most cases the value will be the string 'female' or 'male' (in
		//  lowercase without surrounding quotes or spaces)."
		ip.setCurrent(IX_GENDER, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rGender), ip);

		ip.setCurrent(IX_ORGANIZATION, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rOrganization), ip);

		ip.setCurrent(IX_TITLE, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rTitle), ip);

		ip.setCurrent(IX_NAMEID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rNameId), ip);

		ip.setCurrent(IX_NAMEAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rNameAuth), ip);
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
	protected void extractContextTimeInformation(SubjectNode cS, String[] contextTypes) throws Exception {
		// time

		ip.setCurrent(IX_FROMTIME, contextTypes);
		String fromTime = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromTime), ip);

		ip.setCurrent(IX_TOTIME, contextTypes);
		String toTime = extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToTime), ip);

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
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromPeriodName), ip);

		ip.setCurrent(IX_TOPERIODNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToPeriodName), ip);

		ip.setCurrent(IX_FROMPERIODID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rFromPeriodId), ip);

		ip.setCurrent(IX_TOPERIODID, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rToPeriodId), ip);

		ip.setCurrent(IX_PERIODAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rPeriodAuth), ip);

		ip.setCurrent(IX_EVENTNAME, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rEventName), ip);

		ip.setCurrent(IX_EVENTAUTH, contextTypes);
		extractSingleValue(graph, cS, getURIRef(elementFactory, uri_rEventAuth), ip);
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
		extractRelationsFromNode(s, relationsMap, relations);
	}

	/**
	 * Extraherar och indexerar relationer från mappen från inskickad nod.  
	 * Överlagra i subklasser vid behov.
	 * 
	 * @param relations lista med relationer för specialrelationsindexet
	 * @throws Exception vid fel
	 */
	protected void extractRelationsFromNode(SubjectNode subjectNode,
			Map<String, URI> relationsMap, List<String> relations) throws Exception {
		// relationer, in i respektive index + i IX_RELURI
		final String[] relIx = new String[] { null, IX_RELURI };
		for (Entry<String, URI> entry: relationsMap.entrySet()) {
			relIx[0] = entry.getKey();
			ip.setCurrent(relIx, false);
			extractValue(graph, subjectNode, getURIRef(elementFactory, entry.getValue()), null, ip, relations);
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
		Date addedToIndex = null;
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
		return Logger.getLogger(t.getStackTrace()[1].getClassName());
	}

}
