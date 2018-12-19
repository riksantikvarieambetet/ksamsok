<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="2.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:pres="http://kulturarvsdata.se/presentation#">
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
	#box1 {border-left: 1px solid #999999; border-right: 1px solid #999999; border-bottom: 1px solid #999999; width:800px; height: auto%; padding: 10px; margin-right: auto; margin-left: auto;  background-color: #ffffff;}
      .logo {width:300px}
	

</style>
  </head>
  <body>
  <center><div id="box1"><img src="/ksamsok/bilder/raa_logo.jpg" class="logo"/><div id="red">Demo</div>
  <h2>Kulturarvsdata</h2>
    <table border="0">
      <tr bgcolor="#ededed">
        <th align="left">Tumnagel</th>
        <th align="left">Föremål</th>
        <th align="left">ID</th>
        <th align="left">Repr1</th>
        <th align="left">Repr2</th>
        <th align="left">Repr3</th>
        <th align="left">Organisation</th>
      </tr>
      <xsl:for-each select="//pres:item">
      <tr>
        <td>
          <xsl:for-each select="pres:image/pres:src">
           <xsl:if test="@type='thumbnail'">
            <img src="{.}" width="100px"/>
           </xsl:if>
           <xsl:if test="@pres:type='thumbnail'">
            <img src="{.}" width="100px"/>
           </xsl:if>
          </xsl:for-each>
        </td>
        <td><xsl:value-of select="pres:type"/></td>
        <td><xsl:value-of select="pres:idLabel"/></td>
        <td><a href ="{pres:representations/pres:representation[1]}"><xsl:value-of select="pres:representations/pres:representation[1]/@pres:format"/></a>
            <a href ="{pres:representations/pres:representation[1]}"><xsl:value-of select="pres:representations/pres:representation[1]/@format"/></a></td>
        <td><a href ="{pres:representations/pres:representation[2]}"><xsl:value-of select="pres:representations/pres:representation[2]/@pres:format"/></a>
            <a href ="{pres:representations/pres:representation[2]}"><xsl:value-of select="pres:representations/pres:representation[2]/@format"/></a></td>
        <td><a href ="{pres:representations/pres:representation[3]}"><xsl:value-of select="pres:representations/pres:representation[3]/@pres:format"/></a>
            <a href ="{pres:representations/pres:representation[3]}"><xsl:value-of select="pres:representations/pres:representation[3]/@format"/></a></td>
        <td><xsl:value-of select="pres:organization"/></td>
      </tr>
      </xsl:for-each>
    </table>
	</div></center>
  </body>
  </html>
</xsl:template>
</xsl:stylesheet>