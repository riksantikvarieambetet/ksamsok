<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.apache.solr.common.SolrInputDocument"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceImpl"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="se.raa.ksamsok.lucene.SamsokContentHelper"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/plain;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%
	// Bara ett exempel på ungefär hur det skulle kunna fungera. Man vill nog använda json-libbar etc
	// och inte i jsp om man gör detta "på riktigt" och hela jsp:n är också ett fulhack för test/prototyp
	// Anropas från http://localhost:8080/ksamsok/jsapi/apitest.html
	// Kan testas med tex:
	// http://localhost:8080/ksamsok/jsapi/jssearch.jsp?query=kung&callback=testfunc&fields=itemName,itemType 

	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	SearchService searchService = ctx.getBean(SearchService.class);
	final SamsokContentHelper sch = new SamsokContentHelper();
	String query = request.getParameter("query");
	String callback = request.getParameter("callback");
	query = (query == null ? "" : query.trim());
	SolrDocumentList hits = null;
	String message = "";
	if (query.length() > 0) {
		try {
			final int maxHits = 200;
			SolrQuery q = new SolrQuery();
			q.setRows(maxHits);
			q.setQuery(query);
			q.setFields(ContentHelper.IX_ITEMID + " " + ContentHelper.I_IX_RDF);
			QueryResponse qr = searchService.query(q);
			hits = qr.getResults();
			int i = 0;
			long antal = hits.getNumFound();
			Date fakeAddedDate = new Date();
			HarvestService fakeService = new HarvestServiceImpl();
			fakeService.setId("fake");
			fakeService.setName("fake");
			SolrInputDocument resDoc;
			String[] fields = null;
			String fieldStr = StringUtils.trimToNull(request.getParameter("fields"));
			if (fieldStr != null) {
				fields = StringUtils.split(fieldStr, ",");
			}
			if (fields == null) fields = new String[] { ContentHelper.IX_ITEMID };
%>
<%=callback%>([
<%
			for (SolrDocument sd: hits) {
				if (i > 0) {
%>,<%					
				}
				++i;
				String ident = (String) sd.getFieldValue(ContentHelper.IX_ITEMID);
				byte[] rdfBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_RDF);
				String rdf = new String(rdfBytes, "UTF-8");
				
				resDoc = sch.createSolrDocument(fakeService, rdf, fakeAddedDate);
				ContentHelper.getAndClearProblemMessages();
				String pres = null;
				for (int j = 0; j < fields.length; ++j) {
					if (pres == null) {
						pres = "";
					} else {
						pres += ", ";
					}
					Object v = resDoc.getFieldValue(fields[j]);
					pres += fields[j] + "=" + StringUtils.defaultIfEmpty(v != null ? v.toString() : null , "NULL");
				}
				pres = StringUtils.defaultIfEmpty(pres, "NO VALUES");
				// inga nyrader tack och ersätt ' med " så att js funkar
				pres = pres.replaceAll("\n"," ").replaceAll("\r", " ");
				pres = pres.replaceAll("'","\"");
%>
		'<%=pres%>'
<%
			}
%>
]);
<% System.out.println(new java.util.Date() + ": [jssearch.debug]: sökte med " + query + ", fick " + antal + " träffar"); %>
// sökte med '<%=query%>', <%=antal%> träffar

<%
		} catch (Exception e) {
			System.err.println("Fel vid sökning");
			e.printStackTrace();
		}	
	}
%>
