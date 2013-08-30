<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../css/statistic.css"/>
<script type="text/javascript" src="../jsapi/ugcmanage.js"></script>
</head>
<body>
  <div id="editContent">
    <form id="editUgc" action="saveedit" method="post">
      <c:set var="id" value="${ugchub.id}" />
      <input type="hidden" name="id" id="id" value="${id}" />
      <label for="objecturi">objectUri</label><br />
      <c:set var="objectUri" value="${ugchub.objectUri}" />
      <input type="text" name="objecturi" id="objecturi" value="${objectUri}" /><br />
      <label for="createdate">createDate</label><br />
      <c:set var="createdate" value="${ugchub.createDate}" />
      <input type="text" name="createdate" id="createdate" value="${createdate}" disabled="disabled" /><br />
      <label for="username">username</label><br />
      <c:set var="userName" value="${ugchub.userName}" />
      <input type="text" name="username" id="username" value="${userName}" disabled="disabled" /><br />
      <label for="applicationname">applicationName</label><br />
      <c:set var="applicationName" value="${ugchub.applicationName}" />
      <input type="text" name="applicationname" id="applicationname" value="${applicationName}" disabled="disabled" /><br />
      <label for="tag">tag</label><br />
      <c:set var="tag" value="${ugchub.tag}" />
      <input type="text" name="tag" id="tag" value="${tag}" /><br />
      <span class="error">${errors.tag}</span><br />
      <label for="coordinate">coordinate</label><br />
      <c:set var="coordinate" value="${ugchub.coordinate}" />
      <input type="text" name="coordinate" id="coordinate" value="${coordinate}" /><br />
      <span class="error">${errors.coordinate}</span><br />
      <label for="comment">comment</label><br />
      <c:set var="comment" value="${ugchub.comment}" />
      <input type="text" name="comment" id="comment" value="${comment}" /><br />
      <span class="error">${errors.comment}</span><br />
      <label for="relatedto">relatedTo</label><br />
      <c:set var="relatedUri" value="${ugchub.relatedUri}" />
      <input type="text" name="relatedto" id="relatedto" value="${relatedUri}" /><br />
      <span class="error">${errors.relatedto}</span><br />
      <label for="relationtype">relationType</label><br />
      <c:set var="relationType" value="${ugchub.relationType}" />
      <input type="text" name="relationtype" id="relationtype" value="${relationType}" /><br />
      <span class="error">${errors.relationtype}</span><br />
      <label for="imageurl">imageUrl</label><br />
      <c:set var="imageUrl" value="${ugchub.imageUrl}" />
      <input type="text" name="imageurl" id="imageurl" value="${imageUrl}" /><br />
      <span class="error">${errors.imageurl}</span><br />
      <input type="submit" name="submit" value="Spara ändringar" />
      <span class="error">${errors.submit}</span>
      <a href="#" class="close-dialog">Avbryt</a>
    </form>
  </div>
</body>
</html>