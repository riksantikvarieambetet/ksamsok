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
		<div>
<%
			final Collator sweCol = Collator.getInstance(new Locale("sv", "SE"));
			List<HarvestService> services = hsm.getServices();
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
				Map<String, Long> indexCountMap = searchService.getIndexCounts();
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
					indexCount = indexCountMap.get(service.getId());
					if (indexCount == null) {
						indexCount = 0;
					}
					iCount += indexCount.longValue();
%>
						<td><a href="editservice.jsp?serviceId=<%=service.getId()%>"><%=service.getId()%></a></td>
						<td><%= service.getName() %>
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
		<hr/>
		Enkla exempel med sru (i nytt fönster/flik):<br/>
		<a target="_blank" href="../sru?operation=searchRetrieve&version=1.1&query=<%=ContentHelper.IX_TEXT%>%3Drunsten">Sökning på text=runsten</a>
		<br/>
		<a target="_blank" href="../sru?operation=scan&version=1.1&scanClause=<%=ContentHelper.IX_MUNICIPALITYNAME%>%3D*">Se antal indexerade poster per kommun</a>
	</body> 
</html>
