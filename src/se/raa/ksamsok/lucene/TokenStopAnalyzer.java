package se.raa.ksamsok.lucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;

import java.io.Reader;
import java.util.Set;

public class TokenStopAnalyzer extends Analyzer {

            private Set stopSet;

            /** Builds the named analyzer with the given stop words. */
            public TokenStopAnalyzer(String[] stopWords) {
                stopSet = StopFilter.makeStopSet(stopWords);
            }

            /** Constructs a StandardTokenizer filtered by a StandardFilter, a LowerCaseFilter and a StopFilter. */
            public TokenStream tokenStream(String fieldName, Reader reader) {
                TokenStream result = new StandardTokenizer(reader);
                result = new StandardFilter(result);
                result = new LowerCaseFilter(result);
                if (stopSet != null)
                    result = new StopFilter(result, stopSet);
                return result;
            }
}
