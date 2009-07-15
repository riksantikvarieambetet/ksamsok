<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="org.apache.lucene.search.IndexSearcher"%>
<%@page import="org.apache.lucene.queryParser.QueryParser"%>
<%@page import="org.apache.lucene.search.Query"%>
<%@page import="org.apache.lucene.document.Document"%>

<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="org.apache.lucene.search.TopDocs"%>
<%@page import="org.apache.lucene.search.ScoreDoc"%>
<%@page import="org.apache.solr.util.NumberUtils"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%><html>
	<head>
		<title>Sök</title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
		<script type="text/javascript">
			function toggle(id) {
				var el = document.getElementById(id);
				if (el) {
					if (el.className.indexOf('hide') >= 0) {
						el.className = el.className.replace('hide', '');
					} else {
						el.className = el.className + ' hide';
					}
				}
			}
		</script>
	</head>
<%
	String query = request.getParameter("query");
	query = (query == null ? "" : query.trim());
%>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Startsida</a>
		</div>
		<hr/>
		<form action="" accept-charset="iso-8859-1">
			<div class="center">
				<input name="query" value="<%=query.replace("\"", "&quot;")%>">
				<button>Sök</button>
			</div>
		</form>
		<hr/>
<%
	TopDocs hits = null;
	String message = "";
	IndexSearcher s = null;
	HarvestRepositoryManager hrm = null;
	if (query.length() > 0) {
		try {
			s = LuceneServlet.getInstance().borrowIndexSearcher();
			hrm = HarvesterServlet.getInstance().getHarvestRepositoryManager();
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
			<h2>Sökningen gav <%=antal%> träffar (visar max <%=maxHits%>)</h2>
<%
			for (ScoreDoc sd: hits.scoreDocs) {
				++i;
				Document d = s.doc(sd.doc);
				String ident = d.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				byte[] presBytes = d.getBinaryValue(ContentHelper.I_IX_PRES);
				String lonLat = "kartdata saknas";
				String lon = d.get(ContentHelper.I_IX_LON);
				String lat = d.get(ContentHelper.I_IX_LAT);
				if (lon != null && lat != null) {
					lonLat = NumberUtils.SortableStr2double(lon) + " / " + NumberUtils.SortableStr2double(lat);
				}
				String pres = "inget innehåll";
				if (presBytes != null) {
					pres = new String(presBytes, "UTF-8");
					// ful-escape-xml
					pres = pres.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
				}
				String rdf = hrm.getXMLData(ident);
				if (rdf != null) {
					rdf = rdf.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
				}
%>
			<p>
				<h4 class="bgGrayLight">träff <%=i%>/<%=antal%>, score: <%=sd.score%></h4>
				<div><span class="bold">Källsystem</span> : <%=d.get(ContentHelper.I_IX_SERVICE)%></div>
				<div><span class="bold">URI</span> : <a href="<%=ident%>" target="_blank"><%=ident%></a> (nytt fönster/flik)</div>
				<div><span class="bold">Titel</span> : <%=d.get(ContentHelper.IX_ITEMTITLE)%></div>
				<div><span class="bold">Lon/Lat</span> : <%=lonLat%></div>
				<div><span onclick="toggle('pres_<%= i %>')"><b>Presentation</b> [visa/dölj]</span> : <span id="pres_<%= i %>" class="hide"><%=pres%></span></div>
				<div><span onclick="toggle('rdf_<%= i %>')"><b>RDF</b> [visa/dölj]</span> : <span id="rdf_<%= i %>" class="hide"><%=rdf%></span></div>
			</p>
<%
			}
		} catch (Exception e) {
			System.err.println("Fel vid sökning");
			e.printStackTrace();
%>
			<h2>Fel vid sökning: <%=e.getMessage()%></h2>
<%
		} finally {
			LuceneServlet.getInstance().returnIndexSearcher(s);
		}	
	}
%>
  </body> 
</html>
