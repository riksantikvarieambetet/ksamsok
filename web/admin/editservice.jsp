<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="se.raa.ksamsok.organization.OrganizationDatabaseHandler" %>
<%@page import="se.raa.ksamsok.organization.OrganizationServlet" %>
<%@page import="se.raa.ksamsok.organization.Organization" %>
<%@page import="java.util.Map" %>
<%@page import="java.util.List" %>

<%
	HarvestServiceManager hsm = HarvesterServlet.getInstance().getHarvestServiceManager();
	HarvestRepositoryManager hrm = HarvesterServlet.getInstance().getHarvestRepositoryManager();
	String serviceId = request.getParameter("serviceId");
	String uidString = " [" + request.getRemoteUser() + "]";
%>
	<head>
		<title>
			<%= (serviceId == null ? "Ny" : "Redigera") + " tjänst" + uidString %>
		</title>
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
<%
	HarvestService service;
	if (serviceId != null) {
		service = hsm.getService(serviceId);
		if (service == null) {
			throw new RuntimeException("Ingen service med id " + serviceId);
		}
	} else {
		service = hsm.newServiceInstance();
		service.setCronString("0 15 3 * * ?");
		service.setServiceType("OAI-PMH-SAMSOK");
		service.setName("namn på tjänst");
		service.setId("tjänste-id");
		service.setHarvestURL("http:// om OAI-PMH, file:/ om fil");
		service.setAlwaysHarvestEverything(false);
		service.setShortName("kortnamn");
	}
	Date lastHarvestDate = service.getLastHarvestDate();
	String lastHarvest;
  	if (lastHarvestDate == null) {
		lastHarvest = "aldrig";
	} else {
		lastHarvest = ContentHelper.formatDate(lastHarvestDate, true);
	}
	Date firstIndexDate = service.getFirstIndexDate();
	String firstIndexed;
  	if (firstIndexDate == null) {
  		firstIndexed = "aldrig";
	} else {
		firstIndexed = ContentHelper.formatDate(firstIndexDate, true);
	}
	File spoolFile = hrm.getSpoolFile(service);
%>
		<form action="serviceaction.jsp" method="post" accept-charset="iso-8859-1">
			<br/>
			<div>
				<% if (serviceId != null) { %>
					<button name="action" value="update">Spara</button>
					<button name="action" value="delete" onclick="javascript:return confirm('Vill du verkligen ta bort denna tjänst?')">Ta bort</button>
					<% if (!hsm.isRunning(service)) { %>
						<button name="action" value="trigger" onclick="javascript:return confirm('Vill du verkligen köra denna tjänst nu?')">Kör</button>
						<% if (spoolFile.exists()) { %>
							<button name="action" value="deletespool" onclick="javascript:return confirm('Vill du verkligen ta bort spoolfilen för denna tjänst och påtvinga ny hämtning av data?')">Ta bort spoolfil</button>
						<% } %>
					<% } else { %>
						<button name="action" value="interrupt" onclick="javascript:return confirm('Vill du verkligen försöka stoppa denna tjänst nu?')">Stoppa körning</button>
					<% } %>
					<button name="action" value="reindex" onclick="javascript:return confirm('Vill du verkligen uppdatera indexet för denna tjänst nu?')">Uppdatera index</button>
					<button name="action" value="emptyindex" onclick="javascript:return confirm('Vill du verkligen ta bort indexet för denna tjänst nu?')">Ta bort index</button>
					<button onclick="javascript:window.location='editservice.jsp?serviceId=<%=serviceId%>&showHistory=true'; return false;">Visa historik</button>
				<% } else { %>
					<button name="action" value="new">Skapa</button>
				<% } %>
				<button onclick="javascript:window.location='harvestservices.jsp'; return false;">Tillbaka</button>
			</div>
			<hr/>
			<table>
				<tbody>
					<tr>
						<td><label for="serviceId" class="bold">Tjänst:</label></td>
						<td><% if (serviceId != null) { %>
							<%= serviceId %><input id="serviceId" name="serviceId" type="hidden" value="<%= serviceId %>"/>
							<% } else { %>
							<input id="serviceId" name="serviceId" type="text"/>
							<% } %>
						</td>
					</tr>
					<tr>
						<td><label for="name" class="bold">Namn:</label></td>
						<td><input id="name" name="name" type="text" value="<%= service.getName() %>"/></td>
					</tr>
					<tr>
						<td><label for="shortName" class="bold">Tillhörande institution:</label></td>
						<td>
							<select id="shortName" name="shortName">
								<%OrganizationDatabaseHandler organizationDatabaseHandler = new OrganizationDatabaseHandler(OrganizationServlet.getDataSource()); 
								if(organizationDatabaseHandler != null) {
									List<Organization> orgList = organizationDatabaseHandler.getServiceOrganizations();
									for(Organization org : orgList) {
										if(org.getKortnamn().equals(service.getShortName())) {
										%>
											<option value="<%=org.getKortnamn() %>" selected="selected"><%=org.getNamnSwe() %></option>
										<%
										}else {
										%>
											<option value="<%=org.getKortnamn() %>"><%=org.getNamnSwe() %></option>
										<%
										}
									}
								}%>
							</select>
						</td>
					</tr>
					<tr>
						<td><label for="serviceType" class="bold">Tjänstetyp:</label></td>
						<td>
						<% String selected = "selected=\"selected\""; %>
							<select id="serviceType" name="serviceType">
								<option value="OAI-PMH-SAMSOK" <%= ("OAI-PMH-SAMSOK".equals(service.getServiceType()) ? selected : "") %>>OAI-PMH skörd k-samsöksformat</option>
								<option value="SIMPLE-SAMSOK" <%=("SIMPLE-SAMSOK".equals(service.getServiceType()) ? selected : "")%>>Från fil k-samsök (OAI-PMH-format)</option>
							</select>
						</td>
					</tr>
					<tr>
						<td><label for="cronstring" class="bold">Cron-schema:</label></td>
						<td><input id="cronstring" name="cronstring" type="text" value="<%= service.getCronString() %>"/></td>
					</tr>
					<tr>
						<td><label for="harvestURL" class="bold">Skörde-URL:</label></td>
						<td><input class="widthH" id="harvestURL" name="harvestURL" type="text" value="<%= service.getHarvestURL() %>"/></td>
					</tr>
					<tr>
						<td><label for="harvestSetSpec" class="bold">Skörde-Set:</label></td>
						<td><input id="harvestSetSpec" name="harvestSetSpec" type="text" value="<%= (service.getHarvestSetSpec() != null ? service.getHarvestSetSpec() : "" )%>"/></td>
					</tr>
					<tr>
						<td><label for="harvestDate" class="bold">Skörde-datum</label></td>
						<td><input type="text" name="harvestDate" value="<%=service.getLastHarvestDate()%>"/></td>
					</tr>
					<tr>
						<td><label for="alwayseverything" class="bold">Skörda:</label></td>
						<td>
							<select id="alwayseverything" name="alwayseverything">
								<option value="true" <%= (service.getAlwaysHarvestEverything() ? selected : "") %>>Alltid allt</option>
								<option value="false" <%=(!service.getAlwaysHarvestEverything() ? selected : "")%>>Ändringar (om stöd finns)</option>
							</select>
						</td>
					</tr>
					<tr>
						<td>Första indexering:</td>
						<td><%= firstIndexed %></td>
					</tr>
					<tr>
						<td>Senaste skörd:</td>
						<td><%= lastHarvest %></td>
					</tr>
					<%
						if (spoolFile.exists()) {
					%>
					<tr>
						<td>Spoolfil:</td>
						<td><%= spoolFile.getAbsolutePath() %> (ca <%= (int) (spoolFile.length() / (1024 * 1024)) %>Mb)
							<b>OBS! Kommer användas vid nästa körning om den ej tas bort</b>
						</td>
					</tr>
					<%
						}
					%>
					<%
						if (serviceId != null) {
							int repoCount = hrm.getCount(service);
							int indexCount = LuceneServlet.getInstance().getCount(serviceId);
					%>
					<tr>
						<td>Antal poster i repo/index:</td>
						<td><%= repoCount %> / <%= indexCount %>
						<%
							if (repoCount != indexCount) { 
						%>
							<b>OBS! Skillnad mellan repo och index</b>
						<%
							}
						%>
						</td>
					</tr>
					<tr class="bgWhite">
						<td>Status:</td>
						<td>[<%= hsm.getJobStatus(service) %>]</td>
					</tr>
					<tr class="bgWhite">
						<td colspan="2"><hr/></td>
					</tr>
					<tr class="bgWhite">
						<td>Körlog (senaste):</td>
						<td><% for(String message: hsm.getJobLog(service)) { %>
							<%= message%><br/>
							<% } %>
						</td>
					</tr>
					<%
							if (Boolean.parseBoolean(request.getParameter("showHistory"))) {
					%>
					<tr>
						<td colspan="2">&#160;</td>
					</tr>
					<tr class="bgGrayLight">
						<td colspan="2"><hr/></td>
					</tr>
					<tr class="bgGrayLight">
						<td>Körlog (historik):</td>
						<td><% for(String message: hsm.getJobLogHistory(service)) { %>
							<%= message%><br/>
							<% } %>
						</td>
					</tr>

					<%
							}
						}
					%>
					<!--  ... -->
				</tbody>
			</table>
		</form>
  </body> 

<%@page import="java.io.File"%></html>
