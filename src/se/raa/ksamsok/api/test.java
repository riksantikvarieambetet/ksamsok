package se.raa.ksamsok.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.WildcardQuery;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import se.raa.ksamsok.api.method.APIMethod;
import se.raa.ksamsok.lucene.LuceneServlet;

public class test implements APIMethod
{
	String index;
	String queryString;
	PrintWriter writer;
	
	public test(String index, String queryString, PrintWriter writer)
	{
		this.index = index;
		this.queryString = queryString;
		this.writer = writer;
	}

	@Override
	public void performMethod() throws MissingParameterException,
			BadParameterException, DiagnosticException
	{
		IndexSearcher searcher = 
			LuceneServlet.getInstance().borrowIndexSearcher();;
		try
		{
		}finally
		{
			LuceneServlet.getInstance().returnIndexSearcher(searcher);
		}
	}

}
