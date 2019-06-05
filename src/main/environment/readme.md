Den här katalogen innehåller filer för att sätta upp olika miljöer

Beskrivning över underkataloger

 * buildserver - Har ligger propertyfil för att se till att filerna innehåller rätt taggar för byggservern att ersätta
 * common - Här ligger filerna som innehåller taggar som ska ersättas för de olika miljöerne
 * default - Här ligger propertyfil med defaultvärden som används om man inte anger någon environment till byggskriptet
 * local - Här kan man lägga en egen propertyfil för lokal utvecklingsmiljö. Propertyfilen måste kopieras i sin helhet 
   från default-katalogen och modifieras, men kan inte overrida bara några utvalda properties
