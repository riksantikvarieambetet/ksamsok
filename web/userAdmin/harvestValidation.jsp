<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%> 
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<%@ page import="se.raa.ksamsok.harvest.validation.*" %>  
<%@ page import="java.util.List" %>  

<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>Skördvalidering</title>
	
	<script type="text/javascript">
		function onValidate(){
			var submit_btn = document.getElementById("submit_btn");
			var input_txt = document.getElementById("input_txt");
			var form1 = document.getElementById("form1");
			setInterval("tick()", 500 );
			form1.submit();
			submit_btn.disabled = input_txt.disabled = true;	
		}
	
		var tickNo = 0;
		function tick(){
			var base = "Vänta. Kan ta flera minuter";
			var a = [base, base + ".", base + "..", base + "..."];
			var output_p = document.getElementById("output_p");
			output_p.innerHTML = a[tickNo++ % a.length];
		}
	</script>
	
	<style type="text/css">
		.even {
			background-color: #CCCCCC;
		}
		.odd {
		}
		b.small {
			font-size: 80%;
		}
		p {
			padding: 5px;
			margin: 0;
			word-wrap: break-word;
		}
		#content {
			width:600px;
			margin-left:auto;
			margin-right:auto;
		}
	</style>
</head>
<body>
	<center>
		<%	String url = request.getParameter("url"); %>
		
		<p>Ange url med xmldata i ksamsöks skördformat</p>
		<form id="form1" action="harvestValidation.jsp" method="GET">
			<input size="40" id="input_txt" type="text" name="url" value="<%= url == null ? "" : url %>"/>
		</form>
		<input id="submit_btn" type="button" onclick="onValidate()" value="Validera"/>		
	</center>
	
	<div id="content">
		<p id="output_p"></p>
		<% 
		if(url != null){
			XmlValidator validator = new XmlValidator();
			List<Message> result = validator.validate(url);
			%><%= result.size() %> meddelande(n)<% 
			int i = 0;
			for(Message m : result){ %>
				<p class="<%= i++ % 2 == 0 ? "even" : "odd" %>">
					<%= m.messageText  %>
					<% if(m.showAdditionalInformation()){ %>
						<%= "<br/><b class='small'>Uppstod först på rad " + m.firstOccuranceRow + ", kolumn " + m.firstOccuranceCol + ", totalt " + m.totalOccurances + " gång(er)</b>" %>
					<% } %>
				</p>	
			<%}
		}
		%>
	</div>
</body>
</html>