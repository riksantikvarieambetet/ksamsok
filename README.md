# K-samsök

K-samsöks primära kodbas innehållande kod för APIet, URI resolver, skördegränssnitt samt filer för datamodell och auktoriteter.

## Installation

Det går att peka till Solr och Postgres som inte finns lokalt.

 - ksamsok-solr
 - Tomcat 7.0.82
 - Java 1.8
 - Postgres (uppsatt med `repo.postgres.sql`)

### Bygga med Gradle

Vi använder gradle som lämpligen anropas med wrappern `./gradlew` så kör man samma version som är testat och dessutom behöver man inte ha gradle installerat lokalt.

Gradle läser lokala inställningar så som servernamn till Postgres och Solr från `src/main/environment/*` se separat readme i den mappen för instruktion.

Bygg så här:

```
./gradlew war
```

eller för att bygga med en annan miljökonfiguration:

```
./gradlew -Penv=local war
```

Obs! Vill du byta miljö att bygga för måste du köra ./gradlew clean först.

### Installation

Förutsatt att du har satt upp och pekat ut Solr och Postgres instanser:

1. Se till att Tomcats Connector konfiguration i server.xml har `URIEncoding="UTF-8"` satt.
2. Deploya ksamsok war-fil till Tomcat (ta eventuellt bort existerande K-samsök från `webapps`).
3. Starta Tomcat.
4. Om din Postgres och/eller Solr instans är tom så vill du eventuellt först skörda ett nytt dataset (`127.0.0.1:8080/ksamsok/admin/`).

### Gradle under the hood

Gradle-skriptet letar efter environment.properties-filen i rätt environment-katalog, hittar de properties som finns och petar in dem på rätt ställe i rätt fil samtidigt som den bygger war-filen. Detta gör den för att det ska fungera att bygga lokalt. 

## Kör tester

Kör alla tester:

```
./gradlew test
```

Kör specifik testklass:

```
./gradlew test --tests se.raa.ksamsok.lucene.TimeUtilTest
```
