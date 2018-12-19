<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Admin API nycklar</title>
<script type="text/javascript">
function showAddNewDiv()
{
	if(document.getElementById("hideable").style.display == "none") {
		document.getElementById("hideable").style.display = "";
	}else {
		document.getElementById("hideable").style.display = "none";
	}
}

function hide()
{
	document.getElementById("hideable").style.display = "none";
}
</script>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../css/APIKeyAdmin.css"/>
</head>
<body onload="hide()">
<div id="page">
	<%@include file="nav_and_services_i.jsp" %>
	<div id="head">
		<label id="addKey" onclick="showAddNewDiv()">L&auml;gg till nyckel</label>
	</div>
	<div id="hideable">
		<form action="apiKeyAdmin" accept-charset="UTF-8">
			<input type="hidden" name="operation" value="insert"/>
			<table>
				<tr>
					<td>
						<label class="title">API nyckel:</label>
					</td>
					<td><input type="text" name="APIKey"/></td>
				</tr>
				<tr>
					<td>
						<label class="title">&Auml;gare</label>
					</td>
					<td><input type="text" name="owner"/></td>
				</tr>
				<tr>
					<td></td>
					<td><input type="submit" value="Lägg till"/></td>
				</tr>
			</table>
		</form>
	</div>
	<div id="content">
		<form action="apiKeyAdmin" accept-charset="UTF-8">
			<input type="hidden" name="operation" value="remove"/>
			<table>
				<tr>
					<th>API nyckel</th>
					<th>&Auml;gare</th>
					<th>Ta bort</th>
				</tr>
				<c:forEach var="keys" items="${requestScope.APIKeys}">
					<tr>
						<td>${keys.APIKey}</td>
						<td>${keys.owner}</td>
						<td><input type="radio" name="APIKey" value="${keys.APIKey}"/></td>
					</tr>
				</c:forEach>
				<tr>
					<td colspan="3"><input type="submit" value="Ta bort"/></td>
				</tr>
			</table>
		</form>
	</div>
</div>
</body>
</html>