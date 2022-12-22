package se.raa.ksamsok.solr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.FieldAnalysisRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.AnalysisPhase;
import org.apache.solr.client.solrj.response.AnalysisResponseBase.TokenInfo;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse;
import org.apache.solr.client.solrj.response.FieldAnalysisResponse.Analysis;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.TermsParams;
import org.apache.solr.common.util.NamedList;
import org.springframework.beans.factory.annotation.Autowired;
import se.raa.ksamsok.api.util.Term;
import se.raa.ksamsok.lucene.ContentHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchServiceImpl implements SearchService {

	// max antal termer att hämta om inget angivits (-1)
	private static final int DEFAULT_TERM_COUNT = 1000;

	private static final Logger logger = LogManager.getLogger(SearchService.class);

	@Autowired
	private SolrClient solr;

	public void setSolr(SolrClient solr) {
		this.solr = solr;
	}
	@Override
	public QueryResponse query(SolrQuery query) throws SolrServerException, IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Söker med \"" + query.getQuery() + "\" sort: " + query.getSortField() + " start: " + query.getStart() + " rows: " + query.getRows());
		}
		return solr.query(query, METHOD.POST);
	}
	
	@Override
	public Set<String> analyze(String words) throws SolrServerException, IOException {
		if (logger.isInfoEnabled()) {
			logger.debug("Analyserar " + words);
		}
		Set<String> stems = new HashSet<>();
		FieldAnalysisRequest far = new FieldAnalysisRequest();
		// vi kör analys mot text-indexet då vi vet att den stammar
		far.addFieldName(ContentHelper.IX_TEXT);
		far.addFieldType("text");
		// det är query-delen vi främst är intresserade av (ger a.getQueryPhases() != null nedan)
		far.setQuery(words);
		// men field value måste sättas
		far.setFieldValue(words);
		far.setMethod(METHOD.POST);

		FieldAnalysisResponse fares = far.process(solr);
		Analysis a = fares.getFieldNameAnalysis(ContentHelper.IX_TEXT);
		List<TokenInfo> lastTokenInfoList = null;
		for (AnalysisPhase ap: a.getQueryPhases()) {
			lastTokenInfoList = ap.getTokens();
		}
		if (lastTokenInfoList != null) {
			for (TokenInfo ti: lastTokenInfoList) {
				stems.add(ti.getText());
			}
		}
		return stems;
	}

	@Override
	public List<Term> terms(String index, String prefix, int removeBelow, int maxCount) throws SolrServerException, IOException {
		// * tolkas som del av termen så sådana kan vi inte ha med
		prefix = prefix.replace("*", "");
		if (logger.isDebugEnabled()) {
			logger.debug("Hämtar termer för index: " + index + ", prefix: " + prefix);
		}
		// TODO: kommer finnas bättre sätt att göra detta i senare solr/solrj-versioner
		List<Term> terms = new LinkedList<>();
		SolrQuery query = new SolrQuery();
		query.setRequestHandler("/terms");
		query.set(TermsParams.TERMS_FIELD, index);
		query.set(TermsParams.TERMS, true);
		if (prefix != null && !prefix.isEmpty()) {

			// solr 7 hanterar inte ett tomt prefix särskilt väl
			query.set(TermsParams.TERMS_PREFIX_STR, prefix);
		}
		query.set(TermsParams.TERMS_MINCOUNT, removeBelow);
		if (maxCount > 0) {
			query.set(TermsParams.TERMS_LIMIT, maxCount);
		} else {
			// solr har default 10 vilket är lite så vi sätter alltid mer här
			query.set(TermsParams.TERMS_LIMIT, DEFAULT_TERM_COUNT);
		}
		QueryRequest qreq = new QueryRequest(query, METHOD.POST);
		@SuppressWarnings("unchecked")
		NamedList<Object> termList = (NamedList<Object>) qreq.process(solr).getResponse().get("terms");
		for (int i = 0; i < termList.size(); ++i) {
			String term = termList.getName(i);
			@SuppressWarnings("unchecked")
			NamedList<Object> items = (NamedList<Object>) termList.getVal(i);
			for (int j = 0; j < items.size(); ++j) {
				terms.add(new Term(term, items.getName(j), ((Number) items.getVal(j)).longValue()));
			}
		}
		return terms;
	}

	@Override
	public long getIndexCount(String serviceName) throws SolrServerException, IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Hämtar antal för tjänsten " + (serviceName != null ? serviceName : "*"));
		}

		SolrQuery query = new SolrQuery();
		query.setQuery(ContentHelper.I_IX_SERVICE + ":" + (serviceName != null ? serviceName : "*"));
		query.setFields(ContentHelper.I_IX_SERVICE);
		query.setRows(0);
		return query(query).getResults().getNumFound();
	}

	@Override
	public Map<String, Long> getIndexCounts() throws SolrServerException, IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("Hämtar antal för alla tjänster");
		}
		Map<String, Long> countMap = new HashMap<>();
		SolrQuery query = new SolrQuery();
		query.setQuery("*:*");
		query.setFields(ContentHelper.I_IX_SERVICE);
		query.setRows(0);
		query.setFacet(true);
		query.addFacetField(ContentHelper.I_IX_SERVICE);
		QueryResponse qr = query(query);
		for (FacetField ff: qr.getFacetFields()) {
			if (ff.getValues() != null) {
				for (Count value: ff.getValues()) {
					countMap.put(value.getName(), value.getCount());
				}
			}
		}
		return countMap;
	}
}
