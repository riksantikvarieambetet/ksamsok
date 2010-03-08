package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.RssObject;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.XMLHandler;
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
public class RSS extends Search
{
	/** metodens namn */
	public static final String METHOD_NAME = "rss";
	
	//RSS feed typ
	private static final String RSS_2_0 = "rss_2.0";
	private static final SAXParserFactory spf = SAXParserFactory.newInstance();
	
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
		super(queryString, hitsPerPage, startRecord, writer);
	}

	@Override
	public void performMethod() throws BadParameterException,
			DiagnosticException
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try
		{
			doSearch(searcher);
		} catch (IOException e)
		{
			
		} catch (ParserConfigurationException e)
		{
			throw new DiagnosticException("Oväntat Parser fel uppstod", "se.raa.ksamsok.api.method.RSS.performMethod()", e.getMessage(), true);
		} catch (SAXException e)
		{
			throw new DiagnosticException("Oväntat SAX parser fel", "se.raa.ksamsok.api.method.RSS.performMethod()", e.getMessage(), true);
		} catch (FeedException e)
		{
			throw new DiagnosticException("Oväntat RSS feed fel uppstod", "se.raa.ksamsok.api.method.RSS.performMethod()", e.getMessage(), true);
		}finally
		{
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
		throws DiagnosticException, BadParameterException, IOException, 
			ParserConfigurationException, SAXException, FeedException
	{
		Query q = createQuery();
		SyndFeed feed = getFeed();
		final MapFieldSelector fieldSelector = getFieldSelector();
		TopDocs hits = null;
		int numberOfDocs = 0;
		int nDocs = startRecord - 1 + hitsPerPage;
		hits = searcher.search(q, nDocs == 0 ? 1 : nDocs);
		numberOfDocs = hits.totalHits;
		feed.setEntries(getEntries(numberOfDocs, nDocs, searcher, hits, fieldSelector));
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed, writer);
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
		throws CorruptIndexException, IOException, 
			ParserConfigurationException, SAXException, 
			DiagnosticException
	{
		List<SyndEntry> entries = new Vector<SyndEntry>();
		for(int i = startRecord - 1;i < numberOfDocs && i < nDocs; i++)
		{
			Document doc = searcher.doc(hits.scoreDocs[i].doc,
					fieldSelector);
			String content = null;
			//Hämtar bara XML datan TODO ska man hämta hela RDF?
			byte[] pres = doc.getBinaryValue(ContentHelper.I_IX_PRES);
			if (pres != null) 
			{
				content = new String(pres, "UTF-8");
			}else 
			{
				content = null;
			}
			entries.add(getEntry(content));
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
		throws ParserConfigurationException, SAXException, IOException, 
			DiagnosticException
	{
		SAXParser parser = spf.newSAXParser();
		RssObject data = new RssObject();
		XMLHandler handler = new XMLHandler(data);
		parser.parse(new InputSource(new StringReader(content)), handler);
		SyndEntry entry = new SyndEntryImpl();
		data = handler.getData();
		entry.setTitle(data.getTitle());
		entry.setLink(data.getLink());
		SyndContent syndContent = new SyndContentImpl();
		syndContent.setType("text/plain");
		syndContent.setValue(data.getDescription());
		entry.setDescription(syndContent);
		String thumb = data.getThumbnailUrl();
		String image = data.getImageUrl();
		if (!StringUtils.isEmpty(thumb) && !StringUtils.isEmpty(image)) 
		{
			entry.getModules().add(getMediaModule(thumb, image));
		}
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
			mediaModule.setMetadata(getThumbnail(thumb, mediaModule));
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
	 */
	protected Metadata getThumbnail(String thumb, 
			MediaEntryModule mediaModule) 
		throws URISyntaxException
	{
		Thumbnail thumbnail = new Thumbnail(new URI(thumb));
		Thumbnail[] thumbnails = new Thumbnail[1];
		thumbnails[0] = thumbnail;
		Metadata metadata = mediaModule.getMetadata();
		if (metadata == null) 
		{
			metadata = new Metadata();
		}
		metadata.setThumbnail(thumbnails);
		return metadata;
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
}
