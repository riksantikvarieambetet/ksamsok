<%@page import="org.json.JSONObject"%>
<%@page import="se.raa.ksamsok.util.AjaxChecker"%>
<%@page import="org.springframework.web.context.support.WebApplicationContextUtils"%>
<%@page import="org.springframework.context.ApplicationContext"%>
<%@page contentType="text/html;charset=UTF-8" %>   
<%@page import="se.raa.ksamsok.harvest.HarvestServiceManager"%>
<%@page import="se.raa.ksamsok.harvest.HarvestService"%>
<%@page import="se.raa.ksamsok.harvest.HarvestRepositoryManager"%>
<%@page import="org.apache.commons.lang3.StringUtils"%>
<%@page import="java.util.Date" %>
<%@page import="java.text.SimpleDateFormat" %>
<%@page import="java.util.Locale" %>
<%@page import="java.io.File"%>
<%
	ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	HarvestServiceManager hsm = ctx.getBean(HarvestServiceManager.class);
	final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("sv", "SE"));
	HarvestRepositoryManager hrm = ctx.getBean(HarvestRepositoryManager.class);
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
   		service.setShortName(request.getParameter("shortName"));
   		//TODO!!#4446implementera i db-tabell harvestservices paused-kolumn 0/1
   		service.setPaused(Boolean.valueOf(request.getParameter("paused")));
   		String harvestDate = StringUtils.trimToNull(request.getParameter("harvestDate"));
   		if (harvestDate != null) {
	   		try {
	   			service.setLastHarvestDate(sdf.parse(harvestDate));
	   		} catch (Exception ignore) {}
   		}
   		hsm.updateService(service);
   		
   		redirTo = "editservice.jsp?serviceId=" + serviceId;
   	} else if ("new".equals(action)) {
   		service = hsm.newServiceInstance();
   		service.setId(serviceId);
   		service.setCronString(request.getParameter("cronstring"));
   		service.setName(request.getParameter("name"));
   		service.setServiceType(request.getParameter("serviceType"));
   		service.setHarvestURL(request.getParameter("harvestURL"));
   		service.setHarvestSetSpec(StringUtils.trimToNull(request.getParameter("harvestSetSpec")));
   		service.setAlwaysHarvestEverything(Boolean.valueOf(request.getParameter("alwayseverything")));
   		service.setShortName(request.getParameter("shortName"));
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
   		hrm.clearIndex();
   		redirTo = "indexservices.jsp";
   	} else if ("optimize".equals(action)) {
   		service = hsm.getService(HarvestServiceManager.SERVICE_INDEX_OPTIMIZE);
   		if (service != null) {
   			hsm.triggerHarvest(service);
   		} else {
   			throw new RuntimeException("Hittade inte optimize-servicen");
   		}
   		redirTo = "indexservices.jsp";
   	} else if ("updateoptimize".equals(action)) {
   		service = hsm.getService(HarvestServiceManager.SERVICE_INDEX_OPTIMIZE);
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
		redirTo = "editservice.jsp?serviceId=" + serviceId;
   	} else if ("unziptospool".equals(action)) {
   		hrm.extractGZipToSpool(service);   		
   	} else {
   		throw new RuntimeException("Felaktig action: " + action);
   	}
   	//TODO!!#4446
   	if (AjaxChecker.isAjax(request)) {
		JSONObject json = new JSONObject();
		json = hsm.getServiceAsJSON(serviceId);
		out.println(json);
		out.flush();
   	} else {
   		response.sendRedirect(redirTo);	
   	}
%>
