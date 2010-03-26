<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri = "http://java.sun.com/jstl/core" prefix = "c"%>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="se.raa.ksamsok.organization.Organization" %>
<%@ page import="se.raa.ksamsok.organization.Service" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SericeOrganization Admin</title>
<link media="all" rel="stylesheet" type="text/css" href="../css/orgAdmin.css"/>
</head>
<body>
<div id="page">
	<div id="head">
		<form action="orgAdmin" accept-charset="UTF-8">
			<input type="hidden" name="operation" value="passwordAdmin"/>
			<input type="submit" value="Administrera l&ouml;senord"/>
		</form>
	</div>
	<div id="choseForm">
	<form action="orgAdmin" accept-charset="UTF-8">
		<select name="orgChoice">
			<%
				Map<String,String> orgMap = (Map<String,String>) request.getAttribute("orgMap");
				for(Map.Entry<String,String> entry : orgMap.entrySet()) {
			%>
				<option value="<%=entry.getKey()%>"><%=entry.getValue()%></option>
			<%
				}
			%>
		</select>
		<input type="submit" value="V&auml;lj"/>
	</form>
	</div>
	<%
		Organization org = (Organization) request.getAttribute("orgData");
		if(org != null) {
	%>
	<form action="orgAdmin" accept-charset="UTF-8" method="post">
	<div id="infoForm">
	<div id="orgInfo">
		<input name="update" value="ja" type="hidden"/>
		<table>
			<tr>
				<td class="titel"><strong>Kortnamn:</strong></td>
				<td><input name="kortnamn" type="text" value="<%=org.getKortnamn()%>" readonly="readonly"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Namn p&aring; svenska:</strong></td>
				<td><input name="namnSwe" type="text" value="<%=org.getNamnSwe()%>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Namn p&aring; engelska:</strong></td>
				<td><input name="namnEng" type="text" value="<%=org.getNamnEng()%>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Beskrivning p&aring; svenska:</strong></td>
				<td><textarea cols="20" rows="5" name="beskrivSwe"><%=org.getBeskrivSwe()%></textarea></td>
			</tr>
			<tr>
				<td class="titel"><strong>Beskrivning p&aring; engelska:</strong></td>
				<td><textarea name="beskrivEng" rows="5" cols="20" ><%=org.getBeskrivEng()%></textarea></td>
			</tr>
			<tr>
				<td class="titel"><strong>adressf&auml;lt ett:</strong></td>
				<td><input name="adress1" type="text" value="<%=org.getAdress1() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>adressf&auml;lt tv&aring;:</strong></td>
				<td><input name="adress2" type="text" value="<%=org.getAdress2() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Postadress:</strong></td>
				<td><input name="postadress" type="text" value="<%=org.getPostadress() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Kontaktperson:</strong></td>
				<td><input name="kontaktperson" type="text" value="<%=org.getKontaktperson()%>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>E-post kontaktperson:</strong></td>
				<td><input name="epostKontaktperson" type="text" value="<%=org.getEpostKontaktperson()%>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Websida:</strong></td>
				<td><input name="websida" type="text" value="<%=org.getWebsida() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>K-Samsök relaterad websida:</strong></td>
				<td><input name="websidaKS" type="text" value="<%=org.getWebsidaKS() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>L&aring;guppl&ouml;st bild:</strong></td>
				<td><input name="lowressUrl" type="text" value="<%=org.getLowressUrl() %>"/></td>
			</tr>
			<tr>
				<td class="titel"><strong>Tumnagel:</strong></td>
				<td><input name="thumbnailUrl" type="text" value="<%=org.getThumbnailUrl() %>"/></td>
			</tr>
			<tr class="submit">
				<td></td>
				<td><input type="submit" value="Spara" /></td>
			</tr>
		</table>
		</div>
		<div id="serviceForm">
		<table>
			<tr>
				<th colspan="2"><strong>Tj&auml;nster</strong></th>
			</tr>
			<%
				List<Service> serviceList = org.getServiceList();
				for(int i = 0; serviceList != null && i < serviceList.size(); i++) {
					Service s = serviceList.get(i);
			%>
			<tr>
				<td class="titel"><strong><%=s.getNamn() %> beskrivning:</strong></td>
				<td>
					<textarea name="<%="service_" + i %>" rows="5" cols="20"><%=s.getBeskrivning() %></textarea>
					<input type="hidden" name="<%="service_" + i + "_name" %>" value="<%=s.getNamn() %>"/>
				</td>
			</tr>
			<%
				}
			%>
			
			
		</table>
	</div>
	</div>
	</form>
	<%
		}
	%>
</div>
</body>
</html>