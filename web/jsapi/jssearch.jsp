<%@page contentType="text/plain;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="org.apache.lucene.search.IndexSearcher"%>
<%@page import="org.apache.lucene.queryParser.QueryParser"%>
<%@page import="org.apache.lucene.search.Query"%>
<%@page import="org.apache.lucene.document.Document"%>

<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="org.apache.lucene.search.TopDocs"%>
<%@page import="org.apache.lucene.search.ScoreDoc"%>
<%
	String query = request.getParameter("query");
	String callback = request.getParameter("callback");
	query = (query == null ? "" : query.trim());
	TopDocs hits = null;
	String message = "";
	IndexSearcher s = null;
	if (query.length() > 0) {
		try {
			s = LuceneServlet.getInstance().borrowIndexSearcher();
			// fk. QueryParser är ej trådsäker
			// vi analyzerar detta med en stemmer då vi vet att IX_TEXT analyseras vid indexering
			// se ContentHelper.isAnalyzedIndex()
			QueryParser p = new QueryParser(ContentHelper.IX_TEXT, ContentHelper.getSwedishAnalyzer());
			Query q = p.parse(query);
			final int maxHits = 200;
			hits = s.search(q, maxHits);
			int i = 0;
			int antal = hits.totalHits;
%>
<%=callback%>([
<%
			for (ScoreDoc sd: hits.scoreDocs) {
				++i;
				Document d = s.doc(sd.doc);
				String ident = d.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				String pres = d.get(ContentHelper.I_IX_PRES);
				if (pres != null) {
					// inga nyrader tack
					pres = pres.replaceAll("\n"," ").replaceAll("\r", " ");
				}
%>
		'<%=pres%>',
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
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(s);
		}	
	}
%>
