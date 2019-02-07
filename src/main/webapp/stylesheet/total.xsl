<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:srw="http://www.loc.gov/zing/srw/">
<xsl:output method='html' version='1.0' encoding='iso-8859-1' indent='yes'/>

<xsl:template match="/">
  <html>
  <head>
  <style type="text/css">
	body {font-family: Verdana,arial; font-size: 11px; margin-top: 0; margin-bottom: 0; padding: 0; height: 100%;}

	h1 {font-family: Verdana,arial; font-size: 14px; font-weight: bold;}
	h2 {font-family: Verdana,arial; font-size: 12px; font-weight: bold;}

	p {font-family: Verdana; font-size: 11px;}

	td {font-family: Verdana; font-size: 11px; border-bottom: 1px solid #999999;}
	th {font-family: Verdana; font-size: 11px;}

	li {font-family: Verdana; font-size: 11px;}
	#red {color: #990000; font-weight: bold;}
	#box1 {border-left: 1px solid #999999; border-right: 1px solid #999999; border-bottom: 1px solid #999999; width:800px; height: 100%; padding: 10px; margin-right: auto; margin-left: auto;  background-color: #ededed;}
	
</style>
  </head>
  <body>

  


<xsl:for-each select="//srw:searchRetrieveResponse">
<xsl:value-of select='format-number(srw:numberOfRecords, "###,##0")'/>

</xsl:for-each>

  </body>
  </html>
</xsl:template>
</xsl:stylesheet>
