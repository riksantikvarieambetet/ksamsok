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
<script type="text/javascript" src="../jsapi/ugcmanage.js"></script>
</head>
<body>
 <%@include file="nav_and_services_i.jsp" %>
 <div id="ugcPage">
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
          <td class="cellMaxWidth">${ugchub.relatedUri}</td>
          <td>${ugchub.relationType}</td>
          <td>${ugchub.updateDate}</td>
          <td>${ugchub.imageUrl}</td>
          <td class="cellLinks">
              <a href="editugc?id=${ugchub.id}" data-title="Redigera UGC-objekt" class="edit-dialog">Redigera</a><br />
              <a href="deleteugc?id=${ugchub.id}" data-title="Ta bort UGC-objekt" class="delete-dialog">Ta bort</a>
          </td>
        </tr>
      </c:forEach>
    </table>
    <c:set var="p" value="${sessionScope.pageNumber}" /> <%-- current page (1-based) --%>
    <c:set var="l" value="${requestScope.numberOfPageLinks}" /> <%-- amount of page links to be displayed --%>
    <c:set var="r" value="${l / 2}" /> <%-- minimum link range ahead/behind --%>
    <c:set var="t" value="${sessionScope.tot}" /> <%-- total amount of pages --%>
    <c:set var="begin" value="${((p - r) > 0 ? ((p - r) < (t - l + 1) ? (p - r) : (t - l)) : 0) + 1}" />
    <c:set var="end" value="${(p + r) < t ? ((p + r) > l ? (p + r) : l) : t}" />
    <div id="paginateBlock">
	    <c:forEach begin="${begin}" end="${end}" var="page">
	      <c:if test="${p == page}">
	        <c:set value="currentPageLink" var="cssClass"></c:set>
	      </c:if>
	      <c:if test="${p != page}">
	        <c:set value="notCurrentPageLink" var="cssClass"></c:set>
	      </c:if>
	      <a href="ugchub?pageNumber=${page}" class="${cssClass}">${page}</a>
	    </c:forEach>
    </div>
  </div> 
  <div id="manageDialog"></div> 
</body>
</html>