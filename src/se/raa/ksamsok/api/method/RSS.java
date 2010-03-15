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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.harvest.HarvestRepositoryManager;
import se.raa.ksamsok.harvest.HarvesterServlet;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

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
public class RSS extends DefaultHandler implements APIMethod
{
	// publika värden
	public static final String METHOD_NAME = "rss";
	public static final String QUERY = "query";
	public static final String HITS_PER_PAGE = "hitsPerPage";
	public static final String START_RECORD = "startRecord";
	public static final int DEFAULT_HITS_PER_PAGE = 100;
	public static final int DEFAULT_START_RECORD = 1;
	
	// privata statiska variabler
	private static final String RSS_2_0 = "rss_2.0";
	private static final SAXParserFactory spf = SAXParserFactory.newInstance();
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", new Locale("sv",	"SE"));
	
	//privata variabler
	private String queryString;
	private int hitsPerPage;
	private int startRecord;
	private PrintWriter writer;
	private StringBuffer tempValue;
	private RssObject data;
	private boolean store;
	private String imageType;
	
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
			final MapFieldSelector fieldSelector = getFieldSelector();
			int nDocs = startRecord - 1 + hitsPerPage;
			TopDocs hits = searcher.search(q, nDocs == 0 ? 1 : nDocs);
			int numberOfDocs = hits.totalHits;
			doSyndFeed(numberOfDocs, nDocs, searcher, hits, fieldSelector);
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
	 * @param fieldSelector
	 * @throws DiagnosticException
	 */
	private void doSyndFeed(int numberOfDocs, int nDocs, 
			IndexSearcher searcher, TopDocs hits, 
			MapFieldSelector fieldSelector) 
		throws DiagnosticException 
	{
		try {
			SyndFeed feed = getFeed();
			feed.setEntries(getEntries(numberOfDocs, nDocs, searcher, hits, fieldSelector));
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
	 * @param fieldSelector
	 * @param entries
	 * @return
	 * @throws CorruptIndexException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws DiagnosticException 
	 */
	protected List<SyndEntry> getEntries(int numberOfDocs, int nDocs, 
			IndexSearcher searcher, TopDocs hits, 
			MapFieldSelector fieldSelector) 
		throws DiagnosticException
	{
		List<SyndEntry> entries = new Vector<SyndEntry>();
		HarvestRepositoryManager hrm = HarvesterServlet.getInstance().getHarvestRepositoryManager();
		try {
			for(int i = startRecord - 1;i < numberOfDocs && i < nDocs; i++) {
				Document doc = searcher.doc(hits.scoreDocs[i].doc,
						fieldSelector);
				String uri = doc.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				String content = null;
				content = hrm.getXMLData(uri);
				entries.add(getEntry(content));
			}
		}catch (ParserConfigurationException e) {
			throw new DiagnosticException("Parser fel", "RSS.getEntries", e.getMessage(), true);
		} catch (SAXException e) {
			throw new DiagnosticException("SAX fel", "RSS.getEntries", e.getMessage(), true);
		} catch (CorruptIndexException e) {
			throw new DiagnosticException("Nånting gick fel : /", "RSS.getEntries", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel", "RSS.getEntries", e.getMessage(), true);
		} catch(Exception e) {
			throw new DiagnosticException("Fel vid hämtning av data från databasen", "RSS.getEntries", e.getMessage(), true);
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
			SAXParser parser = spf.newSAXParser();
			data = new RssObject();
			parser.parse(new InputSource(new StringReader(content)), this);
			entry.setTitle(data.getTitle());
			entry.setLink(data.getLink());
			SyndContent syndContent = new SyndContentImpl();
			syndContent.setType("text/plain");
			syndContent.setValue(data.getDescription());
			entry.setDescription(syndContent);
			String thumb = data.getThumbnailUrl();
			String image = data.getImageUrl();
			if (!StringUtils.isEmpty(thumb) && !StringUtils.isEmpty(image)) {
				entry.getModules().add(getMediaModule(thumb, image));
			}
			entry.setPublishedDate(sdf.parse(data.getPublishDate()));
		} catch (ParserConfigurationException e) {
			throw new DiagnosticException("Parser fel", "RSS.getEntry", e.getMessage(), true);
		} catch (SAXException e) {
			throw new DiagnosticException("SAX fel", "RSS.getEntry", e.getMessage(), true);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel", "RSS.getEntry", e.getMessage(), true);
		} catch (ParseException e) {
			throw new DiagnosticException("Parser fel", "RSS.getEntry", e.getMessage(), true);
		} catch(Exception ignore) {}
		return entry;
	}
	
	/**
	 * skapar en MapFieldSelector
	 * @return ett objekt av MapFieldSelector
	 */
	protected MapFieldSelector getFieldSelector()
	{
		final String[] fieldNames = 
		{
			ContentHelper.CONTEXT_SET_REC + "." +
			ContentHelper.IX_REC_IDENTIFIER,
			ContentHelper.I_IX_PRES, 
			ContentHelper.I_IX_LON, 
			ContentHelper.I_IX_LAT
		};
		
		return new MapFieldSelector(fieldNames);
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
	protected MediaEntryModule getMediaModule(String thumbnailUrl, 
			String imageUrl) 
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
	protected Metadata getMetadata(String thumb, 
			MediaEntryModule mediaModule) 
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
	
	public void endElement(String uri, String localName, String qName)
		throws SAXException
	{
		if(store)
		{
			if(qName.equalsIgnoreCase("pres:representation"))
			{
				data.setLink(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:itemLabel"))
			{
				data.setTitle(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:description"))
			{
				data.setDescription(tempValue.toString());
			}else if(qName.equalsIgnoreCase("pres:src"))
			{	
				if(imageType.equalsIgnoreCase("thumbnail"))
				{
					data.setThumbnailUrl(tempValue.toString());
				}else if(imageType.equalsIgnoreCase("lowres"))
				{
					data.setImageUrl(tempValue.toString());
				}
			}else if(qName.equalsIgnoreCase("ns5:itemKeyWord")) {
				data.addKeyWord(tempValue.toString());
			}else if(qName.equalsIgnoreCase("ns5:buildDate")) {
				data.setPublishDate(tempValue.toString());
			}
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException
	{
		//reset
		tempValue = new StringBuffer();
		store = false;
		imageType = "";
		if(qName.equalsIgnoreCase("pres:representation")) {
			if(attributes.getValue("format") != null && attributes.getValue("format").equalsIgnoreCase("HTML")) {
				store = true;
			}
		}else if(qName.equalsIgnoreCase("pres:description")) {
			store = true;
		}else if(qName.equalsIgnoreCase("pres:itemLabel")) {
			store = true;
		}else if(qName.equalsIgnoreCase("pres:src") && (attributes.getValue("type").equalsIgnoreCase("thumbnail") || attributes.getValue("type").equalsIgnoreCase("lowres"))) {
			store = true;
			imageType = attributes.getValue("type");
		}else if(qName.equalsIgnoreCase("ns5:itemKeyWord")) {
			store = true;
		}else if(qName.equalsIgnoreCase("ns5:buildDate")) {
			store = true;
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
		throws SAXException
	{
		tempValue.append(new String(ch, start, length));
	}
	
	public class RssObject
	{
		private String title;
		private String link;
		private String description;
		private String thumbnailUrl;
		private String imageUrl;
		private List<String> keyWords;
		private String publishDate;
		
		public RssObject()
		{
			keyWords = new Vector<String>();
		}
		
		public String[] getKeywordsAsArray()
		{
			String[] result = new String[keyWords.size()];
			for(int i = 0; i < keyWords.size(); i++) {
				result[i] = keyWords.get(i);
			}
			return result;
		}

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
	}
}
