<%@page import="se.raa.ksamsok.solr.SearchService"%>
<%@page import="java.util.Locale"%>
<%@page import="java.text.Collator"%>
<%@page import="java.util.Comparator"%>
<%@page import="java.util.Collections"%>
<%@page import="se.raa.ksamsok.harvest.StatusService.Step"%>
<%@page import="java.util.List"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="java.util.Date"%>
<%@page import="se.raa.ksamsok.lucene.ContentHelper"%>
<%@page import="java.io.File"%><html>
<%
	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	final HarvestServiceManager hsm = ctx.getBean(HarvestServiceManager.class);
	HarvestRepositoryManager hrm = ctx.getBean(HarvestRepositoryManager.class);
	SearchService searchService = ctx.getBean(SearchService.class);

	final Collator sweCol = Collator.getInstance(new Locale("sv", "SE"));
	String uidString = " [" + request.getRemoteUser() + "]";
	Runtime runtime = Runtime.getRuntime();

	int procs = runtime.availableProcessors();
	int freeMem = (int) (runtime.freeMemory() / (1024 * 1024));
	int maxMem = (int) (runtime.maxMemory() / (1024 * 1024));
	int totalMem = (int) (runtime.totalMemory() / (1024 * 1024));
	File spoolDir = hrm.getSpoolDir();
	int freeDisk = (int) (spoolDir.getFreeSpace() / (1024 * 1024));
	String jvmInfo = procs + " processorer/kärnor, minne - ledigt: " + freeMem + "Mb allokerat: " +
		totalMem + "Mb max: " + maxMem + "Mb, disk - ledigt " + freeDisk + "Mb " +
		"<span style='font-size: 85%;'>(på spool: " + spoolDir.getAbsolutePath() + ")</span>";
%>
	<head>
		<title>Tjänstelista<%= uidString %></title>&nbsp;&nbsp;
		<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
	</head>
	<body class="bgGrayUltraLight">
		<br/>
		<div class="bgBlackLight menu">
			<a href="index.jsp">Startsida</a>&nbsp;&nbsp;
			<a href="indexservices.jsp">Indexhantering</a>
			<a href="<%=searchService.getSolrURL() %>/admin/">Solr-admin</a>
			<span class="servername"><%=request.getServerName() %></span>
		</div>
		<hr/>
		<div>
			<button onclick="javascript:window.location='editservice.jsp'; return false;">Ny tjänst</button>
			<span class="paddingWideLeft">JVMInfo: <%=jvmInfo %></span>
		</div>
<%
		List<HarvestService> services = hsm.getServices();
		Collections.sort(services, new Comparator<HarvestService>() {
			public int compare(HarvestService o1, HarvestService o2) {
				Step step1 = hsm.getJobStep(o1);
				Step step2 = hsm.getJobStep(o2);
				if (step1 == step2) {
					return sweCol.compare(o1.getName(), o2.getName());
				}
				if (step1 == Step.IDLE) {
					return 1;
				}
				if (step2 == Step.IDLE) {
					return -1;
				}
				if (step1 == Step.FETCH) {
					return -1;
				}
				if (step1 == Step.STORE && step2 != Step.FETCH) {
					return -1;
				}
				if (step1 == Step.INDEX && step2 != Step.FETCH && step2 != Step.STORE) {
					return -1;
				}
				if (step1 == Step.EMPTYINDEX && step2 != Step.FETCH && step2 != Step.STORE && step2 != Step.INDEX) {
					return -1;
				}
				return 1;
			}
		});
%>
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
	Step lastStep = Step.IDLE;
	for (HarvestService service: services) {
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
   		String colorStyle = "";
   		Step step = hsm.getJobStep(service);
   		if (step != Step.IDLE) {
   			colorStyle = "color: orange;";
   		}
   		if (lastStep != Step.IDLE && step == Step.IDLE) {
%>
				<tr class="bgGrayLight">
					<td colspan="7"><hr /></td>
				</tr>
<%   			
   		}
   		lastStep = step;
%>
				<tr class="<%= className %>">
					<td><a href="editservice.jsp?serviceId=<%= java.net.URLEncoder.encode(serviceId, "ISO-8859-1") %>"><%= serviceId %></a></td>
					<td><%= service.getName() %></td>
					<td><%= cronstring %></td>
					<td><%= service.getHarvestURL() + (service.getHarvestSetSpec() != null ? " (" + service.getHarvestSetSpec() + ")" : "") %></td>
					<td><%= lastHarvest %></td>
					<td><span title="Endast ändringar hämtas bara om tjänsten stödjer det"><%= service.getAlwaysHarvestEverything() ? "Alltid allt" : "Ändringar*" %></span></td>
					<td style="<%=colorStyle%>"><%= step %></td>
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
