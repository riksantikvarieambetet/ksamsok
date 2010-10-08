package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;

import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * Utför en prefix sökning för att ge förslag på fortsättningar av ett givet query
 * @author Henrik Hjalmarsson
 */
public class SearchHelp implements APIMethod
{
	private String prefix;
	private int maxValueCount;
	private PrintWriter writer;
	private List<String> indexList;
	private static final Logger logg = Logger.getLogger(SearchHelp.class);
	
	/** metodens namn */
	public static final String METHOD_NAME = "searchHelp";
	/** parameter namn för prefix */
	public static final String PREFIX_PARAMETER = "prefix";
	/** parameter namn för hur många förslag som önskas */
	public static final String MAX_VALUE_COUNT_PARAMETER = "maxValueCount";
	/** parameter namn för index */
	public static final String INDEX_PARAMETER = "index";
	/** default värde för max value count */
	public static final int DEFAULT_MAX_VALUE_COUNT = 3;
	
	private List<SortableContainer> sortedList;
	
	/**
	 * skapar ett objekt av SearchHelp
	 * @param writer
	 * @param indexList
	 * @param prefix
	 * @param maxValueCount
	 */
	public SearchHelp(PrintWriter writer, List<String> indexList, String prefix,
			int maxValueCount)
	{	
		this.prefix = prefix;
		this.maxValueCount = maxValueCount;
		this.writer = writer;
		this.indexList = indexList;
	}
	
	protected void doSearchHelp(int i, IndexSearcher searcher) 
		throws BooleanQuery.TooManyClauses, IOException
	{
		String index = indexList.get(i);
		index = CQL2Lucene.translateIndexName(index);
		Term term = new Term(index, prefix);
		Query query = new WildcardQuery(term);
		query = searcher.rewrite(query);
		Set<Term> termSet = new HashSet<Term>();
		query.extractTerms(termSet);
		
		sortedList = sort(termSet, searcher);
	}
	
	/**
	 * Sorts the term list depending on frequency of the term
	 * @param termSet
	 * @throws IOException 
	 */
	private List<SortableContainer> sort(Set<Term> termSet, IndexSearcher searcher) throws IOException {
		List<SortableContainer> sortableList = new ArrayList<SortableContainer>();
		for(Term t : termSet){
			SortableContainer s = new SortableContainer();
			s.term = t;
			s.frequency = getTermHitCount(searcher, t);
			sortableList.add(s);
		}
		
		Collections.sort(sortableList);

		
		return sortableList;
	}
	
	/**
	 * Inner class containing a term and its frequency
	 * Maybe this class shouldnt be an inner class, but
	 * it is for now
	 * @author Martin Duveborg
	 */
	private class SortableContainer implements Comparable<SortableContainer>{
		public Integer frequency;
		public Term term;
		public int compareTo(SortableContainer s){
			if(frequency == null) return -1;							
			// -1 makes it descend								
			return frequency.compareTo(s.frequency) * -1;
		}
	}

	/**
	 * @param term the term searched for. For example "text:stockholm"
	 * @return returns total hits for this term
	 */
	private int getTermHitCount(IndexSearcher searcher, Term term) throws IOException{
		TopDocs topDocs = searcher.search(new TermQuery(term), 1);
		return topDocs.totalHits;
	}
	
	
	
	@Override
	public void performMethod()
		throws DiagnosticException
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try {
			for(int i = 0; i < indexList.size(); i++) {
				try {
					doSearchHelp(i, searcher);
				}catch(BooleanQuery.TooManyClauses e) {
					continue;
				}
			}
			writeHead();
			writeResult();
			writeFot();
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod", "SearchHelp.performMethod", e.getMessage(), true);
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	/**
	 * skriver ut början av svaret
	 */
	protected void writeHead()
	{
		StartEndWriter.writeStart(writer);
		StartEndWriter.hasHead(true);
		writer.println("<numberOfTerms>" + sortedList.size() + "</numberOfTerms>");
		writer.println("<terms>");
	}
	
	/**
	 * skriver ut resultatet av svaret
	 */
	protected void writeResult()
	{
		for(int i = 0; i < sortedList.size(); i++)
		{
			writer.println("<term>");
			writer.println("<value>" + StaticMethods.xmlEscape(sortedList.get(i).term.text()) + "</value>");
			writer.println("<count>" + sortedList.get(i).frequency + "</count>");
			writer.println("</term>");
		}
	}
	
	/**
	 * skriver ut foten av svaret
	 */
	protected void writeFot()
	{
		writer.println("</terms>");
		writer.println("<echo>");
		writer.println("<method>" + SearchHelp.METHOD_NAME + "</method>");
		for(int i = 0; i < indexList.size(); i++)
		{
			writer.println("<index>" + indexList.get(i) + "</index>");
		}
		writer.println("<maxValueCount>" + maxValueCount  + "</maxValueCount>");
		writer.println("<prefix>" + StaticMethods.xmlEscape(prefix) + "</prefix>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}
}
