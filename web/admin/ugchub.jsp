<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>UGC-hubb</title>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../css/statistic.css"/>
<link rel="stylesheet" type="text/css" href="http://code.jquery.com/ui/1.10.3/themes/smoothness/jquery-ui.css"/>
<script type="text/javascript" src="http://code.jquery.com/jquery-latest.pack.js"></script>
<script type="text/javascript" src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script>
<script type="text/javascript">
  var UGC_NS = {
	init : function () {
		UGC_NS.initEditLinks();
	},
	
	initEditLinks : function () {
		$('.edit-dialog').on('click', function (e){
	         e.preventDefault();
	         var url = $(this).attr('href'),
	             width = 500,
	             height = 500;
	         UGC_NS.loadDialog(url, width, height);
	      });
	      
	    $('.delete-dialog').on('click', function (e){
	         e.preventDefault();
	         var url = $(this).attr('href'),
                 width = 400,
                 height = 300;
	         UGC_NS.loadDialog(url, width, height);
	      });
	},
	
	loadDialog : function (url, width, height) {
		$('#manageDialog').dialog({
	            autoOpen: false,
	            modal: true,
	            draggable: false,
	            resizable: false,
	            width: width,
	            height: height
// 	            ,
// 	            close: function (event, ui){
// 	            	$(this).close();
// 	            	UGC_NS.initEditLinks();
// 	            }
          }).load(url, function(){
		    	
		    }).dialog('open');
	}
  };
  $(function (){
	  UGC_NS.init();
  });
</script>
</head>
<body>
 <%@include file="nav_and_services_i.jsp" %>
 <div id="page">
    <table>
      <tr>
        <th>ContentId</th>
        <th>ObjectUri</th>
        <th>CreateDate</th>
        <th>UserName</th>
        <th>ApplicationName</th>
        <th>Tag</th>
        <th>Coordinate</th>
        <th>Comment</th>
        <th>RelatedTo</th>
        <th>RelationType</th>
        <th>UpdateDate</th>
        <th>ImageUrl</th>
      </tr>
      <c:forEach var="ugchub" items="${requestScope.ugcHubs}">
        <tr>
          <td>${ugchub.id}</td>
          <td>${ugchub.objectUri}</td>
          <td>${ugchub.createDate}</td>
          <td>${ugchub.userName}</td>
          <td>${ugchub.applicationName}</td>
          <td>${ugchub.tag}</td>
          <td>${ugchub.coordinate}</td>
          <td>${ugchub.comment}</td>
          <td>${ugchub.relatedUri}</td>
          <td>${ugchub.relationType}</td>
          <td>${ugchub.updateDate}</td>
          <td>${ugchub.imageUrl}</td>
          <td style="border: none; padding: 5px;"><a href="editugc?id=${ugchub.id}" class="edit-dialog">Redigera</a></td>
          <td style="border: none; padding: 5px;"><a href="deleteugc?id=${ugchub.id}" class="delete-dialog">Ta bort</a></td>
        </tr>
      </c:forEach>
    </table>
    <c:set var="p" value="${pageNumber}" />
    <c:set var="l" value="${requestScope.pageDisp}" />
    <c:set var="r" value="${l / 2}" />
    <c:set var="t" value="${sessionScope.tot}" />
    <c:set var="begin" value="${((p - r) > 0 ? ((p - r) < (t - l + 1) ? (p - r) : (t - l)) : 0) + 1}" />
    <c:set var="end" value="${(p + r) < t ? ((p + r) > l ? (p + r) : l) : t}" />
    <c:forEach begin="${begin}" end="${end}" var="page">
      <c:if test="${p == page}">
        <c:set value="currentPage" var="cssClass"></c:set>
      </c:if>
      <a href="ugchub?pageNumber=${page}" class="cssClass">${page}</a>
    </c:forEach>
  </div> 
  <div id="manageDialog"></div> 
</body>
</html>