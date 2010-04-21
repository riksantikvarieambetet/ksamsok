<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>K-Sams&ouml;k - statistic</title>
<link rel="stylesheet" type="text/css" href="../css/statistic.css"/>
<script type="text/javascript" src="http://code.jquery.com/jquery-latest.pack.js"></script>
<script type="text/javascript">
function hideShow()
{
	if(document.getElementById("hideable").style.display == "none") {
		document.getElementById("hideable").style.display = "";
		document.getElementById("toggle").innerHTML = "Dölj";
	}else {
		document.getElementById("hideable").style.display = "none";
		document.getElementById("toggle").innerHTML = "Visa";
	}
}
</script>
</head>
<body>
<div id="page">
	<div id="head">
		<a href="../userAdmin/orgAdmin">Administrera organisation</a>
	</div>
	<div id="overview">
		<label id="toggle" onclick="hideShow()">Dölj</label>
		<div id="hideable">
			<form action="statistic" accept-charset="UTF-8">
				<input type="hidden" name="operation" value="show"/>
				<label>Sortera efter:</label>
				<select name="sortBy">
					<option value="apikey">API nyckel</option>
					<option value="searchstring">S&ouml;kstr&auml;ng</option>
					<option value="param">S&ouml;k parameter</option>
					<option value="count">Antal s&ouml;kningar</option>
				</select>
				<label>Sorterings konfiguration:</label>
				<select name="sortConf">
					<option value="ASC">Ascending</option>
					<option value="DESC">Descending</option>
				</select>
				<table>
					<tr>
						<th>API Nyckel</th>
						<th>&Auml;gare</th>
						<th>Antal s&ouml;kningar</th>
						<th><label>Visa:</label><input type="radio" name="APIKey" value="ALL" checked="checked"/></th>
					</tr>
					<c:forEach var="apikeys" items="${requestScope.apikeys}">
						<tr>
							<td>${apikeys.APIKey}</td>
							<td>${apikeys.owner}</td>
							<td>${apikeys.total}</td>
							<td>
								<label>Visa:</label>
								<input type="radio" name="APIKey" value="${apikeys.APIKey}"/>
							</td>
						</tr>
					</c:forEach>
					<tr>
						<td colspan="4"><input type="submit" value="visa"/></td>
					</tr>
				</table>
			</form>
		</div>
	</div>
	<c:if test="${!empty requestScope.statisticData}">
		<div id="statistic">
			<table>
				<tr>
					<th>API nyckel</th>
					<th>S&ouml;kparameter</th>
					<th>S&ouml;kstr&auml;ng</th>
					<th>Antal s&ouml;kningar</th>
				</tr>
				<c:forEach var="statistic" items="${requestScope.statisticData}">
					<tr>
						<td>${statistic.APIKey}</td>
						<td>${statistic.param}</td>
						<td>${statistic.queryString}</td>
						<td>${statistic.count}</td>
					</tr>
				</c:forEach>
			</table>
		</div>
	</c:if>
</div>
</body>
</html>