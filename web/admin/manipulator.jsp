<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.Collection"  %>
<%@ page import="se.raa.ksamsok.manipulator.Manipulator" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" type="text/css" href="../css/manipulator.css"/>
<title>Manipulator</title>
</head>
<body>
<div id="page">
	<div id="head">
		<span id="indexLink">
			<a href="index.jsp">Index</a>
		</span>
		<form action="manipulate"  method="get" id="stopButton">
			<input type="hidden" name="operation" value="stop"/>
			<input type="submit" value="Stop"/>
		</form>
	</div>
	<div id="operations">
		<form action="manipulate" method="get">
			<input type="hidden" name="operation" value="nativeUrl"/>
			<input type="submit" value="Native Url"/>
		</form>
	</div>
	<div id="statusArea">
		<%
			Collection<Manipulator> manipulators = (Collection<Manipulator>) request.getAttribute("manipulators");
			if(manipulators != null) {
				for(Manipulator manipulator : manipulators) {
		%>
		<div class="statusObject">
			<h3><%=manipulator.getName() %></h3>
			<p class="status"><%=manipulator.getStatus() %></p>
		</div>
		<%
				}
			}
		%>
	</div>
</div>
</body>
</html>