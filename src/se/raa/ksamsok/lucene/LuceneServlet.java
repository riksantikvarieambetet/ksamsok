package se.raa.ksamsok.lucene;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;

/**
 * LuceneServlet, hanterar gränssnitt mot lucene
 *
 */
public class LuceneServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(LuceneServlet.class);

	// synkobjekt för att begränsa åtkomst map IndexWriter
	public static final Object IW_SYNC = new Object();
	private static final int WRITER_CLOSE_WAIT = 15;

	// konstanter som kan påverka lucene-prestanda, se lucene-dokumentation
	private static final int MERGE_FACTOR = 20; // default är 10
	private static final double RAM_BUFFER_SIZE_MB = 48; // default är 16
	private static final int TERM_INDEX_INTERVAL = 256; // default är 128

	/**
	 * Systemparameter för katalog för lucene-index, om ej satt används /var/lucene-index/ksamsok.
	 */
	public static final String D_LUCENE_INDEX_DIR = "samsok-lucene-index-dir";

	/**
	 * Namn på datakällan som ska vara konfigurerad i servlet-containern, jdbc/[namn].
	 */
	static final String DATASOURCE_NAME = "harvestdb";

	private static final String LUCENE_DEFAULT = "/var/lucene-index/ksamsok";

	protected Directory indexDir;
	protected IndexSearcher is = null;
	protected IndexWriter iw = null;
	protected Map<IndexSearcher, Long> searchers = new HashMap<IndexSearcher, Long>();
	protected boolean iwBorrowed = false;
	protected boolean isDestroying = false;
	private static LuceneServlet instance;

	/**
	 * Hämtar körande instans.
	 * @return aktuell instans
	 */
	public static LuceneServlet getInstance() {
		if (instance == null) {
			throw new RuntimeException("LuceneServlet har inte initialiserats korrekt");
		}
		return instance;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		if (logger.isInfoEnabled()) {
			logger.info("Startar LuceneServlet");
		}
		// åsterställ statusvariabler utifall att denna instans återanvänds av
		// servletcontainern
		isDestroying = false;
		iwBorrowed = false;
		try {
			// förskö hämta katalogvärde från systemproperties, ta LUCENE_DEFAULT annars
			String dir = System.getProperty(D_LUCENE_INDEX_DIR, LUCENE_DEFAULT);
			File fDir = new File(dir);
			if (!fDir.exists() || !fDir.isDirectory() || !fDir.canWrite()) {
				throw new ServletException("Problem med tilldelad katalog för lucene-index: " + dir +
						", kontrollera skrivrättigheter och att katalogen finns");
			}
			// TODO: NIOFSDirectory tydligen långsam/trasig på win pga en sun-bug
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6265734 
			// https://issues.apache.org/jira/browse/LUCENE-753
			indexDir = NIOFSDirectory.getDirectory(fDir);

			boolean createIndex = false;
			if (!IndexReader.indexExists(indexDir)) {
				if (logger.isInfoEnabled()) {
					logger.info("Inget index hittat i " + fDir + ", skapar nytt");
				}
				createIndex = true;
			} else if (IndexWriter.isLocked(indexDir)) {
				logger.warn("lucene-indexet var låst vilket det inte borde varit, låser upp det för skrivning");
				IndexWriter.unlock(indexDir);
			}
			iw = createIndexWriter(indexDir, createIndex);
			is = new IndexSearcher(IndexReader.open(indexDir, true));
			synchronized (searchers) {
				// stoppa in is så att den inte behöver specialbehandlas i destroy
				searchers.put(is, Long.valueOf(0));
			}
			instance = this;
		} catch (Throwable t) {
			logger.error("Fel vid init av lucene-index", t);
			throw new UnavailableException("Fel vid init av lucene-index");
		}
		if (logger.isInfoEnabled()) {
			logger.info("LuceneServlet startad");
		}
	}

	@Override
	public void destroy() {
		isDestroying = true;
		if (logger.isInfoEnabled()) {
			logger.info("Stoppar LuceneServlet");
		}
		IndexReader r = null;
		synchronized (searchers) {
			for (IndexSearcher searcher: searchers.keySet()) {
				// måste spara en referens då is.close() inte stänger en inskickad reader
				// vilket vi gör i init() för att kunna ange readOnly på den
				r = searcher.getIndexReader();
				try {
					searcher.close();
				} catch (IOException e) {
					log("Fel vid nedstängning av lucene-index-searcher", e);
				}
				try {
					r.close();
				} catch (IOException e) {
					log("Fel vid nedstängning av lucene-index-reader", e);
				}
			}
		}
		if (iw != null) {
			if (iwBorrowed) {
				if (logger.isInfoEnabled()) {
					logger.info("En writer är utlånad, väntar " + WRITER_CLOSE_WAIT + " sek på att den ska lämnas tillbaka");
				}
				try {
					Thread.sleep(WRITER_CLOSE_WAIT * 1000);
				} catch (Exception ignore) {
				}
				if (iwBorrowed) {
					logger.warn("Writer fortfarande utlånad efter väntan, gör rollback och stänger den ändå");
				}
				try {
					iw.rollback();
				} catch (Exception e) {
					logger.error("Fel vid rollback för iw vid destroy", e);
				}
			}
			try {
				iw.close();
			} catch (Exception e) {
				logger.error("Fel vid stängning av iw vid destroy", e);
			}
		}
		if (indexDir != null) {
			try {
				indexDir.close();
			} catch (IOException e) {
				log("Fel vid nedstängning av lucene-index-dir", e);
			}
		}
		super.destroy();
		if (logger.isInfoEnabled()) {
			logger.info("LuceneServlet stoppad");
		}
	}

	/**
	 * Lånar en IndexSearcher för sökning i indexet, måste lämnas tillbaka.
	 * 
	 * @return en IndexSearcher
	 */
	public IndexSearcher borrowIndexSearcher() {
		if (isDestroying) {
			throw new RuntimeException("Applikationen håller på att stänga ner");
		}
		IndexSearcher ret = null;
		synchronized(searchers) {
			ret = is;
			Long c = searchers.get(ret);
			if (c == null) {
				c = Long.valueOf(1);
			} else {
				c = Long.valueOf(c.longValue() + 1);
			}
			searchers.put(ret, c);
		}
		return ret;
	}
	
	/**
	 * Lämnar tillbaka en IndexSearcher.
	 * 
	 * @param ret IndexSearcher
	 */
	public void returnIndexSearcher(IndexSearcher ret) {
		// TODO: hantera gamla indexers som kanske inte lämnas tillbaka bättre
		//       så att mängden indexers inte bara växer, lägg till timestamp?
		if (isDestroying) {
			// allt stängs i destroy
			return;
		}
		if (ret == null) {
			return;
		}
		synchronized (searchers) {
			Long c = searchers.get(ret);
			if (c == null) {
				logger.error("Fick tillbaka en is som inte använts?: " + ret);
			} else {
				c = Long.valueOf(c.longValue() - 1);
				if (ret != is && c.longValue() == 0) {
					// om det är en gammal instans som kommer tillbaka så stänger vi den
					searchers.remove(ret);
					IndexReader ir = ret.getIndexReader();
					try {
						ret.close();
						ir.close();
					} catch (IOException e) {
						logger.error("Fel vid stängning av is och ir vid livscykelavslut", e);
					}
					logger.debug("Stängde en gammal is (och ir): " + ret);
				} else {
					if (c.longValue() < 0) {
						// varna och försök "fixa"
						logger.warn("IndexSearcher har lämnats tillbaka mer gånger än den lånats ut");
						c = Long.valueOf(0);
					}
					searchers.put(ret, c);
				}

			}
		}
	}

	/**
	 * Lånar en IndexWriter för skrivning i lucene-indexet, måste lämnas tillbaka.
	 * 
	 * @return en IndexWriter
	 */
	public IndexWriter borrowIndexWriter() {
		if (isDestroying) {
			throw new RuntimeException("Applikationen håller på att stänga ner");
		}
		synchronized (IW_SYNC) {
			if (iwBorrowed) {
				throw new RuntimeException("Bara en iw i taget");
			}
			iwBorrowed = true;
		}
		return iw;
	}

	/**
	 * Lämnar tillbaka en utlånad IndexWriter.
	 * 
	 * @param ret IndexWriter
	 * @param refresh sant om data har skrivits och indexläsarna ska uppdateras
	 */
	public void returnIndexWriter(IndexWriter ret, boolean refresh) {
		if (ret == null) {
			return;
		}
		synchronized (IW_SYNC) {
			if (!iwBorrowed) {
				logger.warn("Fel iw tillbaka: " + ret + ", ingen var utlånad...");
			}
			if (iw != ret) {
				logger.error("Fel iw tillbaka: " + ret + ", borde varit: " + iw);
			} else {
				iwBorrowed = false;
			}
			// försök avgöra om iw:n har blivit stängd pga rollback
			if (!isDestroying) {
				try {
					iw.getTermIndexInterval();
				} catch (Exception e) {
					if (logger.isInfoEnabled()) {
						logger.info("iw stängd (pga rollback(?)), skapar ny");
					}
					try {
						iw = createIndexWriter(indexDir, false);
					} catch (Exception e2) {
						logger.error("Fel vid skapande av ny iw", e2);
					}
				}
			}
		}
		if (!isDestroying && refresh) {
			synchronized (searchers) {
				try {
					IndexReader old = is.getIndexReader();
					IndexReader reopened = old.reopen();
					if (reopened != old) {
						// indexet har uppdaterats och en ny searcher/reader måste skapas
						Long c = searchers.get(is);
						if (c == null || c.longValue() == 0) {
							// om ej utlånad, stäng och ta bort
							searchers.remove(is);
							is.close();
							old.close();
						}
						is = new IndexSearcher(reopened);
						if (logger.isInfoEnabled()) {
							logger.info("lucene-index har uppdaterats");
						}
					}
				} catch (Exception e) {
					logger.error("Fel vid refresh av ir och is", e);
				}
			}
		}
	}

	/**
	 * Optimerar lucene-indexet.
	 * 
	 * @throws Exception
	 */
	public void optimizeLuceneIndex() throws Exception {
		IndexWriter iw = null;
		boolean refreshIndex = false;
		synchronized (IW_SYNC) { // en i taget som får köra index-write
			try {
				iw = borrowIndexWriter();
				if (logger.isInfoEnabled()) {
					logger.info("Optimize av lucene-index, start");
				}
				iw.optimize();
				if (logger.isInfoEnabled()) {
					logger.info("Optimize av lucene-index, klart");
				}
				iw.commit();
				refreshIndex = true;
			} catch (Throwable e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid rollback för lucene-index", e2);
					}
				}
				logger.error("Fel vid optimize av lucene-index", e);
				throw new Exception(e);
			} finally {
				returnIndexWriter(iw, refreshIndex);
			}
		}
	}

	/**
	 * Rensar lucene-indexet - OBS mycket bättre att stoppa tomcat och rensa indexkatalogen.
	 * 
	 * @throws Exception
	 */
	public void clearLuceneIndex() throws Exception {
		IndexWriter iw = null;
		boolean refreshIndex = false;
		synchronized (IW_SYNC) { // en i taget som får köra index-write
			try {
				iw = borrowIndexWriter();
				if (logger.isInfoEnabled()) {
					logger.info("Rensning av lucene-index, start");
				}
				// alla dokument borde ha ett serviceId-index
				iw.deleteDocuments(new WildcardQuery(new Term(ContentHelper.I_IX_SERVICE, "*")));
				if (logger.isInfoEnabled()) {
					logger.info("Rensning av lucene-index, klart");
				}
				iw.commit();
				refreshIndex = true;
			} catch (Throwable e) {
				if (iw != null) {
					try {
						iw.rollback();
					} catch (Exception e2) {
						logger.warn("Fel vid rollback för lucene-index", e2);
					}
				}
				logger.error("Fel vid optimize av lucene-index", e);
				throw new Exception(e);
			} finally {
				returnIndexWriter(iw, refreshIndex);
			}
		}
	}

	/**
	 * Ger totala antalet indexerade poster i lucene-indexet.
	 * 
	 * @return antal indexerade poster
	 */
	public int getTotalCount() {
		IndexSearcher s = null;
		int poster = 0;
		try {
			s = borrowIndexSearcher();
			poster = s.getIndexReader().numDocs();
		} finally {
			returnIndexSearcher(s);
		}
		return poster;
	}

	/**
	 * Ger antalet indexerade poster för angiven tjänst.
	 * 
	 * @param serviceId id för tjänst
	 * @return antal indexerade poster
	 */
	public int getCount(String serviceId) {
		IndexSearcher s = null;
		int poster = -1;
		try {
			s = borrowIndexSearcher();
			poster = s.search(new TermQuery(new Term(ContentHelper.I_IX_SERVICE, serviceId)), 1).totalHits;
		} catch (Exception e) {
			logger.error("Fel vid hämtning av antal indexerade poster för tjänst " + serviceId);
		} finally {
			returnIndexSearcher(s);
		}
		return poster;
	}

	private static IndexWriter createIndexWriter(Directory indexDir, boolean create) throws Exception {
		IndexWriter iw = new IndexWriter(indexDir, ContentHelper.getSwedishAnalyzer(), create, IndexWriter.MaxFieldLength.UNLIMITED);
		iw.setRAMBufferSizeMB(RAM_BUFFER_SIZE_MB);
		iw.setMergeFactor(MERGE_FACTOR);
		iw.setTermIndexInterval(TERM_INDEX_INTERVAL);
		return iw;
	}
}
