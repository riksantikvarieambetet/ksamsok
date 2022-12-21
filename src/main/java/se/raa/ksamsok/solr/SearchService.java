package se.raa.ksamsok.solr;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import se.raa.ksamsok.api.util.Term;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SearchService {

	/**
	 * Ställer en fråga och ger svaret.
	 * @param query fråga
	 * @return frågesvar
	 * @throws SolrServerException vid kommunikationsproblem
	 */
	QueryResponse query(SolrQuery query) throws SolrServerException, IOException;

	/**
	 * Hämtar antal dokument i indexet för angiven tjänst, eller totalt om tjänstenamnet är null.
	 * @param serviceName tjänstenamn
	 * @return antal träffar för tjänsten eller totalt
	 * @throws SolrServerException vid fel
	 */
	long getIndexCount(String serviceName) throws SolrServerException, IOException;

	/**
	 * Hämtar antal dokument i indexet för alla tjänster nycklat på tjänste-id.
	 * @return antal träffar för alla tjänster
	 * @throws SolrServerException vid fel
	 */
	Map<String, Long> getIndexCounts() throws SolrServerException, IOException;

	/**
	 * Analyserar (stammar) ett eller flera ord.
	 * @param words ord
	 * @return mängd med ordstammar
	 * @throws SolrServerException vid sökfel
	 * @throws IOException vid kommunikationsfel
	 */
	Set<String> analyze(String words) throws SolrServerException, IOException;

	/**
	 * Hämtar termer för angivet index med angivet prefix sorterade i fallande förekomstordning.
	 * @param index indexnamn
	 * @param prefix prefix
	 * @param removeBelow minsta träff-frekvens
	 * @param maxCount max antal termer
	 * @return mängd med {@linkplain Term}er
	 * @throws SolrServerException
	 */
	List<Term> terms(String index, String prefix, int removeBelow, int maxCount) throws SolrServerException, IOException;

}
