package se.raa.ksamsok.lucene;


/**
 * Interface för klasser för specialhantering av relationer i form av översättning av
 * URI:er till indexnamn/relationstypnamn.
 */
public interface RelationToIndexMapper {

	/**
	 * Ger ett relationstypnamn (typiskt ett indexnamn) för en relations-URI.
	 * @param refUri URI
	 * @return relationstypnamn eller null om URI:n inte kunde tolkas/översättas. 
	 */
	String getRelationTypeNameFromURI(String refUri);
}
