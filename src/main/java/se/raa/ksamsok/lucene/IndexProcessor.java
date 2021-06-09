package se.raa.ksamsok.lucene;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Klass som hanterar indexprocessning (översättning av värden, hur de
 * lagras av lucene etc).
 */
class IndexProcessor {
	final Map<String,String> uriValues;
	final SolrInputDocument doc;
	String[] indexNames;
	boolean lookupURI;
	RelationToIndexMapper relationHandler;
	IndexProcessor(SolrInputDocument doc, Map<String,String> uriValues, RelationToIndexMapper relationHandler) {
		this.doc = doc;
		this.uriValues = uriValues;
		this.relationHandler = relationHandler;
	}

	/**
	 * Sätter vilket index vi jobbar med fn, och ev också ett prefix för att hantera
	 * kontext-index samt om uris ska slås upp. Värdet i indexet kommer också lagras i ett kontext-index.
	 * 
	 * @param indexName indexnamn
	 * @param contextPrefix kontext-prefix eller null
	 * @param lookupURI om urivärde ska slås upp
	 * @param extraIndexName ev extra index värdet ska in i, eller null
	 */
	void setCurrent(String indexName, String [] contextPrefix, boolean lookupURI, String extraIndexName) {
		ArrayList<String> indexList = new ArrayList<>();
		indexList.add(indexName);
		if (contextPrefix != null) {
			for (String prefix: contextPrefix) {
				indexList.add(prefix + "_" + indexName);
			}
		}
		if (extraIndexName != null) {
			indexList.add(extraIndexName);
		}
		setCurrent(indexList.toArray(new String[0]), lookupURI);
	}

	/**
	 * Sätter vilket index vi jobbar med fn, och ev också ett prefix för att hantera
	 * kontext-index. Värdet i indexet kommer också lagras i ett kontext-index.
	 * 
	 * @param indexName indexnamn
	 * @param contextPrefixes kontext-prefix eller null
	 */
	void setCurrent(String indexName, String[] contextPrefixes) {
		setCurrent(indexName, contextPrefixes, true, null);
	}

	/**
	 * Sätter vilket index vi jobbar med fn.
	 * 
	 * @param indexName indexnamn
	 */
	void setCurrent(String indexName) {
		setCurrent(indexName, true);
	}

	/**
	 * Sätter vilket index vi jobbar med fn, hur/om det ska lagras av lucene och
	 * om uri-värden ska slås upp.
	 * 
	 * @param indexName indexnamn
	 * @param lookupURI om urivärde ska slås upp
	 */
	void setCurrent(String indexName, boolean lookupURI) {
		setCurrent(new String[] {indexName }, lookupURI);
	}

	/**
	 * Sätter vilka index vi jobbar med fn, hur/om dessa ska lagras av lucene, vilken
	 * typ (lucene) av index dessa är och om uri-värden ska slås upp.
	 * 
	 * @param indexNames indexnamn
	 * @param lookupURI om värden ska slås upp
	 */
	void setCurrent(String[] indexNames, boolean lookupURI) {
		this.indexNames = indexNames;
		this.lookupURI = lookupURI;
	}

	/**
	 * Lägger till värdet till lucenedokumentet för aktuellt index. Värdet läggs till
	 * för både huvudindexet och ev extraindex satta med tex
	 * {@linkplain #setCurrent(String[], boolean)}.
	 * 
	 * @param value värde
	 */
	void addToDoc(String value) {
		for (String indexName : indexNames) {
			addToDoc(indexName, value);
		}
	}

	/**
	 * Ger om uri-värden ska slås upp för aktuellt index.
	 * 
	 * @return sant om uri-värden ska slås upp
	 */
	boolean translateURI() {
		return lookupURI;
	}

	/**
	 * Slår ev upp ett urivärde och hanterar speciallistan med relationer. 
	 * 
	 * @param uri uri
	 * @param relations speciallista med relationer som fylls på, eller null
	 * @param refUri relations-uri, eller null
	 * @return uppslaget värde, eller uri:n
	 * @throws Exception vid fel
	 */
	public String lookupAndHandleURIValue(String uri, List<String> relations, String refUri) throws Exception {
		String value;
		String lookedUpValue;
		// se om vi ska försöka ersätta uri:n med en uppslagen text
		if (translateURI() && (lookedUpValue = lookupURIValue(uri)) != null) {
			value = lookedUpValue;
		} else {
			value = StringUtils.trimToNull(uri);
		}
		if (value != null && refUri != null && relations != null) {
			String relationType = relationHandler.getRelationTypeNameFromURI(refUri);
			if (relationType == null) {//!relationHandler.handleURIs(refUri, uri, relations)) {
				throw new Exception("Okänd/ej hanterad relation? Börjar ej med känt prefix: " + refUri);
			}
			// kontrollera indexnamnet (relationstypen är i praktiken samma som indexnamnet)
			if (!ContentHelper.indexExists(relationType)) {
				throw new Exception("Relationen med uri " + refUri + " översattes till " +
						relationType + ", vilket inte är ett index");
			}
			relations.add(relationType + "|" + value);
			// TODO: refUri/uri, är det ok/bra? fixa överlagring av dessa för olika protokollversioner
			//       eller nåt sånt...
			// specialhantering av relationer
//			String relationType;
//			if (SamsokProtocol.uri_rSameAs.toString().equals(refUri)) {
//				relationType = "sameAs";
//			} else {
//				relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefixKSamsok));
//				// TODO: fixa bättre
//				if (relationType == null) {
//					// testa cidoc
//					relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_cidoc_crm));
//					if (relationType != null) {
//						// strippa sifferdelen
//						relationType = StringUtils.trimToNull(StringUtils.substringAfter(relationType, "."));
//					} else {
//						// bios
//						relationType = StringUtils.trimToNull(StringUtils.substringAfter(refUri, SamsokProtocol.uriPrefix_bio));
//					}
//				}
//				if (relationType == null) {
//					throw new Exception("Okänd relation? Börjar ej med känt prefix: " + refUri);
//				}
//			}
//			relations.add(relationType + "|" + value);
		}
		return value;
	}

	// försöker slå upp uri-värde mha mängden inlästa värden
	// för geografi-uri:s tas fn bara koden (url-suffixet)
	// om ett värde inte hittas lagras det i "problem"-loggen
	private String lookupURIValue(String uri) {
		String value = uriValues.get(uri);
		// TODO: padda med nollor eller strippa de med inledande nollor?
		//       det kommer ju garanterat bli fel i någon ända... :)
		//       UPDATE: ser ut som om det är strippa nollor som gäller då
		//       cql-parsern(?) verkar tolka saker som siffror om de inte quotas
		//       med "", ex "01"
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_county_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_municipality_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_province_pre);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_parish_pre, true);
		if (value != null) {
			return value;
		}
		value = restIfStartsWith(uri, SamsokProtocol.aukt_country_pre);
		if (value != null) {
			return value;
		}
		// lägg in i thread local som ett problem
		ContentHelper.addProblemMessage("No value for " + uri);
		return null;
	}

	// hjälpmetod som tar ut suffixet ur strängen om den startar med inskickad startsträng
	private static String restIfStartsWith(String str, String start) {
		return restIfStartsWith(str, start, false);
	}

	// hjälpmetod som tar ut suffixet ur strängen om den startar med inskickad startsträng
	// och försöker tolka värdet som ett heltal om asNumber är sant
	private static String restIfStartsWith(String str, String start, boolean asNumber) {
		String value = null;
		if (str.startsWith(start)) {
			value = str.substring(start.length());
			if (asNumber) {
				try {
					value = Long.valueOf(value).toString();
				} catch (NumberFormatException nfe) {
					ContentHelper.addProblemMessage("Could not interpret the end of " + str + " (" + value + ") as a digit");
				}
			}
		}
		return value;
	}

	// lägger till ett index till solr-dokumentet
	boolean addToDoc(String fieldName, String value) {
		String trimmedValue = StringUtils.trimToNull(value);
		if (trimmedValue != null) {
			/*
			if (isToLowerCaseIndex(fieldName)) {
				trimmedValue = trimmedValue.toLowerCase();
			} else */
			// TODO: göra detta på solr-sidan?
			if (ContentHelper.isISO8601DateYearIndex(fieldName)) {
				trimmedValue = TimeUtil.parseYearFromISO8601DateAndTransform(trimmedValue);
				if (trimmedValue == null) {
					ContentHelper.addProblemMessage("Could not interpret date value according to ISO8601 for field: " +
							fieldName + " (" + value + ")");
				}
			}
			if (trimmedValue != null) {
				doc.addField(fieldName, trimmedValue);
			}
		}
		return trimmedValue != null;
	}

}