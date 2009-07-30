<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<html>
<%
	HarvestServiceManager hsm = HarvesterServlet.getInstance().getHarvestServiceManager();
%>
	<head>
		<title>K-Samsök</title>
		<link media="all" href="css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<div class="bgGrayLight">
			<h4>
				K-Samsök, gemensamt sökindex för olika källor.
			</h4>
		</div>
		<hr/>
		<div>
<%
			List<HarvestService> services = hsm.getServices();
			if (services.size() > 0) {
%>
			<h5 class="marginBottomSmall">Indexerade källor</h5>
			<ul>
<%
				for (HarvestService service: services) {
					// ta bara med de som har indexerats nån gång
					if (service.getFirstIndexDate() != null) {
%>
				<li><%=service.getName() %></li>
<%
					}
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
		Enkelt exempel med sru:<br/>
		<a target="_blank" href="sru?operation=searchRetrieve&version=1.1&query=<%=ContentHelper.IX_TEXT%>%3Drunsten">Sökning på text=runsten</a>
		<div style="position: absolute; bottom: 1px; right: 1px;"><a style="text-decoration: none; color: black;" title=":)" href="admin/">&#960;</a></div>
	</body> 
</html>
