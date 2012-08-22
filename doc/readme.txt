Enkelt harvest och content-repository

Utvecklat med java 1.6, tomcat 6.0 och FireFox (admingränssnittet fungerar mindre bra i IE).

** Installation

Installeras normalt med rpm och då sker allt automatiskt och applikationen konfigureras
till att gå mot driftdatabasen. Stöd finns för att bygga master- och slav-instanser (vilket också
gäller för ksamsok-solr). Skillnad mellan master och slav i modulen ksamsok är att admin-gui:t inte
finns i en slav-instans.
Rpm skapas med: "ant RAA-RPM" (default är att bygga en master)
OBS! Kräver en ksamsok-solr i samma webapp, se readme för det projektet för relaterade utv-inställningar.

** Manuell installation för utv
Context.xml används inte längre. Context specas i ksamsok***Install paketet.  

* konfigurera datakälla för applikationen jdbc/harvestdb

För utv mot postgres och testdb, se rpm/contextTest.xml, kopiera in postgresql-*.jar till tomcat/lib från
ksamsok/lib och hoppa över steg ner till tomcat-användar-steget.

För derby, ladda ner derby från http://db.apache.org/derby/
och starta en nätverksserver (eller ändra i exempel nedan till fil) och kopiera in derbyclient.jar till
tomcat/lib. Att köra med nätverksserver gör det enklare att hantera databasen och att titta på
datat medan applikationen kör.
De databaser som stöds fn är postgres/postgis, oracle och derby (notera att derby-stödet nog ej är 100% uppdaterat).

Ex i context.xml för en derby i network-mode:
    <Resource name="jdbc/harvestdb" auth="Container" type="javax.sql.DataSource"
    		factory="org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory"
		maxActive="10" maxIdle="5" maxWait="-1"
		username="ksamsok" password="ksamsok" driverClassName="org.apache.derby.jdbc.ClientDriver"
		url="jdbc:derby://127.0.0.1:1527/ksamsok"
		testOnBorrow="true" testOnReturn="false" testWhileIdle="true"
		validationQuery="values(1)"
		timeBetweenEvictionRunsMillis="300000"
		minEvictableIdleTimeMillis="-1" numTestsPerEvictionRun="10"
		defaultAutoCommit="false" />

Jdbc-driver för rätt databastyp måste in i tomcat/lib. För postgres behövs tex postgresql-*.jar (lib) och
för Oracle behöver man tex jbdc-jar och även spatial-utökningar för att kunna lagra spatiala data
(sdoapi, sdoutl och Oracles xmlparser). Javadatabasen Derby/JavaDB stödjer inte
spatiala data men med klassen se.raa.ksamsok.spatial.VerbatimGMLWriter kan gml:en skrivas ner
som en clob om man vill tex för debug, se nedan.

* Skapa ev tabeller enligt sql i sql/repo.sql för datakällan (för derby kan identity-kolumner användas
  istället för sekvenser och för derby vill man också skicka med "create=true" i jdbc-url:en när man
  kopplar upp sig med sitt sql-verktyg för att skapa databasen/schemat automatiskt)

* Peka ev ut var filer som skördas ska läggas innan de behandlas med flaggan
 -Dsamsok-harvest-spool-dir=[sökväg till katalog]
 En skörd hämtas först till en temporärfil och flyttas sen till spool-katalogen så att den kan
 återanvändas om jobbet går fel vid senare steg, tex lagring i databas
 Om ej pekas ut kommer (default) tempdir att användas, typiskt tomcat/temp.
 Behöver normalt sett inte ändras.

* Ange ev om inte datakällan stödjer spatialt data (tex för derby) med -Dsamsok.spatial=false
 Default är sant och en klass för att hantera spatialdata kommer att försöka härledas
 fram utfrån klassen på uppkopplingen - fn stöds postgresql/postgis och oracle och för dessa behövs inget ändras

* Ange ev egen klass för att hantera spatialt data med -Dsamsok.spatial.class=xx.yy.Z
 klassen måste implementera interfacet se.raa.ksamsok.spatial.GMLDBWriter och ha en publik
 default-konstruktor.
 Främst för debug eller tredjeparts utv, har inget defaultvärde och bör normalt ej sättas. Sätts
 den måste databastabellerna stödja det den konfade klassen gör naturligtvis. Klassen
 se.raa.ksamsok.spatial.VerbatimGMLWriter stödjer att geometriernas gml skrivs ner som en
 clob i geometritabellen så om den klassen används måste databaskolumntypen för kolumnen
 geometry vara clob.
 Behövs normalt sett inte ändras.

* Lägg in användare i tomcat med rollen ksamsok för att kunna köra admin-gränssnittet. Görs
  enklast i tomcat-users.xml, se rpm/tomcat-users.xml.

* Kör "ant war" och kopiera war-fil till tomcat/webapps. Skapar normalt en master-war, vilket oftast
 är det man vill ha. För mer info se build.xml.

** Användning

Ett grundläggande gränssnitt finns för att hantera tjänster, uppdatera
solr-index och sökning. Det nås på http://[HOST][:PORT]/ksamsok/admin/

Tjänster kan läggas upp i gränssnittet för skörd med http eller via en
filläsning. En fil måste ha samma syntax som en hämtning mha OAIPMHHarvestJob.getRecords()
vilket är den metod som också används för att göra en skörd via OAI-PMH.
Ex
	fos = new FileOutputStream(new File("d:/temp/raa_fmi.xml"));
	OAIPMHHarvestJob.getRecords("http://127.0.0.1:8080/oaicat-ksamsok/OAIHandler/fmi", null, null, "ksamsok-rdf", null, fos);

Tjänsterna skördas enligt den periodicitet som anges i cron-strängen. Om kolumnen jobbstatus
ej visar "OK" är det troligt att cron-strängen ej är korrekt.
Stöd finns också för att köra en tjänst interaktivt, indexera om en tjänst (innebär att uppdatera
solr-indexet för den tjänsten - förändrar ej det skördade datat) eller att indexera om alla tjänster.
Indexoptimering kan också schemaläggas.

Ett simpelt sökgränssnitt finns vilket söker i fältet "text" (fritext) och visar
träffarna som xml och på karta om de har koordinater.

Admin-delen av centralnoden skyddas och användare måste ha rollen "ksamsok" för att få
använda den vilket måste sättas upp i tomcat-konf på vanligt sätt.

SRU-gränssnittet (som nu är deprecerat) nås genom URL:en http://[HOST][:PORT]/ksamsok/sru och i svaret från den URL:en
finns info om vilka index som stödjs mm genom en "SRU explain". Se istället www.ksamsok.se för information om API, tex
http://[HOST][:PORT]/ksamsok/api?method=search&query=text=bil&x-api=test.

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

* Modifierad jrdf-jar (troligen löst med nyare version av jrdf, utvecklaren informerad)
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
