<%@page import="se.raa.ksamsok.api.util.Term"%>
<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/html;charset=UTF-8" %>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<html>
<%
	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	SearchService searchService = ctx.getBean(SearchService.class);
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
			List<Term> organizations = searchService.terms(ContentHelper.IX_SERVICEORGANISATION, "", 0, -1);
			if (organizations.size() > 0) {
%>
			<h5 class="marginBottomSmall">Indexerade källorganisationer</h5>
			<ul>
<%
				for (Term service: organizations) {
%>
				<li><%= service.getValue() %> (<%= service.getCount() %>)</li>
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
				Totalt finns det för närvarande <%= searchService.getIndexCount(null) %> poster i indexet.
			</p>
		</div>
		<hr/>
		
	</body> 
</html>
