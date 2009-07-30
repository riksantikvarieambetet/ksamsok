Enkelt harvest och content-repository

Utvecklat med java 1.6, tomcat 6.0 och FireFox (admingränssnittet fungerar mindre bra i IE).

** Installation

* konfigurera datakälla för applikationen jdbc/harvestdb

Ex i context.xml för en hsqldb i fil-mode:
    <Resource name="jdbc/harvestdb" auth="Container" type="javax.sql.DataSource"
    		factory="org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory"
		maxActive="10" maxIdle="5" maxWait="-1"
		username="sa" password="" driverClassName="org.hsqldb.jdbcDriver"
		url="jdbc:hsqldb:file:d:/temp/harvestdb"
		testOnBorrow="true" testOnReturn="false" testWhileIdle="true"
		timeBetweenEvictionRunsMillis="300000"
		minEvictableIdleTimeMillis="-1" numTestsPerEvictionRun="10"
		defaultAutoCommit="false" />

Jdbc-driver för rätt databastyp måste in i tomcat/lib. I och med införandet av indexering av
spatialdata följer hsqldb med i lib (den används av internt av geotools), men den bör/måste
flyttas till tomcat/lib *om* det är den databastypen som också används för lagring av innehåll
för att undvika klassladdarproblem. För Oracle behöver man tex jbdc-jar och även spatial-utökningar
för att kunna lagra spatiala data (sdoapi, sdoutl och Oracles xmlparser). Javadatabasen
Derby/JavaDB fungerar bättre än hsql fn med större datamängder. Varken hsql eller derby stödjer
spatiala data men med klassen se.raa.ksamsok.spatial.VerbatimGMLWriter kan gml:en skrivas ner
som en clob om man vill tex för debug, se nedan.

* Skapa tabeller enligt sql i sql/repo.sql för datakällan

* Peka ev ut var lucene ska lägga sitt index med javaflaggan -Dsamsok-lucene-index-dir=[sökväg till katalog]
 Om ej pekas ut kommer indexet att läggas i /var/lucene-index/ksamsok.

* Peka ev ut var filer som skördas ska läggas innan de behandlas med flaggan
 -Dsamsok-harvest-spool-dir=[sökväg till katalog]
 En skörd hämtas först till en temporärfil och flyttas sen till spool-katalogen så att den kan
 återanvändas om jobbet går fel vid senare steg, tex lagring i databas
 Om ej pekas ut kommer (default) tempdir att användas, typiskt tomcat/temp.

* Ange om inte datakällan stödjer spatialt data med -Dsamsok.spatial=false
 Default är sant och en klass för att hantera spatialdata kommer att försöka härledas
 fram utfrån klassen på uppkopplingen - fn stöds bara oracle

* Ange ev egen klass för att hantera spatialt data med -Dsamsok.spatial.class=xx.yy.Z
 klassen måste implementera interfacet se.raa.ksamsok.spatial.GMLDBWriter och ha en publik
 default-konstruktor.
 Främst för debug eller tredjeparts utv, har inget defaultvärde och bör normalt ej sättas

* Kör "ant war" och kopiera war-fil till tomcat/webapps (eller ant rpm för drift på raä)

** Användning

Ett grundläggande (fult och ej stylat) gränssnitt finns för att hantera tjänster, uppdatera
lucene-index och sökning. Det nås på http://[HOST][:PORT]/ksamsok/admin/

Tjänster kan läggas upp i gränssnittet för skörd med http (OAI-PMH-[SAMSOK]) eller via en
filläsning. En fil måste ha samma syntax som en hämtning mha OAIPMHHarvestJob.getRecords()
vilket är den metod som också används för att göra en skörd via OAI-PMH.
Ex
	fos = new FileOutputStream(new File("d:/temp/kthdiva.xml"));
	OAIPMHHarvestJob.getRecords("http://www.diva-portal.org/oai/kth/OAI", null, null, "oai_dc", null, fos);

Tjänster som skördas med OAI-DC-schemat görs om till mycket enkel ksamsöks-xml.

Tjänsterna skördas enligt den periodicitet som anges i cron-strängen. Om kolumnen jobbstatus
ej visar "OK" är det troligt att cron-strängen ej är korrekt.
Stöd finns också för att köra en tjänst interaktivt, indexera om en tjänst (innebär att uppdatera
lucene-indexet för den tjänsten - förändrar ej det skördade datat) eller att indexera om alla tjänster.
Indexoptimering kan också schemaläggas.

Ett simpelt sökgränssnitt finns vilket söker i fältet "text" (fritext) och visar
träffarna som xml och på karta om de har koordinater.

Admin-delen av centralnoden skyddas och användare måste ha rollen "ksamsok" för att få
använda den vilket måste sättas upp i tomcat-konf på vanligt sätt.

SRU-gränssnittet nås genom URL:en http://[HOST][:PORT]/ksamsok/sru och i svaret från den URL:en
finns info om vilka index som stödjs mm genom en "SRU explain".

Uppslagning ("resolve") av URI/URL till html, museumdat och rdf stödjs också. Genom att anropa
http://[HOST][:PORT]/ksamsok/[INSTITUTION]/[TJÄNST][/FORMAT]/[ID] kan man antingen i fallet
rdf få innehållet direkt, eller i övriga bli skickad vidare mha en redirect till respektive
institutions webbplats. Notera att enbart URI:s som börjar med http://kulturarvsdata.se
stödjs och formatet på dessa måste vara som ovan. Exempelvis ger uppslagning av: 
http://kulturarvsdata.se/raa/fmi/10009102180001 eller
http://kulturarvsdata.se/raa/fmi/rdf/10009102180001 -> RDF
och
http://kulturarvsdata.se/raa/fmi/html/10009102180001 -> redir till Fornsök

För test- och utvecklingsversioner (som ej ligger korrekt mappade på kulturarvsdata.se) kan man
testa med:
http://[HOST][:PORT]/ksamsok/raa/fmi/10009102180001 -> RDF osv

* Modifierad jrdf-jar
Nedanstående är (ful-)patchar som behövs för att teckenkodning ska fungera ok med
parseType="Literal" för presentations-xml:en samt för att fixa ett namespace-problem
där namespaces som användes i en literal fortsatte att skrivas ut felaktigt.
Teckenkodningsproblemet kan ev ha berott på en felaktig locale-inställning. Denna
patch förutsätter att alla dokument är i utf-8 vilket de är i ksamsök, men
det är inte tillräckligt för att submitta en generell patch till jrdf-projektet.
Namespace-problemet löses annars bara med en rad se "newNamespaceMappings.clear();"
nedan.

Index: src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java
===================================================================
--- src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java	(revision 2880)
+++ src/java/org/jrdf/parser/rdfxml/RdfXmlParser.java	(working copy)
@@ -407,6 +407,7 @@
         try {
             //saxFilter.clear();
             saxFilter.setDocumentURI(inputSource.getSystemId());
+            saxFilter.setDocumentEncoding("UTF-8"); //inputSource.getEncoding());
 
             SAXParserFactory factory = SAXParserFactory.newInstance();
             factory.setFeature("http://xml.org/sax/features/namespaces", true);
Index: src/java/org/jrdf/parser/rdfxml/SAXFilter.java
===================================================================
--- src/java/org/jrdf/parser/rdfxml/SAXFilter.java	(revision 2880)
+++ src/java/org/jrdf/parser/rdfxml/SAXFilter.java	(working copy)
@@ -106,6 +106,11 @@
     private URI documentURI;
 
     /**
+     * The document's encoding.
+     */
+    private String documentEncoding;
+
+    /**
      * Flag indicating whether the parser parses stand-alone RDF
      * documents. In stand-alone documents, the rdf:RDF element is
      * optional if it contains just one element.
@@ -193,6 +198,7 @@
         elInfoStack.clear();
         charBuf.setLength(0);
         documentURI = null;
+        documentEncoding = null;
         deferredElement = null;
 
         newNamespaceMappings.clear();
@@ -211,6 +217,20 @@
         this.documentURI = createBaseURI(documentURI);
     }
 
+    public void setDocumentEncoding(String encoding) {
+        this.documentEncoding = encoding;
+        try {
+            escapedWriter = new OutputStreamWriter(escapedStream, encoding);
+            th.setResult(new StreamResult(escapedWriter));
+        } catch (Exception e) {
+            e.printStackTrace();
+        }
+    }
+
+    public String getDocumentEncoding() {
+        return documentEncoding;
+    }
+
     public void setParseStandAloneDocuments(boolean standAloneDocs) {
         parseStandAloneDocuments = standAloneDocs;
     }
@@ -273,6 +293,8 @@
         if (parseLiteralMode) {
             appendStartTag(qName, attributes);
             xmlLiteralStackHeight++;
+            // clear now that they have been used once
+            newNamespaceMappings.clear();
         } else {
             ElementInfo parent = peekStack();
             ElementInfo elInfo = new ElementInfo(parent, qName, namespaceURI, localName);
@@ -615,7 +637,11 @@
         try {
             th.characters(c, start, length);
             escapedWriter.flush();
-            sb.append(escapedStream.toString());
+            if (documentEncoding != null) {
+                sb.append(escapedStream.toString(documentEncoding));
+            } else {
+                sb.append(escapedStream.toString());
+            }
         } catch (IOException e) {
             throw new SAXException("Error occurred escaping attribute text ", e);
         }
@@ -749,4 +775,5 @@
             this.baseURI = baseURI.resolve(createBaseURI(uriString));
         }
     }
+
 }
