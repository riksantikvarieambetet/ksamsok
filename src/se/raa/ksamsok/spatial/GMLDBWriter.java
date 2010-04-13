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
	 * @throws Exception vid problem
	 */
	void init(String serviceId, Connection c) throws Exception;

	/**
	 * Frigör eventuella resurser instansen håller.
	 */
	void destroy();

	/**
	 * Anropas vid insert av post.
	 * @param gmlInfoHolder gmldatahållare
	 * @return antal inlagda geometrier
	 * @throws Exception vid fel
	 */
	int insert(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas vid update av post.
	 * @param gmlInfoHolder gmldatahållare
	 * @return antal ändringar (inlagda + borttagna)
	 * @throws Exception vid fel
	 */
	int update(GMLInfoHolder gmlInfoHolder) throws Exception;

	/**
	 * Anropas när en post tas bort.
	 * @param identifier uri
	 * @return antal borttagna
	 * @throws Exception vid fel
	 */
	int delete(String identifier) throws Exception;

	/**
	 * Anropas för att ta bort alla geometrier i databasen för tjänsten.
	 * Används enbart i samband med rensning av repository för en tjänst.
	 * @throws Exception
	 */
	int deleteAllForService() throws Exception;

}
