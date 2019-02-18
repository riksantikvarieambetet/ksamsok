<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@page import="org.apache.solr.client.solrj.util.ClientUtils"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page contentType="text/html;charset=UTF-8" %>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="java.util.Map"%><html>
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
	Map<String,String> params = ContentHelper.extractUTF8Params(request.getQueryString());
	String query = params.get("query");
	boolean escapequery = Boolean.parseBoolean(params.get("escapequery"));
	query = (query == null ? "" : query.trim());
%>
	<body class="bgGrayUltraLight">
		<%@include file="nav_and_services_i.jsp" %>
		<form action="" accept-charset="utf-8">
			<div class="center">
				<input name="escapequery" <%=escapequery ? "checked": ""%> value="true" type="checkbox">&nbsp;Escape?</input>
				<input name="query" value="<%=query.replace("\"", "&quot;")%>" />
				<button>Sök</button>
			</div>
		</form>
		<hr/>
<%
	final int maxHits = 200;
	String message = "";
	if (query.length() > 0) {
		try {
			SolrQuery q = new SolrQuery(escapequery ? ClientUtils.escapeQueryChars(query) : query);
			q.setRows(maxHits);
			q.setFields("* score");
			QueryResponse qr = searchService.query(q);
			int i = 0;
			SolrDocumentList hits = qr.getResults();
			long antal = hits.getNumFound();
%>
			<h2>Sökningen gav <%=antal%> träffar (visar max <%=maxHits%>)</h2>
<%
			for (SolrDocument sd: hits) {
				++i;
				String ident = (String) sd.getFieldValue(ContentHelper.IX_ITEMID);
				byte[] presBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_PRES);
				byte[] rdfBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_RDF);
				String lonLat = "kartdata saknas";
				// TODO: number/double
				Object lon = sd.getFieldValue(ContentHelper.I_IX_LON);
				Object lat = sd.getFieldValue(ContentHelper.I_IX_LAT);
				if (lon != null && lat != null) {
					lonLat = lon + " / " + lat;
				}
				String pres = "inget presentationsinnehåll";
				if (presBytes != null) {
					pres = new String(presBytes, "UTF-8");
					// ful-escape-xml
					pres = pres.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
				}
				String rdf = "inget rdf-innehåll";
				if (rdfBytes != null) {
					rdf = new String(rdfBytes, "UTF-8");
					// ful-escape-xml
					rdf = rdf.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
				}
%>
			<p>
				<h4 class="bgGrayLight">träff <%=i%>/<%=antal%>, score: <%= sd.getFieldValue("score") %></h4>
				<div><span class="bold">Källsystem</span> : <%=sd.getFieldValue(ContentHelper.I_IX_SERVICE)%></div>
				<div><span class="bold">URI</span> : <a href="<%=ident%>" target="_blank"><%=ident%></a> (nytt fönster/flik)</div>
				<div><span class="bold">Titel</span> : <%=sd.getFieldValue(ContentHelper.IX_ITEMTITLE)%></div>
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
		}	
	}
%>
  </body> 
</html>
