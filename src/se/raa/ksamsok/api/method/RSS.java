package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.jrdf.JRDFFactory;
import org.jrdf.SortedMemoryJRDFFactory;
import org.jrdf.collection.MemMapFactory;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.GraphException;
import org.jrdf.graph.Literal;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.parser.StatementHandlerException;
import org.jrdf.parser.rdfxml.GraphRdfXmlParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

import com.sun.syndication.feed.module.georss.GeoRSSModule;
import com.sun.syndication.feed.module.georss.W3CGeoModuleImpl;
import com.sun.syndication.feed.module.georss.geometries.Position;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.MediaEntryModuleImpl;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;



/**
 * Metod för att få tillbaka en mediaRSS feed på ett sökresultat
 * från K-samsök
 * @author Henrik Hjalmarsson
 */
public class RSS implements APIMethod
{
	/** metodens namn */
	public static final String METHOD_NAME = "rss";
	/** namn på parametern för sök sträng */
	public static final String QUERY = "query";
	/** namn på parameter för att ange antal träffar per sida */
	public static final String HITS_PER_PAGE = "hitsPerPage";
	/** namn på parameter för att ange startplats i sökning */
	public static final String START_RECORD = "startRecord";
	/** standard värde för antal träffar per sida */
	public static final int DEFAULT_HITS_PER_PAGE = 100;
	/** standard värde för startplats i sökning */
	public static final int DEFAULT_START_RECORD = 1;
	
	// rss version
	private static final String RSS_2_0 = "rss_2.0";
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("sv",	"SE"));
	// fält att hämta från lucene
	private static final MapFieldSelector RDF_FIELDS = new MapFieldSelector(new String[] {ContentHelper.I_IX_RDF, ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER });
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.Search");

	//privata variabler
	private String queryString;
	private int hitsPerPage;
	private int startRecord;
	private PrintWriter writer;
	private RssObject data;
	
	//fabriker
	private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private static final JRDFFactory jrdfFactory = SortedMemoryJRDFFactory.getFactory();
	
	//URIs för att navigera RDF
	private static final String URI_PREFIX = "http://kulturarvsdata.se/";
	private static final String URI_PREFIX_KSAMSOK = URI_PREFIX + "ksamsok#";
	private static final URI URI_RDF_TYPE = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	private static final URI URI_KSAMSOK_ENTITY = URI.create(URI_PREFIX_KSAMSOK + "Entity");
	private static final URI URI_PRESENTATION = URI.create(URI_PREFIX_KSAMSOK + "presentation");
	private static final URI URI_ITEM_TITLE = URI.create(URI_PREFIX_KSAMSOK + "itemTitle");
	private static final URI URI_ITEM_KEY_WORD = URI.create(URI_PREFIX_KSAMSOK + "itemKeyWord");
	private static final URI URI_BUILD_DATE = URI.create(URI_PREFIX_KSAMSOK + "buildDate");
	
	/**
	 * Skapar ett objekt av RSS
	 * @param queryString CQL query sträng för att söka mot indexet
	 * @param hitsPerPage hur många träffar som skall visas per sida
	 * @param startRecord vart i resultatet sökningen skall starta
	 * @param writer används för att skriva svaret
	 */
	public RSS(String queryString, int hitsPerPage, int startRecord,
			PrintWriter writer)
	{
		this.queryString = queryString;
		this.writer = writer;
		if(startRecord < 1) { 
			this.startRecord = DEFAULT_START_RECORD;
		}else {
			this.startRecord = startRecord;
		}
		if(hitsPerPage > 500 || hitsPerPage < 1) {
			this.hitsPerPage = DEFAULT_HITS_PER_PAGE;
		}else {
			this.hitsPerPage = hitsPerPage;
		}
	}

	@Override
	public void performMethod() throws BadParameterException,
			DiagnosticException
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try {
			doSearch(searcher);
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	/**
	 * utför sökning och skriver svaret
	 * @param searcher IndexSearcher för att söka i index
	 * @throws DiagnosticException
	 * @throws BadParameterException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws FeedException
	 */
	protected void doSearch(IndexSearcher searcher) 
		throws DiagnosticException, BadParameterException
	{
		try {
			Query q = createQuery();
			int nDocs = startRecord - 1 + hitsPerPage;
			TopDocs hits = searcher.search(q, nDocs == 0 ? 1 : nDocs);
			int numberOfDocs = hits.totalHits;
			doSyndFeed(numberOfDocs, nDocs, searcher, hits);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel", "RSS.doSearch", e.getMessage(), true);
		}
	}
	
	/**
	 * skapar och skriver en RSS feed
	 * @param numberOfDocs
	 * @param nDocs
	 * @param searcher
	 * @param hits
	 * @throws DiagnosticException
	 */
	private void doSyndFeed(int numberOfDocs, int nDocs, IndexSearcher searcher, TopDocs hits) 
		throws DiagnosticException 
	{
		try {
			SyndFeed feed = getFeed();
			feed.setEntries(getEntries(numberOfDocs, nDocs, searcher, hits));
			SyndFeedOutput output = new SyndFeedOutput();
			output.output(feed, writer);
		} catch (CorruptIndexException e) {
			throw new DiagnosticException("Nånting gick fel : /", "RSS.doSyndFeed", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel", "RSS.doSyndFeed", e.getMessage(), true);
		} catch (FeedException e) {
			throw new DiagnosticException("Något gick fel : /", "RSS.doSyndFeed", e.getMessage(), true);
		}
	}
	
	/**
	 * Skapar query
	 * @return Ett Lucene query
	 * @throws DiagnosticException
	 * @throws BadParameterException
	 */
	private Query createQuery() 
		throws DiagnosticException, BadParameterException
	{
		Query q = null;
		try {
			CQLParser parser = new CQLParser();
			CQLNode rootNode = parser.parse(queryString);
			q = CQL2Lucene.makeQuery(rootNode);
		} catch (CQLParseException e) {
			throw new DiagnosticException("Parser fel. Kontrollera query sträng", "RSS.createQuery", null, false);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel. Försök igen", "RSS.createQuery", e.getMessage(), true);
		}
		return q;
	}
	
	/**
	 * returnerar en lista med RSS feed entries
	 * @param numberOfDocs 
	 * @param nDocs
	 * @param searcher
	 * @param hits
	 * @param entries
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws DiagnosticException 
	 */
	protected List<SyndEntry> getEntries(int numberOfDocs, int nDocs, 
			IndexSearcher searcher, TopDocs hits) 
		throws DiagnosticException
	{
		List<SyndEntry> entries = new Vector<SyndEntry>();
		try {
			for(int i = startRecord - 1;i < numberOfDocs && i < nDocs; i++) {
				String content = null;
				Document doc = searcher.doc(hits.scoreDocs[i].doc, RDF_FIELDS);
				String uri = doc.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				byte[] xmlData = doc.getBinaryValue(ContentHelper.I_IX_RDF);
				if (xmlData != null) {
					content = new String(xmlData, "UTF-8");
				}
				// TODO: NEK ta bort när allt är omindexerat
				if (content == null) {
					content = HarvesterServlet.getInstance().getHarvestRepositoryManager().getXMLData(uri);
				}
				if (content != null) {
					entries.add(getEntry(content));	
				} else {
					logger.warn("Hittade inte rdf-data för " + uri);
				}
			}
		} catch (CorruptIndexException e) {
			throw new DiagnosticException("Nånting gick fel : /", "RSS.getEntries", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel", "RSS.getEntries", e.getMessage(), true);
		} catch(Exception e) {
			throw new DiagnosticException("Fel vid hämtning av data", "RSS.getEntries", e.getMessage(), true);
		}
		return entries;
	}
	
	/**
	 * skapar ett entry till RSS feed
	 * @param content XML data som en sträng
	 * @return ett entry med data från XML sträng
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws DiagnosticException 
	 */
	@SuppressWarnings("unchecked")
	protected SyndEntry getEntry(String content) 
		throws DiagnosticException
	{
		SyndEntry entry = new SyndEntryImpl();
		try {
			data = getData(content);
			entry.setTitle(data.getTitle());
			entry.setLink(data.getLink());
			SyndContent syndContent = new SyndContentImpl();
			syndContent.setType("text/plain");
			syndContent.setValue(data.getDescription());
			entry.setDescription(syndContent);
			String thumb = data.getThumbnailUrl();
			String image = data.getImageUrl();
			if(data.getCoords() != null) {
				GeoRSSModule geoRssModule = getGeoRssModule(data.getCoords());
				if(geoRssModule != null) {
					entry.getModules().add(geoRssModule);
				}
			}
			if (!StringUtils.isEmpty(thumb) && !StringUtils.isEmpty(image)) {
				entry.getModules().add(getMediaModule(thumb, image));
			}
			synchronized (sdf) {
				if(data.getPublishDate() != null) {
					entry.setPublishedDate(sdf.parse(data.getPublishDate()));
				}
			}
		} catch (ParseException ignore) {}
		return entry;
	}
	
	private GeoRSSModule getGeoRssModule(String coords)
	{
		GeoRSSModule m = new W3CGeoModuleImpl();
		try {
			String[] coordsSplit = coords.split(",");
			double lon = Double.parseDouble(StringUtils.trim(coordsSplit[0]));
			double lat = Double.parseDouble(StringUtils.trim(coordsSplit[1]));
			m.setPosition(new Position(lat, lon));
		}catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		return m;
	}
	
	/**
	 * Hämtar data från RDF dokument
	 * @param content textsträng innehållande RDF data
	 * @return ett Rssobjekt med data
	 * @throws DiagnosticException - om fel uppstår vid hämtning av data 
	 */
	private RssObject getData(String content) 
		throws DiagnosticException
	{
		RssObject data = new RssObject();
		Graph graph = null;
		try {
			graph = getGraph(content);
			GraphElementFactory elementFactory = graph.getElementFactory();
			URIReference rRdfType = elementFactory.createURIReference(URI_RDF_TYPE);
			URIReference rKsamsokEntity = elementFactory.createURIReference(URI_KSAMSOK_ENTITY);
			URIReference rPresentation = elementFactory.createURIReference(URI_PRESENTATION);
			URIReference rItemTitle = elementFactory.createURIReference(URI_ITEM_TITLE);
			URIReference rItemKeyWord = elementFactory.createURIReference(URI_ITEM_KEY_WORD);
			URIReference rBuildDate = elementFactory.createURIReference(URI_BUILD_DATE);
			SubjectNode s = getSubjectNode(graph, rRdfType, rKsamsokEntity);
			data.setTitle(getValueFromGraph(graph, s, rItemTitle, null));
			data = getDataFromPresentationBlock(getSingleValueFromGraph(graph, s, rPresentation), data);
			String itemKeyWordsString = getValueFromGraph(graph, s, rItemKeyWord, null);
			String[] itemKeyWords = new String[0];
			if(itemKeyWordsString != null) {
				itemKeyWords = itemKeyWordsString.split(" ");
			}
			for(int i = 0; i < itemKeyWords.length; i ++) {
				data.addKeyWord(itemKeyWords[i]);
			}
			data.setPublishDate(getSingleValueFromGraph(graph, s, rBuildDate));
		}catch (GraphElementFactoryException e) {
			throw new DiagnosticException("Internt fel uppstod", "RSS.getData", e.getMessage(), true);
		}
		return data;
	}
	
	/**
	 * Skapar en RDF graf från textsträng
	 * @param content RDF data som textsträng
	 * @return RDF graf
	 * @throws DiagnosticException
	 */
	private Graph getGraph(String content) 
		throws DiagnosticException
	{
		Graph g = null;
		StringReader reader = null;
		try {
			reader = new StringReader(content);
			g = jrdfFactory.getNewGraph();
			GraphRdfXmlParser parser = new GraphRdfXmlParser(g, new MemMapFactory());
			parser.parse(reader, "");
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel uppstod", "RSS.getGraph", e.getMessage(), true);
		} catch (org.jrdf.parser.ParseException e) {
			throw new DiagnosticException("Oväntat parser fel uppstod", "RSS.getGraph", e.getMessage(), true);
		} catch (StatementHandlerException e) {
			throw new DiagnosticException("Internt fel uppstod", "RSS.getGraph", e.getMessage(), true);
		}finally {
			if(reader != null) {
				reader.close();
			}
		}
		return g;
	}
	
	/**
	 * returnerar root subject noden
	 * @param graph - grafen som noden skall hämtas ur
	 * @param rRdfType - URI referens till rdfType
	 * @param rKsamsokEntity - URI referens till ksamsokEntity
	 * @return en subject node
	 * @throws DiagnosticException om något fel uppstår när subject noden skall hämtas ur grafen
	 */
	private SubjectNode getSubjectNode(Graph graph, URIReference rRdfType, URIReference rKsamsokEntity) 
		throws DiagnosticException
	{
		SubjectNode s = null;
		try {
			for (Triple triple: graph.find(AnySubjectNode.ANY_SUBJECT_NODE, rRdfType, rKsamsokEntity)) {
				if (s != null) {
					throw new DiagnosticException("Ska bara finnas en entity i rdf-grafen", "se.raa.ksamsok.api.method.RSS.getSubjectNode", null, true);
				}
				s = triple.getSubject();
			}
			if (s == null) {
				logger.error("Hittade ingen entity i rdf-grafen:\n" + graph);
				throw new DiagnosticException("Hittade ingen entity i rdf-grafen", "se.raa.ksamsok.api.method.RSS.getSubjectNode", null, true);
			}
		}catch(GraphException e) {
			throw new DiagnosticException("Internt fel uppstod", "se.raa.ksamsok.api.method.RSS.getSubjectNode", "Fel uppstod vid hantering av RDF graf", true);
		}
		return s;
	}
	
	/**
	 * hämtar data från presentationsblocket
	 * @param presentationBlock presentationsblocket som textsträng
	 * @param data Rss objektet som datan skall läggas i
	 * @return RssObject med data
	 */
	private RssObject getDataFromPresentationBlock(String presentationBlock, RssObject data)
		throws DiagnosticException
	{
		org.w3c.dom.Document doc = getDOMDocument(presentationBlock); 
		NodeList nodeList = doc.getElementsByTagName("pres:item").item(0).getChildNodes();
		for(int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if(node.getNodeName().equals("pres:description")) {
				if(data.getDescription() != null) {
					data.setDescription(data.getDescription() + " " + node.getTextContent());
				}else {
					data.setDescription(node.getTextContent());
				}
			}else if(node.getNodeName().equals("pres:representations")) {
				NodeList childNodes = node.getChildNodes();
				for(int j = 0; j < childNodes.getLength(); j++) {
					Node child = childNodes.item(j);
					if(child.getAttributes().getNamedItem("format").getTextContent().equals("HTML")) {
						data.setLink(child.getTextContent());
					}
				}
			}else if(node.getNodeName().equals("pres:image")) {
				NodeList childNodes = node.getChildNodes();
				for(int j = 0; j < childNodes.getLength(); j++) {
					Node child = childNodes.item(j);
					if(child.getNodeName().equals("pres:src")) {
						if(child.getAttributes().getNamedItem("type").getTextContent().equals("lowres")) {
							data.setImageUrl(child.getTextContent());
						}else if(child.getAttributes().getNamedItem("type").getTextContent().equals("thumbnail")) {
							data.setThumbnailUrl(child.getTextContent());
						}
					}
				}
			}else if(node.getNodeName().equals("pres:itemLabel")) {
				if(StringUtils.trimToNull(data.getTitle()) == null) {
					data.setTitle(node.getTextContent());
				}
			}else if(node.getNodeName().equals("georss:where")) {
				Node child = node.getFirstChild().getFirstChild();
				data.setCoords(StringUtils.trimToNull(child.getTextContent()));
			}
		}
		return data;
	}
	
	/**
	 * Skapar ett DOM document för presentationsblocket
	 * @param presentationBlock - presentationsblocket som textsträng
	 * @return
	 * @throws DiagnosticException
	 */
	private org.w3c.dom.Document getDOMDocument(String presentationBlock)
		throws DiagnosticException
	{
		StringReader reader = null;
		org.w3c.dom.Document doc = null;
		try {
			reader = new StringReader(presentationBlock);
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(new InputSource(reader));
		} catch (ParserConfigurationException e) {
			throw new DiagnosticException("Internt fel", "RSS.getDOMDocument", e.getMessage(), true);
		} catch (SAXException e) {
			throw new DiagnosticException("Internt fel", "RSS.getDOMDocument", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("Internt fel", "RSS.getDOMDocument", e.getMessage(), true);
		}finally {
			if(reader != null) {
				reader.close();
			}
		}
		return doc;
	}
	
	/**
	 * Hämtar ett värde från RDF graf
	 * @param g - grafen som värdet skall hämtas ur
	 * @param sn - subject nod
	 * @param pn - predicate nod
	 * @return värde från graf som textsträng
	 */
	private String getSingleValueFromGraph(Graph g, SubjectNode sn, PredicateNode pn)
		throws DiagnosticException
	{
		String value = null;
		try {
			for(Triple t : g.find(sn, pn, AnyObjectNode.ANY_OBJECT_NODE)) {
				if (t.getObject() instanceof Literal) {
					value = StringUtils.trimToNull(((Literal) t.getObject()).getValue().toString()) + " ";
				}else if (t.getObject() instanceof URIReference) {
					value = StringUtils.trimToNull( ((URIReference) t.getObject()).getURI().toString());
				}
			}
		} catch (GraphException e) {
			throw new DiagnosticException("Internt fel", "RSS.getSingleValueFromGraph", e.getMessage(), true);
		}
		return value;
	}
	
	/**
	 * Hämtar ett eller flera värden från given RDF graf
	 * @param graph - RDF graf
	 * @param s - subject nod
	 * @param ref - URI referens till 
	 * @param refRef - URI referens till eventuella subnoder
	 * @return värden som textsträng
	 */
	private String getValueFromGraph(Graph graph, SubjectNode s, URIReference ref, URIReference refRef)
		throws DiagnosticException
	{
		final String sep = " ";
		StringBuffer buf = new StringBuffer();
		String value = null;
		try {
			for (Triple t: graph.find(s, ref, AnyObjectNode.ANY_OBJECT_NODE)) {
				if (t.getObject() instanceof Literal) {
					Literal l = (Literal) t.getObject();
					if (buf.length() > 0) {
						buf.append(sep);
					}
					value = l.getValue().toString();
					buf.append(value);
				} else if (t.getObject() instanceof URIReference) {
					value = StringUtils.trimToNull(((URIReference) t.getObject()).getURI().toString());
					// lägg till i buffer bara om detta är en uri vi ska slå upp värde för
					if (value != null) {
						if (buf.length() > 0) {
							buf.append(sep);
						}
						buf.append(value);
					}
				} else if (refRef != null && t.getObject() instanceof SubjectNode) {
					SubjectNode resSub = (SubjectNode) t.getObject();
					value = getSingleValueFromGraph(graph, resSub, refRef);
					if (value != null) {
						if (buf.length() > 0) {
							buf.append(sep);
						}
						buf.append(value);
					}
				}
			}
		}catch(GraphException e) {
			throw new DiagnosticException("Internt fel", "RSS.getValueFromGraph", e.getMessage(), true);
		}
		return buf.length() > 0 ? StringUtils.trimToNull(buf.toString()) : null;
	}
	
	/**
	 * Skapar en RSS feed och sätter några av dess attribut.
	 * @return SyndFeed
	 */
	protected SyndFeed getFeed()
	{
		SyndFeed feed = new SyndFeedImpl();
		feed.setFeedType(RSS_2_0);
		feed.setTitle("K-samsök sökresultat");
		feed.setLink(getFeedLinkProperty());
		feed.setDescription("Sök resultat av en sökning mod K-samsök API");
		
		return feed;
	}
	
	/**
	 * Skapar och sätter värden för en media module om bilder finns
	 * @param thumbnailUrl
	 * @param imageUrl
	 * @return Mediamodule med tumnagel och bild
	 * @throws DiagnosticException 
	 */
	protected MediaEntryModule getMediaModule(String thumbnailUrl, String imageUrl) 
		throws DiagnosticException
	{
		String thumb = StaticMethods.encode(thumbnailUrl);
		String image = StaticMethods.encode(imageUrl);
		MediaEntryModuleImpl mediaModule = new MediaEntryModuleImpl();
		try 
		{	
			mediaModule.setMetadata(getMetadata(thumb, mediaModule));
			mediaModule.setMediaContents(getImage(image));
		} catch (URISyntaxException e)
		{
			throw new DiagnosticException("Oväntat fel uppstod", "se.raa.ksamsok.api.method.RSS.getMediaModule()", e.getMessage(), true);
		}
		return mediaModule;
	}
	
	/**
	 * Skapar en bild i form av ett MediaContent[]
	 * @param image bildens URL
	 * @return MediaContent[] innehållande bild data
	 * @throws URISyntaxException
	 */
	protected MediaContent[] getImage(String image) 
		throws URISyntaxException
	{
		MediaContent[] contents = new MediaContent[1];
		MediaContent mediaContent = 
			new MediaContent(new UrlReference(image));
		mediaContent.setType(getImageType(image));
		contents[0] = mediaContent;		
		return contents;
	}
	
	/**
	 * returnerar ett Metadata objekt med URL till en tumnagel bild
	 * @param thumb URL till tumnagel
	 * @param mediaModule MediaModule som används
	 * @return Metadata objekt med tumnagel
	 * @throws URISyntaxException
	 * @throws DiagnosticException 
	 */
	protected Metadata getMetadata(String thumb, MediaEntryModule mediaModule) 
		throws URISyntaxException, DiagnosticException
	{
		Metadata metadata = mediaModule.getMetadata();
		if (metadata == null)
		{
			metadata = new Metadata();
		}
		metadata.setKeywords(data.getKeywordsAsArray());
		metadata.setThumbnail(getThumbnail(thumb));
		return metadata;
	}
	
	private Thumbnail[] getThumbnail(String thumb) 
		throws DiagnosticException
	{
		Thumbnail thumbnail = null;
		try {
			thumbnail = new Thumbnail(new URI(thumb));
		} catch (URISyntaxException e) {
			throw new DiagnosticException("Nånting blev fel", "RSS.getThumbnail", e.getMessage(), true);
		}
		Thumbnail[] thumbnails = new Thumbnail[1];
		thumbnails[0] = thumbnail;
		return thumbnails;
	}
	
	/**
	 * returnerar en stäng med MIME för bild
	 * @param image bild som skall få en MIME
	 * @return MIME som sträng
	 */
	protected String getImageType(String image)
	{
		String imageType = "image/jpeg"; // default
		if (image.endsWith(".gif")) 
		{
			imageType = "image/gif";						
		} else if (image.endsWith(".png")) 
		{
			imageType = "image/png";						
		}
		return imageType;
	}
	
	/**
	 * Returnerar en sträng med den URL som använts för att få detta 
	 * resultat
	 * @return URL som sträng
	 */
	protected String getFeedLinkProperty()
	{
		String link = "http://www.kulturarvsdata.se";
		return link;
	}
	
	/**
	 * Böna (typ) som håller data om en RSS entitet
	 * @author Henrik Hjalmarsson
	 */
	public class RssObject
	{
		private String title;
		private String link;
		private String description;
		private String thumbnailUrl;
		private String imageUrl;
		private List<String> keyWords;
		private String publishDate;
		private String coords;
		
		public RssObject()
		{
			keyWords = new Vector<String>();
		}
		
		/**
		 * returnerar listan med nyckelord som en array
		 * @return
		 */
		public String[] getKeywordsAsArray()
		{
			String[] result = new String[keyWords.size()];
			for(int i = 0; i < keyWords.size(); i++) {
				result[i] = keyWords.get(i);
			}
			return result;
		}

		/**
		 * Lägger till ett nyckelord till nyckelordslistan
		 * @param keyWord
		 */
		public void addKeyWord(String keyWord)
		{
			keyWords.add(keyWord);
		}

		public String getPublishDate()
		{
			return publishDate;
		}

		public void setPublishDate(String publishDate)
		{
			this.publishDate = publishDate;
		}

		public void setThumbnailUrl(String url)
		{
			thumbnailUrl = url;
		}
		
		public String getThumbnailUrl()
		{
			return thumbnailUrl;
		}
		
		public void setImageUrl(String url)
		{
			imageUrl = url;
		}
		
		public String getImageUrl()
		{
			return imageUrl;
		}
		
		public void setTitle(String title)
		{
			this.title = title;
		}
		
		public String getTitle()
		{
			return title;
		}
		
		public void setLink(String link)
		{
			this.link = link;
		}
		
		public String getLink()
		{
			return link;
		}
		
		public void setDescription(String description)
		{
			this.description = description;
		}
		
		public String getDescription()
		{
			return description;
		}

		public void setCoords(String coords)
		{
			this.coords = coords;
		}

		public String getCoords()
		{
			return coords;
		}
	}
}
