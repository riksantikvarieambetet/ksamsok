package se.raa.ksamsok.spatial;

import java.sql.Connection;

/**
 * Interface för att hantera skrivning av spatial-data i form av gml till databas.
 * Instanser skapas med default-konstruktorn och init() anropas innan någon av de andra
 * metoderna körs. Se också {@linkplain GMLUtil#getGMLDBWriter(String, Connection)}.
 * Alla gml-geometrier är konverterade till SWEREF 99 TM (EPSG:3006) innan de kommer
 * in till instanser av GMLDBWriter.
 */
public interface GMLDBWriter {

	/**
	 * Initierar instansen med värden för aktuell tjänst och databas.
	 * @param serviceId id för tjänsten
	 * @param c en uppkoppling mot databasen
	 */
	void init(String serviceId, Connection c);

	/**
	 * Anropas vid insert av ny post i repositoriet (nytt lucenedokument).
	 * @param gmlInfoHolder gmldatahållare
	 * @throws Exception vid fel
	 */
	void insert(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas vid update av ny post i repositoriet (nytt lucenedokument).
	 * @param gmlInfoHolder gmldatahållare
	 * @throws Exception vid fel
	 */
	void update(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas när en post tas bort ur repositoriet.
	 * @param oaiURI oaiURI
	 * @throws Exception vid fel
	 */
	void delete(String oaiURI) throws Exception;

	/**
	 * Anropas vid rensning av alla poster för denna tjänst.
	 * @throws Exception vid fel
	 */
	void deleteAllForService() throws Exception;

}
