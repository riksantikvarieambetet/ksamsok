<%@page import="java.net.URL"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.Collator"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/html;charset=UTF-8" %>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<html>
<%
	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	SearchService searchService = ctx.getBean(SearchService.class);
	HarvestServiceManager hsm = ctx.getBean(HarvestServiceManager.class);
	HarvestRepositoryManager hrm = ctx.getBean(HarvestRepositoryManager.class);
	// funkar bara ok i drift om man går mot www.kulturarvsdata.se så länge som proxyHost
	//  inte är satt till utsidans hostnamn då solr inte är (eller ska vara i alla fall!) synligt utåt
	URL solrURL = new URL(searchService.getSolrURL());
	if ("127.0.0.1".equals(solrURL.getHost())) {
		solrURL = new URL(solrURL.toString().replaceFirst("127\\.0\\.0\\.1", request.getServerName()));
	} else if ("localhost".equals(solrURL.getHost())) {
		solrURL = new URL(solrURL.toString().replaceFirst("localhost", request.getServerName()));
	}
%>
	<head>
		<title>Översikt</title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="map.jsp">Sök + karta</a>&nbsp;&nbsp;
			<a href="harvestservices.jsp">Tjänster</a>&nbsp;&nbsp;
			<a href="apiKeyAdmin">API-nycklar</a>&nbsp;&nbsp;
			<a href="orgAdmin">Admin org</a>&nbsp;&nbsp;
			<a href="statistic">Statistik</a>&nbsp;&nbsp;
			<a href="../sru">SRU-gränssnitt</a>
			<a href="<%=solrURL.toString() %>/admin/">Solr-admin</a>
			<span class="servername"><%=request.getServerName() %></span>
		</div>
		<hr/>
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
						<th>Senast indexerad</th>
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
