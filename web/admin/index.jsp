<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<html>
<%
	HarvestServiceManager hsm = HarvesterServlet.getInstance().getHarvestServiceManager();
%>
	<head>
		<title>Startsida</title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="map.jsp">Sök + karta</a>
			<a href="harvestservices.jsp">Tjänster</a>
			<a href="../sru">SRU-gränssnitt</a>
		</div>
		<hr/>
		<div class="bgGrayLight">
			<h4>
				K-Samsök 0.2, gemensamt sökindex för olika källor.
			</h4>
		</div>
		<hr/>
		<div>
<%
			List<HarvestService> services = hsm.getServices();
			if (services.size() > 0) {
%>
			<h5 class="marginBottomSmall">Källor (senaste skörd)</h5>
			<ul>
<%
				Date d;
				for (HarvestService service: services) {
					d = service.getLastHarvestDate();
%>
				<li><%=service.getName() %> (<i><%= (d != null ? ContentHelper.formatDate(d, false) : "aldrig") %></i>)</li>
<%
				}
%>
			</ul>
<%
			} else {
%>
			<p>Inga källor inlagda för närvarande</p>
<%
			}
%>
			<br/>
			<p>
				Totalt finns det för närvarande <%= LuceneServlet.getInstance().getTotalCount() %> poster i indexet.
			</p>
		</div>
		<hr/>
		Enkla exempel med sru (i nytt fönster/flik):<br/>
		<a target="_blank" href="../sru?operation=searchRetrieve&version=1.1&query=<%=ContentHelper.IX_TEXT%>%3Drunsten">Sökning på text=runsten</a>
		<br/>
		<a target="_blank" href="../sru?operation=scan&version=1.1&scanClause=<%=ContentHelper.IX_MUNICIPALITYNAME%>%3D*">Se antal indexerade poster per kommun</a>
	</body> 
</html>
