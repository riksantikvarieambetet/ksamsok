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
	// om man gör detta "på riktigt"


	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	SearchService searchService = ctx.getBean(SearchService.class);

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
			q.setFields(ContentHelper.IX_ITEMID + " " + ContentHelper.I_IX_PRES);
			QueryResponse qr = searchService.query(q);
			hits = qr.getResults();
			int i = 0;
			long antal = hits.getNumFound();
%>
<%=callback%>([
<%
			for (SolrDocument sd: hits) {
				if (i > 0) {
%>,<%					
				}
				++i;
				String ident = (String) sd.getFieldValue(ContentHelper.IX_ITEMID);
				byte[] presBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_PRES);
				String pres = new String(presBytes, "UTF-8");
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
