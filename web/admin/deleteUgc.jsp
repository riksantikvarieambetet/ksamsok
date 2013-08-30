<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title></title>
<link media="all" href="../css/default.css" type="text/css" rel="stylesheet">
<link rel="stylesheet" type="text/css" href="../css/statistic.css"/>
</head>
<body>
  <div id="deleteContent">
    <form id="deleteUgc" action="deleteedit" method="post">
      <input type="hidden" name="id" id="id" value="${ugchub.id}" /><br />
      <label for="objecturi">objectUri</label><br />
      <input type="text" name="objecturi" id="objecturi" value="${ugchub.objectUri}" disabled="disabled" /><br />
      <label for="createdate">createDate</label><br />
      <input type="text" name="createdate" id="createdate" value="${ugchub.createDate}" disabled="disabled" /><br />
      <label for="username">username</label><br />
      <input type="text" name="username" id="username" value="${ugchub.userName}" disabled="disabled" /><br />
      <label for="applicationname">applicationName</label><br />
      <input type="text" name="applicationname" id="applicationname" value="${ugchub.applicationName}" disabled="disabled" /><br />
      <label for="tag">tag</label><br />
      <input type="text" name="tag" id="tag" value="${ugchub.tag}" disabled="disabled" /><br />
      <label for="coordinate">coordinate</label><br />
      <input type="text" name="coordinate" id="coordinate" value="${ugchub.coordinate}" disabled="disabled" /><br />
      <label for="comment">comment</label><br />
      <input type="text" name="comment" id="comment" value="${ugchub.comment}" disabled="disabled" /><br />
      <label for="relatedto">relatedTo</label><br />
      <input type="text" name="relatedto" id="relatedto" value="${ugchub.relatedUri}" disabled="disabled" /><br />
      <label for="relationtype">relationType</label><br />
      <input type="text" name="relationtype" id="relationtype" value="${ugchub.relationType}" disabled="disabled" /><br />
      <label for="imageurl">imageUrl</label><br />
      <input type="text" name="imageurl" id="imageurl" value="${ugchub.imageUrl}" disabled="disabled" /><br />
      <input type="submit"  name="submit" value="Ta bort">
      <span class="error">${errors.submit}</span>
      <a href="#" class="close-dialog">Avbryt</a>
    </form>
  </div>
</body>
</html>