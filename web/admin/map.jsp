<%@page import="java.util.Collections"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@page import="org.apache.solr.client.solrj.response.QueryResponse"%>
<%@page import="org.apache.solr.client.solrj.SolrResponse"%>
<%@page import="org.apache.solr.client.solrj.util.ClientUtils"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>
<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.StringTokenizer"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%><html>
	<head>
		<title>Sök</title>
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
			function init(zoomToExtent, extent) {
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
				if (zoomToExtent && extent) {
					map.zoomToExtent(OpenLayers.Bounds.fromString(extent));
				} else if (markers.markers.length == 1) {
					var lonlat = markers.markers[0].lonlat;
					zoomTo(lonlat.lon, lonlat.lat);
				} else if (markers.markers.length > 0) {
					map.zoomToExtent(markers.getDataExtent());
				} else {
					map.zoomToMaxExtent();
				}
			}
			var size = new OpenLayers.Size(14,14);
			var offset = new OpenLayers.Pixel(-(size.w/2), -size.h);
			var icon = new OpenLayers.Icon('http://kulturarvsdata.se/bilder/globe.png',size,offset);

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
			function doSearch(form) {
				if (form.useMapExtent.checked) {
					var extent = map.getExtent();
					form.searchExtent.value = extent.toBBOX();
				}
			}
		</script>
	</head>
<%
	Map<String,String> params = ContentHelper.extractUTF8Params(request.getQueryString());
	String query = StringUtils.trimToEmpty(params.get("query"));
	boolean withCoordsOnly = Boolean.parseBoolean(params.get("withCoords"));
	boolean getRDF = true; //Boolean.parseBoolean(params.get("getRDF"));
	boolean useMapExtent = Boolean.parseBoolean(params.get("useMapExtent"));
	String searchExtent = StringUtils.trimToEmpty(params.get("searchExtent"));
	String limitToService = StringUtils.trimToEmpty(params.get("serviceId"));
%>
	<body onload="init(<%=useMapExtent%>, '<%=searchExtent%>')" class="bgGrayUltraLight">
		<%@include file="nav_and_services_i.jsp" %>
		<div class="floatLeft">
			<div id="map" style="width: 512px; height: 256px; border: 1px solid #ccc;" class="smallmap"></div>
		</div>
		<div class="floatLeft marginLeft" style="width: 40%;">
			<form action="" accept-charset="utf-8" onSubmit="doSearch(this)">
				<input style="width: 75%" name="query" value="<%=query.replace("\"", "&quot;")%>">
				<button>Sök</button>
				<hr/>
				<input class="middle" type="checkbox" id="withCoords" name="withCoords" value="true" <%= withCoordsOnly ? "checked" : "" %> />
				<label class="marginSmallLeft" for="withCoords">Enbart träffar med kartdata</label>
				<!--
				<br/>
				<input class="middle" type="checkbox" id="getRDF" name="getRDF" value="true" <%= getRDF ? "checked" : "" %> />
				<label class="marginSmallLeft" for="getRDF">Hämta träffarnas RDF</label>
				-->
				<br/>
				<input class="middle" type="checkbox" id="useMapExtent" name="useMapExtent" value="true" <%= useMapExtent ? "checked" : "" %> />
				<label class="marginSmallLeft" for="useMapExtent">Sök inom kartutsträckning</label>
				<input type="hidden" name="searchExtent"/>
				<hr/>
				<label for="serviceId">Begränsa till tjänst</label>
				<select class="marginSmallLeft" name="serviceId" id="serviceId">
					<option value="">[ingen begränsning]</option>
					<%
						if (services == null) {
							services = Collections.emptyList();
						}
						for (HarvestService s: services) {
					%>
					<option value="<%= s.getId() %>"<%= s.getId().equals(limitToService) ? " selected" : ""%>><%= s.getId() %> - <%= s.getName() %></option>
					<%
						}
					%>
				</select>
				<br/>
			</form>
		</div>
<%
	String message = "";
	if (query.length() > 0 || withCoordsOnly || useMapExtent || limitToService.length() > 0) {
		try {
			String qs = "";
			SolrQuery q = new SolrQuery();
			if (query.length() > 0) {
				qs = ClientUtils.escapeQueryChars(query);
			}
			if (withCoordsOnly) {
				qs += " " + ContentHelper.IX_GEODATAEXISTS + ":j";
			}
			if (useMapExtent && searchExtent.length() > 0) {
				double[] coords = new double[4];
				StringTokenizer tokenizer = new StringTokenizer(searchExtent, ",");
				int i = 0;
				while(tokenizer.hasMoreTokens()) {
					coords[i++] = Double.parseDouble(tokenizer.nextToken().trim());
				}
				String fq = ContentHelper.I_IX_LON + ":[" + coords[0] + " TO " + coords[2] + "] ";
				fq += ContentHelper.I_IX_LAT + ":[" + coords[1] + " TO " + coords[3] + "]";
				q.setFilterQueries(fq);
			}
			if (limitToService.length() > 0) {
				qs += " " + ContentHelper.I_IX_SERVICE + ":" + ClientUtils.escapeQueryChars(limitToService);
			}
			
			final int maxHits = 200;
			q.setRows(maxHits);
			if (qs.length() == 0) {
				qs = "*";
			}
			q.setQuery(qs.trim());
			q.setFields("* score");
			QueryResponse qr = searchService.query(q);
			SolrDocumentList hits = qr.getResults();
			int i = 0;
			long antal = hits.getNumFound();
%>
			<h2 class="clear">Sökningen gav <%=antal%> träffar (visar max <%=maxHits%>, varav <span id="mapCount">0</span> på kartan)</h2>
<%
			for (SolrDocument sd: hits) {
				++i;
				//Document d = s.doc(sd.doc);
				String ident = (String) sd.getFieldValue(ContentHelper.IX_ITEMID); //.CONTEXT_SET_REC + "." + ContentHelper.IX_REC_IDENTIFIER);
				String itemTitle = (String) sd.getFieldValue(ContentHelper.IX_ITEMTITLE);
				if (itemTitle == null) {
					itemTitle = "titel saknas";
				}
				byte[] presBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_PRES);
				String lonLat = "kartdata saknas";
				Object lon = sd.getFieldValue(ContentHelper.I_IX_LON);
				Object lat = sd.getFieldValue(ContentHelper.I_IX_LAT);
				String zoomTo = "", zoomToTitle = "";
				if (lon != null && lat != null) {
					lonLat = lon + " / " + lat;
					zoomTo = "zoomTo(" + lon + "," + lat + ")";
					zoomToTitle = "Klicka för att zooma in till";
%>
				<script type="text/javascript">
					addMarker(<%=lon%>,<%=lat%>, '<%=itemTitle%>', '<%=ident%>');
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
				String rdf = null;
				if (getRDF) {
					byte[] rdfBytes = (byte[]) sd.getFieldValue(ContentHelper.I_IX_RDF);
					if (rdfBytes != null) {
						rdf = new String(rdfBytes, "UTF-8");
						// ful-escape-xml
						rdf = rdf.replaceAll("\\&","&amp;").replaceAll("<","&lt;");
					} else {
						rdf = "inget rdf-innehåll";
					}
				}
				String htmlURL = (String) sd.getFieldValue(ContentHelper.I_IX_HTML_URL);
				String museumdatURL = (String) sd.getFieldValue(ContentHelper.I_IX_MUSEUMDAT_URL);
%>
			<p>
				<h4 class="bgGrayLight">träff <%= i %>/<%= antal %>, score: <%= sd.getFieldValue("score") %></h4>
				<div><span class="bold">Källsystem</span> : <%= (String) sd.getFieldValue(ContentHelper.I_IX_SERVICE) %></div>
				<div><span class="bold">URI</span> : <a href="<%= ident %>" target="_blank"><%= ident %></a> [
					<% if (htmlURL != null) { %> <a href="<%= htmlURL %>" target="_blank">HTML</a><% } %>
					<% if (museumdatURL != null) { %> <a href="<%= museumdatURL %>" target="_blank">MUSEUMDAT</a><% } %> ]
					(öppnas i nytt fönster/flik)</div>
				<div><span class="bold">Titel</span> : <span class="capitalize"><%= itemTitle %></span></div>
				<div><span onclick="<%= zoomTo %>" title="<%= zoomToTitle %>"><b>Lon/Lat</b> : <%= lonLat %></span></div>
				<div><span title="Klicka för att visa/dölja presentationsblocket" onclick="toggle('pres_<%= i %>')"><b>Presentation</b> [visa/dölj]</span> : <span id="pres_<%= i %>" class="hide"><%= pres %></span></div>
				<% if (getRDF) { %>
				<div><span title="Klicka för att visa/dölja RDF" onclick="toggle('rdf_<%= i %>')"><b>RDF</b> [visa/dölj]</span> : <span id="rdf_<%= i %>" class="hide"><%= rdf %></span></div>
				<% } %>
			</p>
<%
			}
		} catch (Exception e) {
			System.err.println("Fel vid sökning");
			e.printStackTrace();
%>
			<h2 class="clear">Fel vid sökning: <%=e.getMessage()%></h2>
<%
		}	
	}
%>
  </body> 
</html>