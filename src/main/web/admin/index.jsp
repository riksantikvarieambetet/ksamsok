<%@page import="java.util.Map.Entry"%>
<%@page import="java.net.URL"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.Collator"%>
<%@page contentType="text/html;charset=UTF-8" %>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<html>
	<head>
		<title>Översikt</title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<%@include file="nav_and_services_i.jsp" %>
		<div class="bgGrayLight">
			<h4>
				K-Samsök 0.99, gemensamt sökindex för olika källor.
			</h4>
		</div>
		<hr/>
		<div style="float: left">
<%
			final Collator sweCol = Collator.getInstance(new Locale("sv", "SE"));
			// "trasig" db om null
			if (services == null) {
				services = Collections.emptyList();
			}
			Map<String, Long> indexCountMap = searchService.getIndexCounts();
			Collections.sort(services, new Comparator<HarvestService>() {
				public int compare(HarvestService o1, HarvestService o2) {
						return sweCol.compare(o1.getName(), o2.getName());
					}
			});
			long dbCount = 0;
			long iCount = 0;
			if (services.size() > 0) {
%>
			<h5 class="marginBottomSmall">Källor</h5>
			<table id="servicetable">
				<thead>
					<tr>
						<th>Tjänst</th>
						<th>Namn</th>
						<th>Senast skördad</th>
						<th title="Ej borttagna, dvs där deleted är null"># i databas*</th>
						<th title="Obs att poster med itemForIndexing=n inte är med här men kan fn ej skiljas ut ifrån databasposterna så antalen kan skilja beroende på det"># i index*</th>
					</tr>
				</thead>
				<tbody>
<%
				Date d;
				Number repoCount;
				Number indexCount;
				Map<String, Integer> repoCountMap = hrm.getCounts();
				for (HarvestService service: services) {
%>
					<tr>
<%
					d = service.getLastHarvestDate();
					repoCount = repoCountMap.get(service.getId());
					if (repoCount == null) {
						repoCount = 0;
					}
					dbCount += repoCount.longValue();
					indexCount = indexCountMap.remove(service.getId());
					if (indexCount == null) {
						indexCount = 0;
					}
					iCount += indexCount.longValue();
%>
						<td><a href="editservice.jsp?serviceId=<%=service.getId()%>"><%=service.getId()%></a></td>
						<td><%= service.getName() %></td>
						<td class="right"><%= (d != null ? ContentHelper.formatDate(d, false) : "aldrig") %></td>
						<td class="right"><%= repoCount %></td>
						<td class="right" style='<%= (repoCount.longValue() != indexCount.longValue() ? "color: orange;" : "") %>'><%= indexCount %></td>
					</tr>
<%
				}
%>
				</tbody>
			</table>
<%
			} else {
%>
			<p>Inga källor inlagda för närvarande</p>
<%
			}
%>
			<br/>
			<p>
				Totalt finns det för närvarande <%= iCount %> poster i indexet och
				<%= dbCount %> aktiva (deleted is null) poster i databasen fördelat på
				<%= services.size() %> tjänster.
			</p>
		</div>
<%
			if (indexCountMap.size() > 0) {
%>
		<div style="float: left; margin-left: 20px;">
			<h5 class="marginBottomSmall">OBS Dessa tjänster finns bara i indexet och inte i repot</h5>
			<table>
				<thead>
					<tr>
						<th>Tjänst</th>
						<th># i index</th>
					</tr>
				</thead>
				<tbody>
<%
				for (Entry<String,Long> entry: indexCountMap.entrySet()) {
%>
				<tr>
					<td><%=entry.getKey()%></td>
					<td class="right" style='color: orange;'><%= entry.getValue() %></td>
				</tr>
<%
				}
%>
				</tbody>
			</table>
		</div>
<%
			}
%>
		<hr style="clear: both"/>
		
	</body> 
</html>
