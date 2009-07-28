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
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="java.util.Map"%><html>
	<head>
		<title>Sök</title>
		<!--
		<link rel="stylesheet" href="http://openlayers.org/dev/theme/default/style.css" type="text/css" />
		<link rel="stylesheet" href="http://openlayers.org/dev/examples/style.css" type="text/css" />
		<script src="http://www.fmis.raa.se/cocoon/kms-app-base/OpenLayers/2.6/OpenLayers-full.js"></script>
		-->
		<script language="JavaScript" type="text/javascript" src="http://www.openlayers.org/api/OpenLayers.js"></script>

		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
		<script type="text/javascript">
			var map = null;
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
			function zoomTo(lon, lat) {
				map.setCenter(new OpenLayers.LonLat(lon, lat), map.getNumZoomLevels() - 3);
			}
			var markers = new OpenLayers.Layer.Markers( "Markers" );
			function init(){
				map = new OpenLayers.Map('map');
				var ol_wms = new OpenLayers.Layer.WMS(
					"MagnaCarta OpenLayers WMS",
					"http://labs.metacarta.com/wms/vmap0",
					{layers: 'basic'}
				);
				var jpl_wms = new OpenLayers.Layer.WMS( "NASA Global Mosaic",
					"http://t1.hypercube.telascience.org/cgi-bin/landsat7", 
					{layers: "landsat7"}
				);
				var osmLayer = new OpenLayers.Layer.WMS( "OpenStreetMap", 
					[
						"http://t1.hypercube.telascience.org/tiles?",
						"http://t2.hypercube.telascience.org/tiles?",
						"http://t3.hypercube.telascience.org/tiles?",
						"http://t4.hypercube.telascience.org/tiles?"
					], 
					{layers: 'osm-4326', format: 'image/png' }
				);
				map.addLayers([ol_wms, jpl_wms, osmLayer, markers]);
				map.addControl(new OpenLayers.Control.LayerSwitcher());
				map.addControl(new OpenLayers.Control.MousePosition());
				map.addControl(new OpenLayers.Control.Scale());
				map.addControl(new OpenLayers.Control.ScaleLine());
				map.zoomToMaxExtent();
			}
			var size = new OpenLayers.Size(10,17);
			var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
			var icon = new OpenLayers.Icon('http://boston.openguides.org/markers/AQUA.png',size,offset);

			function addMarker(lon, lat, text, uri) {
				var m = new OpenLayers.Marker(new OpenLayers.LonLat(lon, lat), icon.clone());
				m.events.register("click", m, function (e) {
					var popup = new OpenLayers.Popup("x",
						new OpenLayers.LonLat(lon,lat),
						new OpenLayers.Size(220,60),
						"<b class='capitalize'>" + text + "</b><br>" +
						"<span style='font-size: 77%;'>" + lon + "," + lat + "</span><br>" +
						"<span style='font-size: 69%; white-space: nowrap;'>" + uri + "</span>",
						true);
					map.panTo(new OpenLayers.LonLat(lon, lat));
					map.addPopup(popup, true);
				});
				markers.addMarker(m);
			}

			function updateMapCount() {
				var el = document.getElementById("mapCount");
				if (el) {
					var val = parseInt(el.innerHTML);
					if (!isNaN(val)) {
						el.innerHTML = "" + ++val;
					}
				}
			}
		</script>
	</head>
<%
	Map<String,String> params = ContentHelper.extractUTF8Params(request.getQueryString());
	String query = params.get("query");
	query = (query == null ? "" : query.trim());
%>
	<body onload="init()" class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Startsida</a>
			<a href="search.jsp">Sök utan karta</a>
		</div>
		<hr/>
		<form action="" accept-charset="utf-8">
			<div class="center">
				<input name="query" value="<%=query.replace("\"", "&quot;")%>">
				<button>Sök</button>
			</div>
		</form>
		<hr/>
		<div id="map" style="width: 512px; height: 256px; border: 1px solid #ccc;" class="smallmap"></div>
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
			<h2>Sökningen gav <%=antal%> träffar (visar max <%=maxHits%>, varav <span id="mapCount">0</span> på kartan)</h2>
<%
			for (ScoreDoc sd: hits.scoreDocs) {
				++i;
				Document d = s.doc(sd.doc);
				String ident = d.get(ContentHelper.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				String itemTitle = d.get(ContentHelper.IX_ITEMTITLE);
				if (itemTitle == null) {
					itemTitle = "titel saknas";
				}
				byte[] presBytes = d.getBinaryValue(ContentHelper.I_IX_PRES);
				String lonLat = "kartdata saknas";
				String lon = d.get(ContentHelper.I_IX_LON);
				String lat = d.get(ContentHelper.I_IX_LAT);
				String zoomTo = "", zoomToTitle = "";
				if (lon != null && lat != null) {
					double lonDouble = NumberUtils.SortableStr2double(lon);
					double latDouble = NumberUtils.SortableStr2double(lat);
					lonLat = lonDouble + " / " + latDouble;
					zoomTo = "zoomTo(" + lonDouble + "," + latDouble + ")";
					zoomToTitle = "Klicka för att zooma in till";
%>
				<script type="text/javascript">
					addMarker(<%=NumberUtils.SortableStr2double(lon)%>,<%=NumberUtils.SortableStr2double(lat)%>, '<%=itemTitle%>', '<%=ident%>');
					updateMapCount();
				</script>
<%
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
				String htmlURL = d.get(ContentHelper.I_IX_HTML_URL);
				String museumdatURL = d.get(ContentHelper.I_IX_MUSEUMDAT_URL);
%>
			<p>
				<h4 class="bgGrayLight">träff <%=i%>/<%=antal%>, score: <%=sd.score%></h4>
				<div><span class="bold">Källsystem</span> : <%=d.get(ContentHelper.I_IX_SERVICE)%></div>
				<div><span class="bold">URI</span> : <a href="<%=ident%>" target="_blank"><%=ident%></a> [
					<% if (htmlURL != null) { %> <a href="<%=htmlURL%>" target="_blank">HTML</a><%}%>
					<% if (museumdatURL != null) { %> <a href="<%=museumdatURL%>" target="_blank">MUSEUMDAT</a><%}%> ]
					(öppnas i nytt fönster/flik)</div>
				<div><span class="bold">Titel</span> : <%=itemTitle%></div>
				<div><span onclick="<%=zoomTo%>" title="<%=zoomToTitle%>"><b>Lon/Lat</b> : <%=lonLat%></span></div>
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