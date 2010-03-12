package se.raa.ksamsok.api.method;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TopDocs;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

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
 * Klass gjort för att enkelt implementera facet sökningar i TA
 * @author Henrik Hjalmarsson
 */
public class Facet extends StatisticSearch 
{	
	/** metodens namn */
	public static final String METHOD_NAME = "facet";
	
	private static final Logger logger = Logger.getLogger("se.raa.ksamsok.api.method.Facet");

	/**
	 * skapar ett objekt av Facet
	 * @param indexMap de index som skall ingå i facetten
	 * @param writer för att skriva resultatet
	 * @param queryString filtrerar resultatet efter query
	 */
	public Facet(Map<String, String> indexMap, PrintWriter writer, String queryString) 
	{
		super(writer, queryString, indexMap); 
	}

	@Override
	public void performMethod() 
		throws BadParameterException, DiagnosticException, 
			MissingParameterException 
	{
		IndexSearcher searcher = LuceneServlet.getInstance().borrowIndexSearcher();
		try {	
			Map<String,Set<Term>> termMap = buildTermMap(searcher);
			List<QueryContent> queryContentList = 
				convertTermMapToQueryContentList(termMap);
			CQLParser parser = new CQLParser();
			CQLNode node = parser.parse(queryString);
			Query filterQuery = CQL2Lucene.makeQuery(node);
			doFacet(searcher, queryContentList, filterQuery);
			writeHead(queryContentList);
			writeResult(queryContentList);
			writeFot();
		} catch (CQLParseException e) {
			throw new DiagnosticException("Oväntat parserfel uppstod - detta beror troligen på att query strängen inte följer CQL syntax. Var god kontrollera query-strängen eller kontakta systemadministratören för systemet du använder dig av.", "Facet.performMethod", null, false);
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO-fel, var god försök igen", "Facet.performMethod", e.getMessage(), true); 
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}

	@Override
	protected void writeFot()
	{
		writer.println("<echo>");
		writer.println("<method>" + Facet.METHOD_NAME + "</method>");
		for(String index : indexMap.keySet()) {	
			writer.println("<index>" + index + "</index>");
		}
		writer.println("<query>" + StaticMethods.xmlEscape(queryString) + "</query>");
		writer.println("</echo>");
		StartEndWriter.writeEnd(writer);
		StartEndWriter.hasFoot(true);
	}

	/**
	 * Tar ut facetter utifrån den inskickade frågan. Använd bara om removeBelow
	 * är > 0 då denna metod använder sig av termfrekvensvektorer för de dokument
	 * som träffas och därför aldrig kan lista alla värden (förekomst 0 ggr).
	 * 
	 * @param searcher lucene-searcher
	 * @param filterQuery sökfråga
	 * @return lista med {@linkplain QueryContent}-instanser
	 * @throws DiagnosticException vid alla fel
	 */
	/*
	 * Försök med termvektorer som inte blev helt bra. Första sökningen (med samma parametrar) tar
	 * längre tid än alternativet men sen går det oftast snabbare, tyvärr så söker folk inte på
	 * exakt samma sak flera gånger och eftersom det cachas internt i lucene vet man inte hur
	 * länge frågecachen "lever". För att detta ska funka alls måste fält till skapas med flaggan
	 * Field.TermVector.YES, dvs med new Field(... Field.Store.NO, Field.Index.NOT_ANALYZED, Field.TermVector.YES);
	 * vid indexering, annars lagras ingen termvektor.
	private List<QueryContent> doFacet(IndexSearcher searcher, DocIdSet idSet) throws DiagnosticException {
		List<QueryContent> qcList = null;
		//Filter qwf = new CachingWrapperFilter(new QueryWrapperFilter(filterQuery));
		try {
			// hämta doc-id:n
			//DocIdSet idSet = qwf.getDocIdSet(searcher.getIndexReader());
			DocIdSetIterator iter = idSet.iterator();
			qcList = new LinkedList<QueryContent>();
			// map för att spara undan träffar
			Map<String, Map<String, Integer>> counts = new HashMap<String, Map<String,Integer>>();
			Map<String, Integer> count;
			while (iter.next()) {
				// för varje dokument och index hämta termfreq-vektor och uppdatera
				// totala antalet förekomster av termer i dokumentmängden
				for (Entry<String, String> indices: indexMap.entrySet()) {
					String index = CQL2Lucene.translateIndexName(indices.getKey());
					TermFreqVector v = searcher.getIndexReader().getTermFreqVector(iter.doc(), index);
					if (v == null) {
						continue;
					}
					for (String term: v.getTerms()) {
						count = counts.get(index);
						if (count == null) {
							// första gången indexet används
							count = new HashMap<String, Integer>();
							counts.put(index, count);
						}
						Integer c = count.get(term);
						if (c == null) {
							// första förekomsten av termen
							c = 1;
						} else {
							++c;
						}
						count.put(term, c);
					}
				}
			}
			for (Entry<String, Map<String, Integer>> indexNames: counts.entrySet()) {
				String indexName = indexNames.getKey();
				// måste indexets värden konverteras?
				final boolean isIsoIndex = ContentHelper.isISO8601DateYearIndex(indexName);
				String term;
				int termCount;
				for (Entry<String, Integer> termCounts: indexNames.getValue().entrySet()) {
					QueryContent qc = new QueryContent();
					term = termCounts.getKey();
					termCount = termCounts.getValue();
					// filtrera bort de som har för få träffar
					if (termCount >= removeBelow) {
						// konvertera ev värdet till nåt läsbart
						if (isIsoIndex) {
							term = Long.toString(ContentHelper.transformLuceneStringToLong(term));
						}
						qc.addTerm(indexName, term);
						qc.setHits(termCount);
						qcList.add(qc);
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Problem att ta fram facetter (removeBelow: " + removeBelow + ")", t);
			throw new DiagnosticException("Problem att ta fram facetter", "Facet.doFacet", t.getMessage(), false);
		}
		return qcList;
	}
	*/

	/**
	 * utför facet sökningen
	 * @param searcher
	 * @param queryContentList
	 * @throws DiagnosticException
	 */
	private void doFacet(IndexSearcher searcher, List<QueryContent> queryContentList,
			Query filterQuery) 
		throws DiagnosticException
	{
		try {
			// använd frågan som ett filter och cacha upp filterresultatet
			Filter qwf = new CachingWrapperFilter(new QueryWrapperFilter(filterQuery));
			if (logger.isDebugEnabled()) {
				logger.debug("about to make " + queryContentList.size() +
						" queries filtered by " + filterQuery);
			}
			// TODO: datastrukturen/algoritmen bör kanske ändras här då det borde bli
			//       en del onödigt kopierande inne i ArrayList när element tas bort
			for (int i = 0; i < queryContentList.size(); i++) {	
				QueryContent queryContent = queryContentList.get(i);
				Query query = queryContent.getQuery();
				TopDocs topDocs = searcher.search(query, qwf, 1);
				if (topDocs.totalHits < removeBelow) {
					queryContentList.remove(i);
					i--;
				} else {
					queryContent.setHits(topDocs.totalHits);
					Map<String, String> termMap = queryContent.getTermMap();
					// gå igenom indexen för att se om värdena behöver översättas för att kunna visas
					for (Map.Entry<String, String> indexTerm: termMap.entrySet()) {
						if (ContentHelper.isISO8601DateYearIndex(indexTerm.getKey())) {
							long year = ContentHelper.transformLuceneStringToLong(indexTerm.getValue());
							indexTerm.setValue(Long.toString(year));
						}
					}
					queryContentList.set(i, queryContent);
				}
			}
		} catch (IOException e) {
			throw new DiagnosticException("Oväntat IO fel uppstod. Var god försök igen.", "Facet.doFacet", e.getMessage(), true);
		}
	}
	
	/**
	 * converterar term mappen till en lista med QueryContent Objekt.
	 * detta för att denna metod ej skall göra kartesisk produkt på alla värden.
	 * @param termMap
	 * @return List<QueryContent>
	 */
	protected List<QueryContent> convertTermMapToQueryContentList(
			Map<String,Set<Term>> termMap)
	{
		List<QueryContent> queryContentList =  new ArrayList<QueryContent>();
		for(String index : termMap.keySet()) {
			for(Term term: termMap.get(index)) {
				QueryContent queryContent = new QueryContent();
				queryContent.addTerm(index, term.text());
				queryContentList.add(queryContent);
			}
		}
		return queryContentList;
	}
}