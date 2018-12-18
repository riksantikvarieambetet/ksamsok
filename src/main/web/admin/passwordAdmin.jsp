<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.Map" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Administrera l&ouml;senord</title>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../css/passwordAdmin.css"/>
</head>
<body>
	<%@include file="nav_and_services_i.jsp" %>
<div id="page">
	<div id="head">
		<form action="orgAdmin" accept-charset="UTF-8">
			<input type="submit" value="Administrera organisationsinformation"/>
		</form>
	</div>
	<div id="content">
		<form action="orgAdmin" accept-charset="UTF-8" method="post">
			<input type="hidden" name="operation" value="updatePasswords"/>
			<table>
				<tr id="th">
					<th>Organisation</th>
					<th>L&ouml;sen</th>
				</tr>
				<%
				Map<String,String> passwordMap = (Map<String,String>) request.getAttribute("passwords");
				int counter = 0;
				for(Map.Entry<String,String> entry : passwordMap.entrySet()) {
				%>
				<tr>
					<td class="title">
						<strong><%=entry.getKey() %></strong>
						<input type="hidden" name="organizations" value="<%=entry.getKey() %>"/>
					</td>
					<td><input type="text" name="passwords" value="<%=entry.getValue() == null ? "" : entry.getValue() %>"/></td>
				</tr>
				<%
					counter++;
				}
				%>
				<tr>
					<td></td>
					<td><input type="submit" value="spara"/></td>
				</tr>
			</table>
		</form>
	</div>
</div>
</body>
</html>