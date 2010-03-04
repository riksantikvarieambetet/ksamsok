package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

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
	
	protected void doSearchHelp(int i, IndexSearcher searcher, List<String> termList) 
		throws BooleanQuery.TooManyClauses, IOException
	{
		String index = indexList.get(i);
		index = CQL2Lucene.translateIndexName(index);
		Term term = new Term(index, prefix);
		Query query = new WildcardQuery(term);
		query = searcher.rewrite(query);
		Set<Term> termSet = new HashSet<Term>();
		query.extractTerms(termSet);
		int counter = 1;
		for(Term t : termSet) {
			if(counter > maxValueCount) {
				break;
			}
			termList.add(t.text());
			counter++;
		}
	}
	
	@Override
	public void performMethod()
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try {
			List<String> termList = new ArrayList<String>();
			for(int i = 0; i < indexList.size(); i++) {
				try {
					doSearchHelp(i, searcher, termList);
				}catch(BooleanQuery.TooManyClauses e) {
					continue;
				}
			}
			writeHead(termList);
			writeResult(termList);
			writeFot();
		} catch (IOException e) {
			// TODO fix
			e.printStackTrace();
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	/**
	 * skriver ut början av svaret
	 * @param termList
	 */
	protected void writeHead(List<String> termList)
	{
		StartEndWriter.writeStart(writer);
		StartEndWriter.hasHead(true);
		writer.println("<numberOfTerms>" + termList.size() + "</numberOfTerms>");
		writer.println("<terms>");
	}
	
	/**
	 * skriver ut resultatet av svaret
	 * @param termList
	 */
	protected void writeResult(List<String> termList)
	{
		for(int i = 0; i < termList.size(); i++)
		{
			writer.println("<term>");
			writer.println("<value>" + StaticMethods.xmlEscape(termList.get(i)) + "</value>");
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
