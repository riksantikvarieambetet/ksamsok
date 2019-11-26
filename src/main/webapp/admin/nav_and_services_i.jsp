<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="se.raa.ksamsok.harvest.StatusService"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page pageEncoding="UTF-8" %>
<%
	final ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	final SearchService searchService = ctx.getBean(SearchService.class);
	final HarvestServiceManager hsm = ctx.getBean(HarvestServiceManager.class);
	final HarvestRepositoryManager hrm = ctx.getBean(HarvestRepositoryManager.class);
	final StatusService statusService = ctx.getBean(StatusService.class);

	List<HarvestService> services = hsm.getServices();
	final boolean isSchedulerStarted = hsm.isSchedulerStarted();
%>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Översikt</a>
			<a href="harvestservices.jsp">Tjänster</a>
			<a href="indexservices.jsp">Indexhantering</a>
			<a href="problemlog.jsp">Problemlogg</a>
			<a href="orgAdmin">Admin org</a>
			<span class="servername"><%=request.getServerName() %></span>
		</div>
<%
	if (!isSchedulerStarted) {
%>
		<hr>
			<h2 class="red">Scheduleraren körs ej fn, troligen pga db-problem vid uppstart!</h2>
<%
	}
	if (services == null) {
%>
		<hr>
			<h2 class="red">Ingen kontakt med databasen fn!</h2>
<%
	}
%>
		<hr/>
