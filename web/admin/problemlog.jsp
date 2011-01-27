<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.LogEvent"%>
<%@page import="se.raa.ksamsok.harvest.StatusService"%>
<%@page import="java.net.URL"%>
<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="java.util.List"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/html;charset=UTF-8" %>   
<%
	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	final HarvestServiceManager hsm = ctx.getBean(HarvestServiceManager.class);
	//HarvestRepositoryManager hrm = ctx.getBean(HarvestRepositoryManager.class);
	//SearchService searchService = ctx.getBean(SearchService.class);
	StatusService statusService = ctx.getBean(StatusService.class);

	String infoMessage = null;
	String serviceId = request.getParameter("serviceId");
	HarvestService service = null;
	if (serviceId != null) {
		service = hsm.getService(serviceId);
		if (service == null) {
			infoMessage = "Hittade inte tjänst med id " + serviceId;
		}
	}
	String hl = request.getParameter("hl");
	String maxRowsStr = request.getParameter("maxrows");
	int maxRows = 200;
	if (maxRowsStr != null) {
		try {
			maxRows = Integer.parseInt(maxRowsStr);
		} catch (Exception ignore) {}
	}
	String sort = request.getParameter("sortby");
	if (sort == null) {
		sort = "eventTs";
	}
	String sortDir = request.getParameter("sortdir");
	if (sortDir == null) {
		sortDir = "desc";
	}
	boolean sortDesc = "desc".equals(sortDir);
	String newSortDir = sortDesc ? "asc" : "desc";
	String serviceSortClass = "serviceId".equals(sort) ? (sortDesc ? "sortdesc" : "sortasc") : "sortable";
	String typeSortClass = "eventType".equals(sort) ? (sortDesc ? "sortdesc" : "sortasc") : "sortable";
	String dateSortClass = "eventTs".equals(sort) ? (sortDesc ? "sortdesc" : "sortasc") : "sortable";
	String messageSortClass = "message".equals(sort) ? (sortDesc ? "sortdesc" : "sortasc") : "sortable";

%>
<html>
	<head>
		<title>Problem- och fellogg</title>&nbsp;&nbsp;
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Startsida</a>
			<a href="harvestservices.jsp">Tjänster</a>
			<a href="indexservices.jsp">Indexhantering</a>
			<span class="servername"><%=request.getServerName() %></span>
		</div>
		<hr/>
		<h4>Problem- och felmeddelanden</h4>
<%
	if (infoMessage != null) {
%>
		<h3>INFO: <%= infoMessage %></h3>
<%
	}
	if (service == null) {
%>
		<div>
			<form method="GET">
				<label for="maxrows">Max antal rader</label>
				<input name="maxrows" type="text" value="<%=maxRows %>" onchange="this.form.submit()"/>
				<input name="sortby" type="hidden" value="<%=sort %>"/>
				<input name="sortdir" type="hidden" value="<%=sortDir %>"/>
			</form>
		</div>
		<table id="problemlog">
			<thead class="bgGrayLight">
				<tr>
					<th class="<%= serviceSortClass %>"><a href="?sortby=serviceId&sortdir=<%= newSortDir %>">Tjänst</th>
					<th class="<%= typeSortClass %>"><a href="?sortby=eventType&sortdir=<%= newSortDir %>">Typ</th>
					<th class="<%= dateSortClass %>"><a href="?sortby=eventTs&sortdir=<%= newSortDir %>">Tid</th>
					<th class="<%= messageSortClass %>"><a href="?sortby=message&sortdir=<%= newSortDir %>">Meddelande</th>
				</tr>
			</thead>
			<tbody>
<%
		List<LogEvent> messages = statusService.getProblemLogHistory(maxRows, sort, sortDir);
		boolean even = true;
		String type;
		for (LogEvent logEvent: messages) {
			switch (logEvent.getEventType()) {
			case LogEvent.EVENT_ERROR:
				type = "FEL";
				break;
			case LogEvent.EVENT_WARNING:
				type = "VARNING";
				break;
			case LogEvent.EVENT_INFO:
				type = "INFO";
				break;
				default:
					type = "OKÄNT: " + logEvent.getEventType();
			}
%>
				<tr class="<%= (even = !even) ? "bgGrayUltraLight" : "bgWhite" %>">
					<td><a href="?serviceId=<%= logEvent.getServiceId() %>&hl=<%=logEvent.getEventTime()%>" title="Visa logg">
							<%= logEvent.getServiceId() %>
						</a>
					</td>
					<td><%= type %></td>
					<td><%= logEvent.getEventTime() %></td>
					<td><%= logEvent.getMessage() %></td>
				</tr>
<%
   		}
%>
			</tbody>
		</table>
<%
	} else {
%>

		<hr/>
		<a href="javascript:history.go(-1)">Tillbaka</a>
		<hr/>
		<table id="problemlog">
			<thead class="bgGrayLight">
				<tr>
					<th>Logg för <%= serviceId %>, <%= service.getName() %></th>
				</tr>
			</thead>
			<tbody>
<%
		List<String> messages = statusService.getStatusLogHistory(service);
		String msgClass;
		for (String message: messages) {
			msgClass = (message != null && message.indexOf(hl) >= 0 ? "bold bgWhite" : "");
%>
			<tr>
				<td class="<%=msgClass %>"><%= message %></td>
			</tr>
<%
		}
%>
			</tbody>
		</table>
<%
	}
%>
  </body> 
</html>
