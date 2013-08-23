<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
</head>
<body>
  <div id="editUgc">
    Edit UGC 
    id: ${ugchub.id}
    pathInfo: ${pathInfo}
    <form action="saveedit" method="post">
      <input type="hidden" name="id" value="${ugchub.id}" />
      <label for="objecturi">objectUri</label>
      <input type="text" name="objecturi" id="objecturi" value="${ugchub.objectUri}" />
      <label for="createdate">createDate</label>
      <input type="text" name="createdate" id="createdate" value="${ugchub.createDate}" disabled="disabled" />
      <label for="username">username</label>
      <input type="text" name="username" id="username" value="${ugchub.userName}" disabled="disabled" />
      <label for="applicationname">applicationName</label>
      <input type="text" name="applicationname" id="applicationname" value="${ugchub.applicationName}" disabled="disabled" />
      <label for="tag">tag</label>
      <input type="text" name="tag" id="tag" value="${ugchub.tag}" />
      <label for="coordinate">coordinate</label>
      <input type="text" name="coordinate" id="coordinate" value="${ugchub.coordinate}" />
      <label for="comment">comment</label>
      <input type="text" name="comment" id="comment" value="${ugchub.comment}" />
      <label for="relatedto">relatedTo</label>
      <input type="text" name="relatedto" id="relatedto" value="${ugchub.relatedUri}" />
      <label for="relationtype">relationType</label>
      <input type="text" name="relationtype" id="relationtype" value="${ugchub.relationType}" />
      <label for="imageurl">imageUrl</label>
      <input type="text" name="imageurl" id="imageurl" value="${ugchub.imageUrl}" />
      <input type="submit" value="Spara ändringar" />
    </form>
  </div>
</body>
</html>