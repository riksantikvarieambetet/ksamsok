package se.raa.ksamsok.api.util.parser;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.solr.util.NumberUtils;
import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLNotNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLSortNode;
import org.z3950.zing.cql.CQLTermNode;
import org.z3950.zing.cql.Modifier;

import se.raa.ksamsok.api.exception.BadParameterException;
import se.raa.ksamsok.api.exception.DiagnosticException;
import se.raa.ksamsok.api.util.StaticMethods;
import se.raa.ksamsok.lucene.ContentHelper;
import se.raa.ksamsok.spatial.GMLUtil;

import com.pjaol.lucene.search.SerialChainFilter;
import com.pjaol.search.geo.utils.BoundaryBoxFilter;
import com.pjaol.search.geo.utils.DistanceQuery;

/**
 * Kod mer eller mindre kopierad från LuceneTranslator från 
 * srwlucene 1.0 (http://wiki.osuosl.org/display/OCKPub/SRWLucene)
 * vilken är under apache 2.0-licens. Projektet ej uppdaterat på
 * 2 år och har beroenden på ett projekt (SRW/U 2.0,
 * http://www.oclc.org/research/software/srw/default.htm)
 * som ej heller är uppdaterat på 2 år och till vilket det dessutom
 * inte går att nå källkoden till pga trasig webbplats.
 * 
 * Omskrivet till stor del för att hantera uppdaterad lucene, andra sätt att 
 * göra
 * queries och indexmanipulering samt värdenormalisering mm.
 * 
 * Ändringar gjorda för att passa nytt API
 * 
 * @author Niklas Eklund, Henrik Hjalmarsson
 */
public class CQL2Lucene 
{
	//klass specifik logger
	private static final Logger logger = 
		Logger.getLogger("se.raa.ksamsok.sru.CQL2Lucene");

	private static final String INDEX_CQL_SERVERCHOICE = "cql.serverChoice";
	private static final String INDEX_CQL_RESULTSETID = "cql.resultSetId";

	private static final Query NO_MATCH_DUMMY_QUERY = new TermQuery(new Term("dumdummy", "dummy"));

	/**
	 * Skapar en lucene-query utifrån en CQL-nod.
	 * 
	 * @param node cql-nod
	 * @return lucene-query eller null
	 */
	public static Query makeQuery(CQLNode node)
		throws DiagnosticException, BadParameterException
	{
		return makeQuery(node, null);
	}

	/**
	 * Skapar en lucene-query utifrån en CQL-nod och ett vänsterled.
	 * 
	 * @param node cql-nod
	 * @param leftQuery vänsterled i form av en lucene-query eller null
	 * @return en lucene-query eller null
	 */
	static Query makeQuery(CQLNode node, Query leftQuery)
		throws DiagnosticException, BadParameterException
	{
		Query query = null;

		if(node instanceof CQLBooleanNode) 
		{
			CQLBooleanNode cbn=(CQLBooleanNode)node;

			Query left = makeQuery(cbn.left);
			Query right = makeQuery(cbn.right, left);

			if(node instanceof CQLAndNode) 
			{
					query = new BooleanQuery();
					if (logger.isDebugEnabled()) 
					{
						logger.debug("  Anding left and right in new query");
					}
					AndQuery((BooleanQuery) query, left);
					AndQuery((BooleanQuery) query, right);

			} else if(node instanceof CQLNotNode) 
			{
					query = new BooleanQuery();
					if (logger.isDebugEnabled()) 
					{
						logger.debug("  Notting left and right in new query");
					}
					AndQuery((BooleanQuery) query, left);
					NotQuery((BooleanQuery) query, right);

			} else if(node instanceof CQLOrNode) 
			{
					if (logger.isDebugEnabled()) 
					{
						logger.debug("  Or'ing left and right in new query");
					}
					query = new BooleanQuery();
					OrQuery((BooleanQuery) query, left);
					OrQuery((BooleanQuery) query, right);
			} else 
			{
				throw new BadParameterException("okänd boolesk operation",
						"CQL2Lucene.makeQuery", node.toString(), true);
			}

		} else if(node instanceof CQLTermNode) 
		{
			CQLTermNode ctn=(CQLTermNode)node;

			String relation = ctn.getRelation().getBase();
			String index = translateIndexName(ctn.getIndex());

			if (!index.equals("")) 
			{
				String term = ctn.getTerm();
				// result sets stöds ej
				if (INDEX_CQL_RESULTSETID.equals(index)) 
				{
					throw new DiagnosticException("Resultat sett stöds ej",
							"CQL2Lucene.makeQuery", "unsupported", false);
				}
				// anchoring (position av värde i indexerat fält) stöds ej
				if (term.indexOf('^') != -1) 
				{
					throw new DiagnosticException("ankra tecken stöds ej",
							"CQL2Lucene.makeQuery", "unsupported", false);
				}
				// hantera virtuellt index
				if (ContentHelper.isSpatialVirtualIndex(index)) 
				{
					query = createSpatialQuery(index, ctn);
				} else if (relation.equals("=") || relation.equals("scr")) 
				{
					query = createTermQuery(index,term, relation);
				} else if (relation.equals("<")) 
				{
					//term is upperbound, exclusive
					term = transformValueForField(index, term);
					// csrq istället för en range query då den inte ger 
					// TooManyClauses
					// TODO: behöver vi använda cache-wrapper 
					// (CachingWrapperFilter) för dessa?
					query = new ConstantScoreRangeQuery(index, null, term,
							false, false);
				} else if (relation.equals(">")) 
				{
					//term is lowerbound, exclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, term, null,
							false, false);
				} else if (relation.equals("<=")) 
				{
					//term is upperbound, inclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, null, term,
							false, true);
				} else if (relation.equals(">=")) 
				{
					//term is lowebound, inclusive
					term = transformValueForField(index, term);
					query = new ConstantScoreRangeQuery(index, term, null,
							true, false);
				} else if (relation.equals("<>")) 
				{
					/**
					 * <> is an implicit NOT.
					 *
					 * For example the following statements are identical 
					 * results:
					 *   foo=bar and zoo<>xar
					 *   foo=bar not zoo=xar
					 */

					if (leftQuery == null) 
					{
						// first term in query create an empty Boolean query
						// to NOT
						query = new BooleanQuery();
					} else 
					{
							query = new BooleanQuery();
							AndQuery((BooleanQuery)query, leftQuery);
					}
					//create a term query for the term then NOT it to the
					// boolean query
					Query termQuery = createTermQuery(index,term, relation);
					NotQuery((BooleanQuery) query, termQuery);

				} else if (relation.equals("any")) 
				{
					//implicit or
					query = createTermQuery(index,term, relation);

				} else if (relation.equals("all")) 
				{
					//implicit and
					query = createTermQuery(index,term, relation);
				} else if (relation.equals("exact")) 
				{
					/**
					 * implicit and.  this query will only return accurate
					 * results for indexes that have been indexed using
					 * a non-tokenizing analyzer
					 */
					query = createTermQuery(index,term, relation);
				} else 
				{
					//anything else is unsupported
					throw new DiagnosticException("relationen " + 
							ctn.getRelation().getBase() + " stöds ej",
							"CQL2Lucene.makeQuery",
							ctn.getRelation().getBase() + " stöds ej", true);
				}
			}
		} else if(node instanceof CQLSortNode) 
		{
			throw new DiagnosticException("sortering stöds ej",
					"CQL2Lucene.makeQuery", null, false);
		} else 
		{
			throw new DiagnosticException("okänt fel uppstod",
					"CQL2Lucene.makeQuery", "error: " + 47 + " - okänd CQL "
					+ "nod: " + ""+node+")", true);
		}
		if (query != null && logger.isDebugEnabled()) 
		{
			// för att klara de specialsträngar som krävs för ISO-datum
			// utan att skriva ut låga kontrolltecken men ändå nån info per 
			// tecken
			// kontrolltecknen kan tex ge högtalarpip på windows
			String q = query.toString();
			StringBuffer b = new StringBuffer(q.length());
			for (char c: q.toCharArray()) 
			{
				if (c < 32) 
				{
					b.append('#');
				} else 
				{
					b.append(c);
				}
			}
			logger.debug("Query : " + b);
		}
		return query;
	}

	/**
	 * Skapar en term-query utifrån inskickade värden.
	 * 
	 * @param field index
	 * @param value värde
	 * @param relation relation
	 * @return lucene-query eller null
	 * @throws Exception
	 */
	public static Query createTermQuery(String field, String value,
			String relation)
		throws DiagnosticException {
		// detta api ska alltid vara lenient map stoppord enl önskemål från kringla
		return createTermQuery(field, value, relation, true);
	}

	/**
	 * Skapar en term-query utifrån inskickade värden.
	 * 
	 * @param field index
	 * @param value värde
	 * @param relation relation
	 * @param lenientOnStopwords om stoppord ej ska kasta fel
	 * @return lucene-query eller null
	 * @throws Exception
	 */
	public static Query createTermQuery(String field, String value,
			String relation, boolean lenientOnStopwords)
		throws DiagnosticException
	{
		Query termQuery = null;

		/**
		 * check to see if there are any spaces.  If there are spaces each
		 * word must be broken into a single term search and then all queries
		 * must be combined using an and.
		 */
		// för ej analyserade fält vill vi tillåta mellanslag i termerna, 
		// typ "Stockholm 1:1"
		if (value.indexOf(" ") == -1 ||	!ContentHelper.isAnalyzedIndex(field))
		{
			// inga mellanslag, skapa en term query eller wildcard query
			Term term;
			if (value.indexOf("?") != -1 || value.indexOf("*")!= -1)
			{
				if (ContentHelper.isToLowerCaseIndex(field)) 
				{
					// gör till gemener
					value = value.toLowerCase();
				} else if (ContentHelper.isISO8601DateYearIndex(field) ||
						ContentHelper.isSpatialCoordinateIndex(field)) 
				{
					// inget stöd för wildcards för dessa fält
					throw new DiagnosticException("Wildcard tecken stöds ej" +
							" för index: " + field,
							"CQL2Lucene.createTermQuery", null, true);
				}
				
				term = new Term(field, value);
				termQuery = new WildcardQuery(term);
			} else 
			{
				// fixa ev till värdet beroende på index
				value = transformValueForField(field, value, lenientOnStopwords);
				if (value != null) {
					term = new Term(field, value);
					termQuery = new TermQuery(term);
				} else {
					termQuery = NO_MATCH_DUMMY_QUERY;
				}
			}
		} else 
		{
			// space found, iterate through the terms to create a multiterm 
			//search
			if (relation == null || relation.equals("=") ||
					relation.equals("<>") || relation.equals("exact")) 
			{
				/**
				 * default is =, all terms must be next to eachother.
				 * <> uses = as its term query.
				 * exact is a phrase query
				 */
				if (value.indexOf("?") != -1 || value.indexOf("*")!=-1 ) 
				{
					throw new DiagnosticException("Wildcard tecken stöds ej" +
							" för detta query", "CQL2Lucene.createTermQuery",
							"wildcard tecken stöds ej:" + field, true);
				}
				PhraseQuery phraseQuery =
					(PhraseQuery) StaticMethods.analyseQuery(field,
						value);
				Term[] t = phraseQuery.getTerms();
				if (t == null || t.length == 0) 
				{
					if (!lenientOnStopwords) {
						throw new DiagnosticException("fel i query sträng",
								"CQL2Lucene.createTermQuery", "endast stopp " +
										"ord: " + field, true);
					} else {
						termQuery = NO_MATCH_DUMMY_QUERY;
					}
				} else {
					termQuery = phraseQuery;
				}
			} else if(relation.equals("any")) 
			{
				/**
				 * any is an implicit OR
				 */
				termQuery = new BooleanQuery();
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				while (tokenizer.hasMoreTokens()) 
				{
					String curValue = tokenizer.nextToken();
					Query subSubQuery = createTermQuery(field, curValue,
							relation, true);
					if (subSubQuery != NO_MATCH_DUMMY_QUERY) {
						OrQuery((BooleanQuery) termQuery, subSubQuery);
					}
				}

			} else if (relation.equals("all")) 
			{
				/**
				 * any is an implicit AND
				 */
				termQuery = new BooleanQuery();
				StringTokenizer tokenizer = new StringTokenizer(value, " ");
				while (tokenizer.hasMoreTokens()) 
				{
					String curValue = tokenizer.nextToken();
					Query subSubQuery = createTermQuery(field, curValue,
							relation, true);
					if (subSubQuery != NO_MATCH_DUMMY_QUERY) {
						AndQuery((BooleanQuery) termQuery, subSubQuery);
					}
				}
			} else 
			{
				throw new DiagnosticException("relationen " + relation +
						" stöds ej", "CQL2Lucene.createTermQuery",
						"relationen " + relation + " stöds ej för phrase" +
								" query", true);
			}

		}

		return termQuery;
	}

	/**
	 * Skapar en spatial-query för lucene för ett virtuellt spatialt index.
	 * Queryn blir olika beroende på index.
	 * @param index indexnamn
	 * @param ctn termnod
	 * @return query
	 * @throws DiagnosticException vid problem
	 */
	private static Query createSpatialQuery(String index, CQLTermNode ctn) 
		throws DiagnosticException 
	{
		Query query;
		String relation = ctn.getRelation().getBase();
		if (!"=".equals(relation)) 
		{
			throw new DiagnosticException("kombinationen av relation och " +
					"index stöds ej: " + index + " " + relation,
					"CQL2Lucene.createSpatialQuery", null, true);
		}
		String term = ctn.getTerm();
		if (ContentHelper.IX_BOUNDING_BOX.equals(index)) 
		{
			double[] coords = parseDoubleValues(term, 4);
			if (coords == null) 
			{
				throw new DiagnosticException("Termen har ogiltligt format" +
						" för index eller relation: " + index + ": " + term,
						"CQL2Lucene.createSpatialQuery", null, true);
			}
			
			String epsgIdent = getSingleModifier(ctn);
			epsgIdent = translateEPSGModifier(epsgIdent);
			coords = transformCoordsToWGS84(epsgIdent, coords);
			// gör pss som locallucene, men utan påtvingad fyrkant
			// (dvs vi tillåter rektanglar)
			BoundaryBoxFilter latFilter = 
				new BoundaryBoxFilter(ContentHelper.I_IX_LAT,
						NumberUtils.double2sortableStr(coords[1]),
						NumberUtils.double2sortableStr(coords[3]), true,
						true);
			BoundaryBoxFilter lngFilter = 
				new BoundaryBoxFilter(ContentHelper.I_IX_LON,
						NumberUtils.double2sortableStr(coords[0]),
						NumberUtils.double2sortableStr(coords[2]),
						true, true);
			query = new ConstantScoreQuery(
					new SerialChainFilter(new Filter[] {latFilter,
							lngFilter},
                     new int[] {SerialChainFilter.AND,
							SerialChainFilter.AND}));
		} else if (ContentHelper.IX_POINT_DISTANCE.equals(index)) 
		{
			double[] coordsAndDist = parseDoubleValues(term, 3);
			if (coordsAndDist == null) 
			{
				throw new DiagnosticException("Termen har ogiltligt format" +
						" för index eller relation: " + index + ": " + term,
						"CQL2Lucene.createSpatialQuery", null, true);
			}
			String epsgIdent = getSingleModifier(ctn);
			epsgIdent = translateEPSGModifier(epsgIdent);
			// hämta ut punkten
			double[] point = { coordsAndDist[0], coordsAndDist[1] };
			// och hämta distansen och konvertera den till miles för
			// locallucenes DistanceQuery
			double distInKm = coordsAndDist[2];
			double distInMiles =  distInKm * 0.621371;
			if (distInMiles < 1) 
			{
				// locallucene ger fel om distansvärdet är för litet så vi
				// kontrollerar här först
				throw new DiagnosticException("Termen har ogiltligt format" +
						" för index eller relation: avstånd för litet",
						"CQL2Lucene.createSpatialQuery", "distans " +
						distInMiles + " för liten", true);
			} else if (distInKm > 30) 
			{
				// locallucene cachar upp en massa data för punkt + distans
				// så det här värdet
				// får definitivt inte vara för stort heller, då riskerar vi
				// oom - 30km kanske är ok?
				throw new DiagnosticException("Term har ogiltigt format för" +
						" index eller relation: avstånd för stort",
						"CQL2Lucene.createSpatialQuery", "distans " +
						distInKm + " för stor", true);
			}
			point = transformCoordsToWGS84(epsgIdent, point);
			query = new DistanceQuery(point[1], point[0], distInMiles,
					ContentHelper.I_IX_LAT,	ContentHelper.I_IX_LON,
					true).getQuery();
		} else 
		{
			// Hmm, det här borde inte hända
			throw new DiagnosticException("index " + index + " stöds ej",
					"CQL2Lucene.createSpatialQuery", null, false);
		}
		return query;
	}

	/**
	 * Join the two queries together with boolean AND
	 * @param query
	 * @param query2
	 */
	public static void AndQuery(BooleanQuery query, Query query2) 
	{
		/**
		 * required = true (must match sub query)
		 * prohibited = false (does not need to NOT match sub query)
		 */
		query.add(query2, BooleanClause.Occur.MUST);
	}

	public static void OrQuery(BooleanQuery query, Query query2) 
	{
		/**
		 * required = false (does not need to match sub query)
		 * prohibited = false (does not need to NOT match sub query)
		 */
		query.add(query2, BooleanClause.Occur.SHOULD);
	}

	public static void NotQuery(BooleanQuery query, Query query2) 
	{
		/**
		 * required = false (does not need to match sub query)
		 * prohibited = true (must not match sub query)
		 */
		query.add(query2, BooleanClause.Occur.MUST_NOT);
	}

	/**
	 * Översätter indexnamn från cql till de som lucene använder internt.
	 * Ger default-indexnamn och strippar samsökskontext. 
	 * 
	 * @param index index
	 * @return översatt indexnamn
	 */
	public static String translateIndexName(String index) 
	{
		// TODO: toLowerCase() ?
		String translatedIndex = index;
		// översätt default-index till fritext
		if (INDEX_CQL_SERVERCHOICE.equals(index)) 
		{
			translatedIndex = ContentHelper.IX_TEXT;
		} else if (index.indexOf('.') > 0 &&
				index.startsWith(ContentHelper.CONTEXT_SET_SAMSOK)) 
		{
			// strippar samsok-contextet eftersom det är default
			translatedIndex = index.substring(
					ContentHelper.CONTEXT_SET_SAMSOK.length() + 1);
		}
		return translatedIndex;
	}

	// analyserar (stammar) inskickad text med svensk analyzer
	public static String analyzeIndexText(String text) 
	{
		Analyzer a = ContentHelper.getSwedishAnalyzer();
		TokenStream ts = null;
		String retText = text;
		try 
		{
			ts = a.tokenStream(null, new StringReader(text));
			Token t = new Token();
			t = ts.next(t);
			if (t != null) 
			{
				retText = t.term();
				if ((t = ts.next(t)) != null) 
				{
					// sätt tillbaka
					retText = text;
				}
			} else 
			{
				retText = null;
			}
		} catch (Exception e) 
		{
			logger.warn("Fel vid analys av index-text", e);
		} finally 
		{
			if (ts != null) 
			{
				try 
				{
					ts.close();
				} catch (IOException e) 
				{
					logger.warn("Fel vid stängning av tokenström vid " +
							"analys av index-text", e);
				}
			}
		}
		return retText;
	}

	// "översätter" ev värde beroende på indextyp
	public static String transformValueForField(String field, String value)
		throws DiagnosticException {
		return transformValueForField(field, value, false);
	}

	// "översätter" ev värde beroende på indextyp
	public static String transformValueForField(String field, String value, boolean lenientOnStopwords)
		throws DiagnosticException 
	{
		// använd svensk stamning, samma som för indexeringen
		// beroende på fält/index om vi ska stamma eller ej
		if (ContentHelper.isAnalyzedIndex(field)) 
		{
			String analyzedValue = analyzeIndexText(value);
			if (!lenientOnStopwords && analyzedValue == null)
			{
				throw new DiagnosticException("felaktigt värde",
						"CQL2Lucene.transformValueForField", "term " +
								"innehåller endast stoppord: " + value,
								true);
			}
			value = analyzedValue;
		} else if (ContentHelper.isToLowerCaseIndex(field)) 
		{
			value = value.toLowerCase();
		} else if (ContentHelper.isISO8601DateYearIndex(field)) 
		{
			// gör om år till för lucene tillfixad sträng så att intervall
			// mm stöds
			try 
			{
				value = ContentHelper.transformNumberToLuceneString(
						parseYear(value));
			} catch (Exception e) 
			{
				throw new DiagnosticException("Term har ogiltigt format för" +
						" index eller relation",
						"CQL2Lucene.transformValueForField", "ogiltigt " +
								"format: " + field + ": " + value, true);
			}
		} else if (ContentHelper.isSpatialCoordinateIndex(field)) 
		{
			// gör om till för lucene tillfixad sträng så att intervall mm
			// stöds
			// lon/lat lagras som double och värdena här måste vara double
			try 
			{
				value = ContentHelper.transformNumberToLuceneString(
						Double.parseDouble(value));
			} catch (Exception e) 
			{
				throw new DiagnosticException("Term har ogiltigt format för" +
						" index eller relation",
						"CQL2Lucene.transformValueForField", "ogiltigt " +
								"värde: " + field + ": " + value, true);
			}
		}
		return value;
	}

	private static int parseYear(String value) 
	{
		return Integer.parseInt(value);
	}

	private static double[] parseDoubleValues(String value, int expected) 
	{
		double[] values = null;
		StringTokenizer tok = new StringTokenizer(value, " ");
		if (tok.countTokens() == expected) 
		{
			values = new double[expected];
			int i = 0;
			while (tok.hasMoreTokens()) 
			{
				try 
				{
					values[i] = Double.parseDouble(tok.nextToken());
					++i;
				} catch (Exception e) 
				{
					values = null;
					break;
				}
			}
		}
		return values;
	}

	private static String getSingleModifier(CQLTermNode ctn)
		throws DiagnosticException 
	{
		String singleModifier = null;
		List<Modifier> modifiers = ctn.getRelation().getModifiers();
		if (modifiers.size() > 1) 
		{
			String diagData = modifiers.get(0).getType();
			for (int j = 1; j < modifiers.size(); ++j) 
			{
				diagData += "/" + modifiers.get(j).getType();
			}
			throw new DiagnosticException("Kombinationen av relations " +
					"modifierare stöds ej", "CQL2Lucene.getSingleModifier",
					"kombinationen stöds ej: " + diagData, true);
		} else if (modifiers.size() == 1) 
		{
			singleModifier = modifiers.get(0).getType().toUpperCase();
		}
		return singleModifier;
	}

	private static double[] transformCoordsToWGS84(String fromCRS,
			double[] coords)
		throws DiagnosticException 
	{
		double[] xformedCoords = coords;
		if (fromCRS != null && !GMLUtil.CRS_WGS84_4326.equals(fromCRS)) 
		{
			if (coords == null || coords.length == 0) 
			{
				throw new DiagnosticException("Term har ogiltigt format för" +
						" index eller relation: Inga kordinater",
						"CQL2Lucene.transformCoordsToWGS84", null, false);
			}
			try 
			{
				xformedCoords = GMLUtil.transformCRS(coords, fromCRS,
						GMLUtil.CRS_WGS84_4326);
			} catch (Exception e) 
			{
				throw new DiagnosticException("Relations modifierare stöds" +
						" ej", "CQL2Lucene.transformCoordsToWGS84",
						"relationsmodifierare stöds ej: " + fromCRS, true);
			}
			if (logger.isDebugEnabled()) 
			{
				logger.debug("Transformerade koordinater från " + fromCRS +
						" till WGS 84 (" + ArrayUtils.toString(coords) +
						" -> " + ArrayUtils.toString(xformedCoords) + ")");
			}
		}
		return xformedCoords;
	}

	private static String translateEPSGModifier(String epsgIdent) 
	{
		if (epsgIdent != null) 
		{
			// översätt kända konstanter till epsg-koder
			if (ContentHelper.WGS84_4326.equalsIgnoreCase(epsgIdent)) 
			{
				epsgIdent = GMLUtil.CRS_WGS84_4326;
			} else if (
					ContentHelper.SWEREF99_3006.equalsIgnoreCase(epsgIdent)) 
			{
				epsgIdent = GMLUtil.CRS_SWEREF99_TM_3006;
			} else if (ContentHelper.RT90_3021.equalsIgnoreCase(epsgIdent)) 
			{
				epsgIdent = GMLUtil.CRS_RT90_3021;
			}
		} else 
		{
			// default är SWEREF99 så vi måste alltid transformera då
			// koordinater lagras i wgs84
			epsgIdent = GMLUtil.CRS_SWEREF99_TM_3006;
		}
		return epsgIdent;
	}

	// bara för debug, dumpar cql-trädet
	static void dumpQueryTree(CQLNode node) 
	{
		if (!logger.isDebugEnabled()) 
		{
			return;
		}
		if (node instanceof CQLBooleanNode) 
		{
			CQLBooleanNode cbn=(CQLBooleanNode)node;
			dumpQueryTree(cbn.left);
			if (node instanceof CQLAndNode) 
			{
				logger.debug(" AND ");
			} else if (node instanceof CQLNotNode) 
			{
				logger.debug(" NOT ");
			} else if (node instanceof CQLOrNode) 
			{
				logger.debug(" OR ");
			} else 
			{
				logger.debug(" UnknownBoolean("+cbn+") ");
			}
			dumpQueryTree(cbn.right);
		} else if (node instanceof CQLTermNode) 
		{
			CQLTermNode ctn=(CQLTermNode)node;
			logger.debug("term(qualifier=\""+ctn.getIndex()+"\" relation=\""+
					ctn.getRelation().getBase()+"\" term=\""+ctn.getTerm()+
					"\")");
		} else 
		{
			logger.debug("UnknownCQLNode("+node+")");
		}
	}
}