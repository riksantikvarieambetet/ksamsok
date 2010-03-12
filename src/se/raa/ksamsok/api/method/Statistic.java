package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.exception.MissingParameterException;
import se.raa.ksamsok.api.util.QueryContent;
import se.raa.ksamsok.api.util.StartEndWriter;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.api.util.parser.CQL2Lucene;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.lucene.LuceneServlet;

/**
 * söka statistik
 * @author Henrik Hjalmarsson
 */
public class Statistic implements APIMethod
{
	/** namnet på metoden */
	public static final String METHOD_NAME = "statistic";
	/** namnet på parametern som håller medskickade index */
	public static final String INDEX_PARAMETER = "index";
	/** namn på parameter för att ta bort nollor i svars XML */
	public static final String REMOVE_BELOW = "removeBelow";
	/** max antal kombinationer av indexvärden */
	protected static final int MAX_CARTESIAN_COUNT = 20000;
	
	//set med index som skall kollas
	protected Map<String,String> indexMap;
	//writer som används för att skriva ut svaren
	protected PrintWriter writer;
	protected int removeBelow = 0;
	
	/**
	 * Skapar ett nytt statistic objekt
	 * @param indexes de index som skall scannas
	 * @param writer används för att skriva ut svaret
	 */
	public Statistic(Map<String,String> indexMap, PrintWriter writer)
	{
		this.indexMap = indexMap;
		this.writer = writer;
	}
	
	/**
	 * anger om nollor skall tas bort i svars XML
	 * @param p
	 */
	public void setRemoveBelow(int i)
	{
		removeBelow = i;
	}
	
	@Override
	public void performMethod()
		throws BadParameterException, DiagnosticException,
			MissingParameterException
	{
		IndexSearcher searcher = null;
		Map<String, Set<Term>> termMap = null;
		try {
			searcher = LuceneServlet.getInstance().borrowIndexSearcher();
			//en mängd med mängder med mängder!
			termMap = buildTermMap(searcher);
			if(getCartesianCount(termMap)  > MAX_CARTESIAN_COUNT) {
				throw new BadParameterException("den kartesiska produkten av inskickade index blir för stor för att utföra denna operation.", "Statistic.performMethod", null, false);
			}
			//gör en kartesisk produkt på de värden i termMap
			List<QueryContent> queryResults = cartesian(termMap);
			//utför själva sökningen
			doStatistic(searcher, queryResults);
			writeHead(queryResults);
			writeResult(queryResults);
			writeFot();
		}catch(OutOfMemoryError e) {
			throw new BadParameterException("de inskickade index värdena gav upphov till att för många värden hittades och denna sökning gick ej att utföra", "Statistic.performMethod", null, false);
		}finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}
	
	/**
	 * bygger en kartesisk produkt av x antal mängder med n antal element
	 * @param data som skall göra kartesisk produkt av
	 * @return Kartesisk produkt av indata som en lista
	 * @throws MissingParameterException
	 */
	protected static List<QueryContent> cartesian(Map<String,Set<Term>> data)
		throws MissingParameterException
	{
		String index1 = null;
		String index2 = null;
		List<QueryContent> result = null;
		//lite special cases ifall den bara gick 0 eller 1 varv
		for(String index : data.keySet()) {
			if(index1 == null) {//körs första varvet
				index1 = index;
				continue;
			}else if(index2 == null) {//körs andra varvet
				index2 = index;
				result = cartesian(index1, index2, data.get(index1),
						data.get(index2));
				continue;
			}else {//körs resten av varven
				index1 = index;
				result = cartesian(index1, data.get(index1), result);
			}		
		}
		if(index1 == null && index2 == null) {
			throw new MissingParameterException("minst ett index behövs för denna operation", "Statistic.cartesian", null, false);
		}else if(index1 != null && index2 == null) {
			result = cartesianWithOneIndex(data, index1);
		}
		return result;
	}
	
	/**
	 * körs om endast ett index finns
	 * @param data
	 * @param index1
	 * @return
	 */
	private static List<QueryContent> cartesianWithOneIndex(
			Map<String,Set<Term>> data, String index1)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(Term term : data.get(index1))
		{
			QueryContent content = new QueryContent();
			content.addTerm(index1, term.text());
			result.add(content);
		}
		return result;
	}
	
	/**
	 * Kollar hur stor den kartesiska produkten kommer bli
	 * @param data
	 * @return
	 */
	protected int getCartesianCount(Map<String,Set<Term>> data)
	{
		int count = 1;
		for(String index : data.keySet())
		{
			count *= data.get(index).size();
		}
		return count;
	}
	
	/**
	 * Bygger kartesisk produkt av värden i given lista och värden i givet set
	 * @param index för tillhörande set
	 * @param set med värden
	 * @param list med värden
	 * @return ny lista med kartesisk produkt av indata
	 */
	private static List<QueryContent> cartesian(String index, Set<Term> set,
			List<QueryContent> list)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(int i = 0; i < list.size(); i++)
		{
			for(Term term : set)
			{
				Map<String,String> map = list.get(i).getTermMap();
				QueryContent content = new QueryContent();
				for(String index2 : map.keySet())
				{	
					content.addTerm(index2, map.get(index2));
				}
				content.addTerm(index, term.text());
				result.add(content);
			}
		}
		return result;
	}
	
	/**
	 * bygger kartesisk produkt av de två givna setten
	 * @param index1 tillhörande set1
	 * @param index2 tillhörande set2
	 * @param set1 med värden
	 * @param set2 med värden
	 * @return lista med kartesisk produkt av de båda setten
	 */
	private static List<QueryContent> cartesian(String index1, String index2,
			Set<Term> set1, Set<Term> set2)
	{
		List<QueryContent> result = new ArrayList<QueryContent>();
		for(Term term1 : set1)
		{
			for(Term term2 : set2)
			{
				QueryContent content = new QueryContent();
				content.addTerm(index1, term1.text());
				content.addTerm(index2, term2.text());
				result.add(content);
			}
		}
		return result;
	}
	
	/**
	 * bygger en term map av den inskickade mappen
	 * @param searcher som används för att söka i index
	 * @param indexMap med index och sökvärden
	 * @return Map<String,Set<Term>> med index och dess termer
	 * @throws BadParameterException 
	 */
	protected Map<String, Set<Term>> buildTermMap(IndexSearcher searcher)
		throws DiagnosticException, BadParameterException
	{
		Query query;
		String indexValue;
		HashMap<String, Set<Term>> termMap = new HashMap<String, Set<Term>>();
		// TODO: den här ska sättas en gång per jvm och ska sättas innan lucene-klasserna används,
		//       helst kanske via -D-parameter
		BooleanQuery.setMaxClauseCount(10000);

		for(String index : indexMap.keySet()) {
			try {
				indexValue = CQL2Lucene.translateIndexName(index);
				if(!ContentHelper.indexExists(indexValue)) {
					throw new BadParameterException("Indexet " + index + " existerar inte", "Statistic.buildTermMap", null, false);
				}
				HashSet<Term> extractedTerms = new HashSet<Term>();
				String value = indexMap.get(index);
				if ("*".equals(value)) {
					// TODO: göra liknande om inte värdet är *, prefix borde vara ganska lätt om inte annat - behov?
					TermEnum tenum = null;
					try {
						tenum = searcher.getIndexReader().terms(new Term(index));
						Term t;
						// hmm, inte som andra enumerations, här börjar den direkt utan att man
						// ska anropa next först..
						// TODO: undra vad som händer i ett tomt index.. hoppas nullkontrollen är ok?
						do {
							t = tenum.term();
							if (t == null || !t.field().equals(index)) {
								break;
							}
							// snabbfiltrering, finns det inte ens tillräckligt många träffar
							// totalt så finns det ju inte sen i sökningen heller
							if (tenum.docFreq() >= removeBelow) {
								extractedTerms.add(t);
							}
						} while (tenum.next());
					} finally {
						if (tenum != null) {
							tenum.close();
						}
					}
				} else {
					Term term = new Term(indexValue,value);
					query = new WildcardQuery(term);
					Query tempQuery = searcher.rewrite(query);
					tempQuery.extractTerms(extractedTerms);
				}
				termMap.put(indexValue, extractedTerms);
			}catch(TooManyClauses e) {
				throw new BadParameterException("indexet " + index + " har för många unika värden för att utföra denna operation", "Statistic.buildTermMap", null, false);
			}
			catch(IOException e) {
				throw new DiagnosticException("Oväntat IO fel uppstod. Var god försök igen", "Statistic.buildTermMap", e.getMessage(), true);
			}
		}
		return termMap;
	}

	/**
	 * skriver ut nedre delen av svars XML
	 */
	protected void writeFot()
	{
		writer.println("<echo>");
		writer.println("<method>" + Statistic.METHOD_NAME + "</method>");
		for(String index : indexMap.keySet()) {
			writer.println("<index>" + index + "=" + indexMap.get(index) + "</index>");
		}
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}

	/**
	 * skriver ut resultat
	 * @param queryResults
	 */
	protected void writeResult(List<QueryContent> queryResults)
	{
		for(int i = 0; i < queryResults.size(); i++)
		{
			QueryContent queryContent = queryResults.get(i);
			writer.println("<term>");
			for(String index : queryContent.getTermMap().keySet())
			{
				writer.println("<indexFields>");
				writer.print("<index>");
				writer.print(index);
				writer.println("</index>");
				writer.print("<value>");
				//xmlEscape snodde jag ur SRUServlet
				writer.print(StaticMethods.xmlEscape(
						queryContent.getTermMap().get(index)));
				writer.println("</value>");
				writer.println("</indexFields>");
			}
			writer.print("<records>");
			writer.print(queryContent.getHits());
			writer.println("</records>");
			writer.println("</term>");
		}
	}

	/**
	 * skriver ut övre del av svars XML
	 * @param queryResults
	 */
	protected void writeHead(List<QueryContent> queryResults)
	{
		StartEndWriter.writeStart(writer);
		StartEndWriter.hasHead(true);
		//skriver ut hur många värden det blev
		writer.println("<numberOfTerms>" + queryResults.size() +
				"</numberOfTerms>");
	}

	/**
	 * utför en sökning
	 * @param searcher som används för sökning
	 * @param queryResults lista med querys som skall göras
	 * @throws DiagnosticException
	 * @throws BadParameterException
	 */
	protected void doStatistic(IndexSearcher searcher,	
			List<QueryContent> queryResults)
		throws DiagnosticException, BadParameterException
	{
		for(int i = 0; i < queryResults.size(); i++) {
			try {
				QueryContent content = queryResults.get(i);
				Query q = content.getQuery();
				TopDocs topDocs = searcher.search(q, 1);
				if(topDocs.totalHits >= removeBelow) {
					content.setHits(topDocs.totalHits);
					queryResults.set(i, content);
				}else {
					queryResults.remove(i);
					i--;
				}
			}catch(IOException e) {
				throw new DiagnosticException("Oväntat IO fel uppstod. Var god försök igen", "Statistic.doStatistic", e.getMessage(), true);
			}
		}
	}
}