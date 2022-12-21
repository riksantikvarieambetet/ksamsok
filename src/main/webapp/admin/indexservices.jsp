<%@page import="org.apache.solr.client.solrj.SolrServerException"%>
<%@page import="org.apache.solr.common.util.NamedList"%>
<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="java.util.List"%><html>
<%
	String uidString = " [" + request.getRemoteUser() + "]";
%>
	<head>
		<title>Indexhantering<%= uidString %></title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<%@include file="nav_and_services_i.jsp" %>
<%		
		HarvestService service = hsm.getService(HarvestServiceManager.SERVICE_INDEX_OPTIMIZE);
		if (service != null) {
			String serviceId = service.getId();
			String cronstring = service.getCronString();
	   		Date lastHarvestDate = service.getLastHarvestDate();
	   		String lastHarvest;
	   		if (lastHarvestDate == null) {
	   			lastHarvest = "aldrig";
	   		} else {
				lastHarvest = ContentHelper.formatDate(lastHarvestDate, true);
	   		}
%>

		<form action="serviceaction.jsp" method="post" accept-charset="iso-8859-1">
			<table id="servicetable">
				<thead class="bgGrayLight">
					<tr>
						<th>Namn</th>
						<th>Cron-schema</th>
						<th>Senaste körning</th>
						<th>Jobbstatus</th>
						<th>Indexeringskommandon</th>
					</tr>
				</thead>
				<tbody>
					<tr class="bgWhite">
					<td><%= service.getName() %></td>
					<td>
						<input name="cronstring" type="text" value="<%= cronstring %>"/>
					</td>
					<td><%= lastHarvest %></td>
					<td><%= hsm.getJobStep(service) %></td>
					<td>
						<button name="action" value="updateoptimize">Spara cron-schema</button>
						<button name="action" value="optimize" onclick="return (confirm('Vill du verkligen göra optimize på indexet?'));">Kör optimize nu</button>
						<button onclick="javascript:window.location='indexservices.jsp?showHistoryOptimize=true'; return false;">Visa historik</button>
					</td>
					</tr>
					<tr class="bgWhite">
						<td colspan="5">
							[ <%= hsm.getJobStatus(service) %> ]
							<% List<String> messages = hsm.getJobLog(service);
							   if (messages != null && messages.size() > 0) {
							%>
							<hr/>
							<%    for(String message: messages) { %>
							    <%= message%><br/>
							<%     } %>
							<% } %>
						</td>
					</tr>
					<%
							if (Boolean.parseBoolean(request.getParameter("showHistoryOptimize"))) {
					%>
					<tr>
						<td colspan="5">&#160;</td>
					</tr>
					<tr class="bgGrayLight">
						<td colspan="5"><hr/></td>
					</tr>
					<tr class="bgGrayLight">
						<td>Körlog (historik):</td>
						<td colspan="4"><% for(String message: hsm.getJobLogHistory(service)) { %>
							<%= message%><br/>
							<% } %>
						</td>
					</tr>

					<%
							}
					%>

				</tbody>
			</table>
		</form>
<%
		}
%>
		<br/>
		<hr/>
		<div>
			<h4 class="bgGrayLight">Omindexering från repository</h4>
			<br/>
<%
		service = hsm.newServiceInstance();
		service.setId(HarvestServiceManager.SERVICE_INDEX_REINDEX);
		if (!hsm.isRunning(service)) {
%>
			<button onclick="if (confirm('Vill du verkligen uppdatera hela indexet från repot?')) javascript:window.location='serviceaction.jsp?action=reindexall'; return false;">Uppdatera hela indexet</button>
<%
		} else {
%>
			<button onclick="if (confirm('Vill du verkligen stoppa indexuppdateringen från repot?')) javascript:window.location='serviceaction.jsp?action=interruptreindexall'; return false;">Avbryt uppdatering av hela indexet</button>
<%
		}
%>
			<button onclick="javascript:window.location='indexservices.jsp?showHistoryReindex=true'; return false;">Visa historik</button>
		</div>
		<br/>
		<div class="bgWhite">
			[ <%= hsm.getJobStatus(service) %> ]
			<% List<String> messages = hsm.getJobLog(service);
			   if (messages != null && messages.size() > 0) {
			%>
			<hr/>
			<%
				for(String message: messages) {
			%>
			    <%= message%><br/>
			<%
				}
			   }
			%>
		</div>
			<%
				if (Boolean.parseBoolean(request.getParameter("showHistoryReindex"))) {
			%>
		<br/>
		<div class="bgGrayLight">
			<hr/>
			<p>Körlog (historik):</p>
			<%
					for(String message: hsm.getJobLogHistory(service)) {
			%>
					<%= message%><br/>
			<%
					}
				}
			%>
		</div>
		<br/>
		<div>
			<h4 class="bgGrayLight">Tömning av index</h4>
			<br/>
			<button onclick="if (confirm('Vill du verkligen tömma hela indexet?\n\n(Om du har tillgång till filsystemet är det MYCKET mer\neffektivt att stoppa centralnoden och ta bort indexet manuellt.\n Den här åtgärden låser också din webbläsarsession under tiden lucene jobbar.)')) javascript:window.location='serviceaction.jsp?action=clear'; return false;">Töm indexet</button>
		</div>
  </body> 
</html>
