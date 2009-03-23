<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%><html>
<%
	HarvestServiceManager hsm = HarvesterServlet.getInstance().getHarvestServiceManager();
	String uidString = " [" + request.getRemoteUser() + "]";
%>
	<head>
		<title>Tjänstelista<%= uidString %></title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Startsida</a>
			<a href="indexservices.jsp">Indexhantering</a>
		</div>
		<hr/>
		<div>
			<button onclick="javascript:window.location='editservice.jsp'; return false;">Ny tjänst</button>
		</div>
		<hr/>
		<table id="servicetable">
			<thead class="bgGrayLight">
				<tr>
					<th>Tjänst</th>
					<th>Namn</th>
					<th>Cron-schema</th>
					<th>Skörde-URL</th>
					<th>Senaste skörd</th>
					<th>Skörda</th>
					<th>Jobbstatus</th>
				</tr>
			</thead>
			<tbody>
<%
	int i = 0;
	String className;
	for (HarvestService service: hsm.getServices()) {
   		String serviceId = service.getId();
   		String cronstring = service.getCronString();
   		Date lastHarvestDate = service.getLastHarvestDate();
   		String lastHarvest;
   		if (lastHarvestDate == null) {
   			lastHarvest = "aldrig";
   		} else {
			lastHarvest = ContentHelper.formatDate(lastHarvestDate, true);
   		}
   		if (++i % 2 == 0) {
   			className = "bgWhite";
   		} else {
   			className = "bgGrayUltraLight";
   		}
%>
				<tr class="<%= className %>">
					<td><a href="editservice.jsp?serviceId=<%= serviceId %>"><%= serviceId %></a></td>
					<td><%= service.getName() %></td>
					<td><%= cronstring %></td>
					<td><%= service.getHarvestURL() + (service.getHarvestSetSpec() != null ? " (" + service.getHarvestSetSpec() + ")" : "") %></td>
					<td><%= lastHarvest %></td>
					<td><span title="Endast ändringar hämtas bara om tjänsten stödjer det"><%= service.getAlwaysHarvestEverything() ? "Alltid allt" : "Ändringar*" %></span></td>
					<td><%= hsm.getJobStep(service) %></td>
				</tr>
				<tr class="<%= className %>">
					<td>&#160;</td>
					<td colspan="6">[ <%= hsm.getJobStatus(service) %> ]</td>
				</tr>
<%
   }
%>
			</tbody>
		</table>
<%
%>
  </body> 
</html>
