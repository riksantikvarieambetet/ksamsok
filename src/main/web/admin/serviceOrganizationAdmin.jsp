<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri = "http://java.sun.com/jsp/jstl/core" prefix = "c"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>ServiceOrganization Admin</title>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link media="all" rel="stylesheet" type="text/css" href="../css/orgAdmin.css"/>
<script type="text/javascript">
function showHide()
{
	if(document.getElementById("hideable").style.display == "none") {
		document.getElementById("hideable").style.display = "block";
	}else {
		document.getElementById("hideable").style.display = "none";
	}
}

function remove()
{
	try {
		var r = confirm("Är du säker på att du vill ta bort organisationen?");
		if(r==true) {
			document.getElementById("operation").value = "remove";
			document.getElementById("orgForm").submit();
		}
	}catch(err) {
		alert(err);
	}
}
</script>
</head>
<body>
<%@include file="nav_and_services_i.jsp" %>
<div id="page">
	<div id="head">
		<form action="orgAdmin" accept-charset="UTF-8" method="post">
			<input type="hidden" name="operation" value="passwordAdmin"/>
			<input type="submit" value="Administrera Lösenord"/>
		</form>
		<button onclick="showHide()">Lägg till organisation</button>
	</div>
	<div id="content">
		<div id="hideable" style="display: none;">
			<form action="orgAdmin" accept-charset="UTF-8" method="post">
				<input type="hidden" name="operation" value="addOrg"/>
				<table>
					<tr>
						<td class="title">Kortnamn:</td>
						<td><input type="text" name="kortnamn"/></td>
					</tr>
					<tr>
						<td class="title">Namn p&aring; svenska</td>
						<td><input type="text" name="namnSwe"/></td>
					</tr>
					<tr>
						<td colspan="2"><input type="submit" value="Lägg till"/></td>
					</tr>
				</table>
			</form>
		</div>
		<form action="orgAdmin" accept-charset="UTF-8" method="post">
			<input type="hidden" name="operation" value="orgChoice"/>
			<select name="orgChoice">
				<c:forEach var="orgs" items="${requestScope.orgList}">
					<option value="${orgs.kortnamn}">${orgs.namnSwe}</option>
				</c:forEach>
			</select>
			<input type="submit" value="visa"/>
		</form>
		<c:if test="${!empty requestScope.orgInfo}">
			<div id="orgInfo">
				<form id="orgForm" action="orgAdmin" accept-charset="UTF-8" method="post">
					<input type="hidden" id="operation" name="operation" value="update"/>
					<div id="left">
						<table>
							<tr>
								<td class="title">Kortnamn f&ouml;r organisation:</td>
								<td><input type="text" name="kortnamn" value="${requestScope.orgInfo.kortnamn}" readonly="readonly"/></td>
							</tr>
							<tr>
								<td class="title">serviceOrganization i RDF:</td>
								<td><input type="text" name="serv_org" value="${requestScope.orgInfo.serv_org}"/></td>
							</tr>
							<tr>
								<td class="title">Organisationens namn p&aring; svenska:</td>
								<td><input type="text" name="namnSwe" value="${requestScope.orgInfo.namnSwe}"/></td>
							</tr>
							<tr>
								<td class="title">Organisationens namn p&aring; engelska:</td>
								<td><input type="text" name="namnEng" value="${requestScope.orgInfo.namnEng}"/></td>
							</tr>
							<tr>
								<td class="title">Beskrivning av organisationen p&aring; svenska:</td>
								<td><textarea cols="20" rows="5" name="beskrivSwe">${requestScope.orgInfo.beskrivSwe}</textarea></td>
							</tr>
							<tr>
								<td class="title">Beskrivning av organisationen p&aring; engelska:</td>
								<td><textarea cols="20" rows="5" name="beskrivEng">${requestScope.orgInfo.beskrivEng}</textarea></td>
							</tr>
							<tr>
								<td class="title">Adressf&auml;lt 1:</td>
								<td><input type="text" name="adress1" value="${requestScope.orgInfo.adress1}"/></td>
							</tr>
							<tr>
								<td class="title">Adressf&auml;lt 2:</td>
								<td><input type="text" name="adress2" value="${requestScope.orgInfo.adress2}"/></td>
							</tr>
							<tr>
								<td class="title">Postadress:</td>
								<td><input type="text" name="postadress" value="${requestScope.orgInfo.postadress}"/></td>
							</tr>
							<tr>
								<td class="title">E-post funktionsbrevlåda (ej personlig adress):</td>
								<td><input type="text" name="epostKontaktperson" value="${requestScope.orgInfo.epostKontaktperson}"/></td>
							</tr>
							<tr>
								<td class="title">Organisationens webbplats:</td>
								<td><input type="text" name="websida" value="${requestScope.orgInfo.websida}"/></td>
							</tr>
							<tr>
								<td class="title">K-sams&ouml;k relaterad sida:</td>
								<td><input type="text" name="websidaKS" value="${requestScope.orgInfo.websidaKS}"/></td>
							</tr>
							<tr>
								<td class="title">URL till l&aring;guppl&ouml;st bild:</td>
								<td><input type="text" name="lowressUrl" value="${requestScope.orgInfo.lowressUrl}"/></td>
							</tr>
							<tr>
								<td class="title">URL till tummnagel:</td>
								<td><input type="text" name="thumbnailUrl" value="${requestScope.orgInfo.thumbnailUrl}"/></td>
							</tr>
							<tr>
								<td></td>
								<td>
									<input type="submit" value="Spara"/>
									<button id="removeButton" onclick="remove()">Ta bort</button>
								</td>
							</tr>
						</table>
					</div>
					<div id="right">
						<table>
							<tr>
								<th colspan="2">Tj&auml;nster</th>
							</tr>
							<%int counter = 0; %>
							<c:forEach var="s" items="${requestScope.orgInfo.serviceList}">
								<tr>
									<td class="title">${s.namn}:</td>
									<td>
										<textarea cols="20" rows="5" name="service_<%=counter %>">${s.beskrivning }</textarea>
										<input type="hidden" name="service_<%=counter %>_name" value="${s.namn }"/>
										<%counter++; %>
									</td>
								</tr>
							</c:forEach>
						</table>
					</div>
				</form>
			</div>
		</c:if>
	</div>
</div>
</body>
</html>