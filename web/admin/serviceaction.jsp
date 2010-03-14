<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvesterServlet"%>
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="se.raa.ksamsok.lucene.LuceneServlet"%>
<%@page import="java.io.File"%><html>
	<body>
		Jobbar...
<%
	HarvestServiceManager hsm = HarvesterServlet.getInstance().getHarvestServiceManager();
	HarvestRepositoryManager hrm = HarvesterServlet.getInstance().getHarvestRepositoryManager();
   	String action = request.getParameter("action");
	String serviceId = request.getParameter("serviceId");
	if (serviceId == null && !("reindexall".equals(action) ||
			"optimize".equals(action) || "clear".equals(action) ||
			"updateoptimize".equals(action) || "interruptreindexall".equals(action))) {
		throw new RuntimeException("Inget serviceId vilket måste finnas för action=" + action);
	}
	HarvestService service = null;
   	if (serviceId != null && !"new".equals(action)) {
   		service = hsm.getService(serviceId);
   		if (service == null) {
   			throw new RuntimeException("Ingen service med serviceId = " + serviceId);
   		}
   	}
   	String redirTo = "harvestservices.jsp";
   	if ("delete".equals(action)) {
   		hsm.deleteService(service);
   	} else if ("update".equals(action)) {
   		service.setCronString(request.getParameter("cronstring"));
   		service.setName(request.getParameter("name"));
   		service.setServiceType(request.getParameter("serviceType"));
   		service.setHarvestURL(request.getParameter("harvestURL"));
   		service.setHarvestSetSpec(StringUtils.trimToNull(request.getParameter("harvestSetSpec")));
   		service.setAlwaysHarvestEverything(Boolean.valueOf(request.getParameter("alwayseverything")));
   		hsm.updateService(service);
   	} else if ("new".equals(action)) {
   		service = hsm.newServiceInstance();
   		service.setId(serviceId);
   		service.setCronString(request.getParameter("cronstring"));
   		service.setName(request.getParameter("name"));
   		service.setServiceType(request.getParameter("serviceType"));
   		service.setHarvestURL(request.getParameter("harvestURL"));
   		service.setHarvestSetSpec(StringUtils.trimToNull(request.getParameter("harvestSetSpec")));
   		service.setAlwaysHarvestEverything(Boolean.valueOf(request.getParameter("alwayseverything")));
   		hsm.createService(service);
   	} else if ("trigger".equals(action)) {
   		hsm.triggerHarvest(service);
   	} else if ("interrupt".equals(action)) {
   		hsm.interruptHarvest(service);
   	} else if ("reindex".equals(action)) {
   		hsm.triggerReindex(service);
   	} else if ("emptyindex".equals(action)) {
   		hsm.triggerRemoveindex(service);
   	} else if ("reindexall".equals(action)) {
   		hsm.triggerReindexAll();
   		redirTo = "indexservices.jsp";
   	} else if ("interruptreindexall".equals(action)) {
   		hsm.interruptReindexAll();
   		redirTo = "indexservices.jsp";
   	} else if ("clear".equals(action)) {
   		LuceneServlet.getInstance().clearLuceneIndex();
   		redirTo = "indexservices.jsp";
   	} else if ("optimize".equals(action)) {
   		service = hsm.getService(HarvestServiceManager.SERVICE_LUCENE_OPTIMIZE);
   		if (service != null) {
   			hsm.triggerHarvest(service);
   		} else {
   			throw new RuntimeException("Hittade inte optimize-servicen");
   		}
   		redirTo = "indexservices.jsp";
   	} else if ("updateoptimize".equals(action)) {
   		service = hsm.getService(HarvestServiceManager.SERVICE_LUCENE_OPTIMIZE);
   		if (service != null) {
   			service.setCronString(request.getParameter("cronstring"));
   			hsm.updateService(service);
   		} else {
   			throw new RuntimeException("Hittade inte optimize-servicen");
   		}
   		redirTo = "indexservices.jsp";
   	} else if ("deletespool".equals(action)) {
   		File spoolFile = hrm.getSpoolFile(service);
   		if (spoolFile.exists()) {
   			spoolFile.delete();
   		}
   	} else {
   		throw new RuntimeException("Felaktig action: " + action);
   	}
   	response.sendRedirect(redirTo);
%>
  </body> 
</html>
