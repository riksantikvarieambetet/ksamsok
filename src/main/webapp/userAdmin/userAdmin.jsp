<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="se.raa.ksamsok.organization.Organization" %>
<%@ page import="se.raa.ksamsok.organization.Service" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Organisation anv&auml;ndar admin</title>
<link media="all" rel="stylesheet" type="text/css" href="../css/userAdmin.css"/>
</head>
<body>
<div id="page">
	<c:choose>
		<c:when test="${sessionScope.isAuthenticated eq 'n' or empty sessionScope.isAuthenticated}">
			<form action="orgAdmin" accept-charset="UTF-8" method="post">
				<input type="hidden" name="operation" value="authenticate"/>
				<table>
					<tr>
						<td class="title">Anv&auml;ndarnamn:</td>
						<td><input type="text" name="username"/></td>
					</tr>
					<tr>
						<td class="title">L&ouml;senord:</td>
						<td><input type="password" name="password"/></td>
					</tr>
					<tr>
						<td></td>
						<td><input type="submit" value="OK"/></td>
					</tr>
				</table>
			</form>
		</c:when>
		<c:when test="${sessionScope.isAuthenticated eq 'e'}">
			<p>Fel anv&auml;ndarnamn eller l&ouml;senord</p>
			<form action="orgAdmin" accept-charset="UTF-8">
				<input type="hidden" name="operation" value="unAuthenticate"/>
				<input type="submit" value="Tillbaka"/>
			</form>
		</c:when>
		<c:otherwise>
			<div id="head">
				<form action="orgAdmin" accept-charset="UTF-8">
					<input type="hidden" name="operation" value="logout"/>
					<input type="submit" value="Logga ut"/>
				</form>
			</div>
			<div id="content">
				<form action="orgAdmin" accept-charset="UTF-8" method="post">
					<input type="hidden" name="operation" value="update"/>
					<input type="hidden" name="kortnamn" value="${requestScope.orgData.kortnamn }"/>
					<input type="hidden" name="serv_org" value="${requestScope.orgData.serv_org }"/>
					<div id="left">
						<table>
							<tr>
								<td class="title">Organisationens svenska namn:</td>
								<td>
									<input type="text" name="namnSwe" value="${requestScope.orgData.namnSwe }"/>
								</td>
							</tr>
							<tr>
								<td class="title">Organisationens engelska namn:</td>
								<td>
									<input type="text" name="namnEng" value="${requestScope.orgData.namnEng }"/> 
								</td>
							</tr>
							<tr>
								<td class="title">Beskrivning av organisationen p&aring; svenska:</td>
								<td>
									<textarea name="beskrivSwe" rows="5" cols="20">${requestScope.orgData.beskrivSwe }</textarea>
								</td>
							</tr>
							<tr>
								<td class="title">Beskrivning av organisationen p&aring; engelska:</td>
								<td>
									<textarea name="beskrivEng" rows="5" cols="20">${requestScope.orgData.beskrivEng }</textarea>
								</td>
							</tr>
							<tr>
								<td class="title">Adress 1:</td>
								<td>
									<input type="text" name="adress1" value="${requestScope.orgData.adress1 }"/>
								</td>
							</tr>
							<tr>
								<td class="title">Adress 2:</td>
								<td>
									<input type="text" name="adress2" value="${requestScope.orgData.adress2 }"/>
								</td>
							</tr>
							<tr>
								<td class="title">Postadress:</td>
								<td>
									<input type="text" name="postadress" value="${requestScope.orgData.postadress }"/>
								</td>
							</tr>
							<tr>
								<td class="title">E-post till funktionsbrevl&aring;da (ej personlig adress):</td>
								<td>
									<input type="text" name="epostKontaktperson" value="${requestScope.orgData.epostKontaktperson }"/>
								</td>
							</tr>
							<tr>
								<td class="title">Organisationens webbplats:</td>
								<td>
									<input type="text" name="websida" value="${requestScope.orgData.websida }"/>
								</td>
							</tr>
							<tr>
								<td class="title">K-Sams&ouml;krelaterad sida:</td>
								<td>
									<input type="text" name="websidaKS" value="${requestScope.orgData.websidaKS }"/>
								</td>
							</tr>
							<tr>
								<td class="title">URL till l&aring;guppl&ouml;st bild (logoty el dyl):</td>
								<td>
									<input type="text" name="lowressUrl" value="${requestScope.orgData.lowressUrl }"/>
								</td>
							</tr>
							<tr>
								<td class="title">URL till tummnagel (logoty el dyl):</td>
								<td>
									<input type="text" name="thumbnailUrl" value="${requestScope.orgData.thumbnailUrl }"/>
								</td>
							</tr>
							<tr>
								<td></td>
								<td>
									<input type="submit" value="Spara"/>
								</td>
							</tr>
						</table>
					</div>
					<div id="right">
						<table>
							<tr>
								<th></th>
								<th>Tj&auml;nster</th>
							</tr>
							<%int counter = 0; %>
							<c:forEach var="service" items="${requestScope.orgData.serviceList}">
								<tr>
									<td class="title">${service.namn}</td>
									<td>
										<textarea name="service_<%=counter%>" rows="5" cols="20">${service.beskrivning}</textarea>
										<input type="hidden" name="service_<%=counter%>_name" value="${service.namn}"/>
										<%counter++; %>
									</td>
								</tr>
							</c:forEach>
						</table>
					</div>
				</form>
			</div>
		</c:otherwise>
	</c:choose>
</div>
</body>
</html>