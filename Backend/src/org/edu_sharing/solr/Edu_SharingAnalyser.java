package org.edu_sharing.solr;

import java.io.Reader;
import java.util.Set;

import org.alfresco.repo.search.impl.lucene.analysis.AlfrescoStandardFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ISOLatin1AccentFilter;
import org.apache.lucene.analysis.LengthFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.de.GermanStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class Edu_SharingAnalyser extends Analyzer {

	 private Set stopSet;

    /**
     * An array containing some common German words that are usually not useful for searching.
     */
	public final static String[]  GERMAN_STOP_WORDS = {
    	         "einer", "eine", "eines", "einem", "einen",
    	         "der", "die", "das", "dass", "daß",
    	         "du", "er", "sie", "es",
    	         "was", "wer", "wie", "wir",
    	         "und", "oder", "ohne", "mit",
    	         "am", "im", "in", "aus", "auf",
    	         "ist", "sein", "war", "wird",
    	         "ihr", "ihre", "ihres",
    	         "als", "für", "von", "mit",
    	         "dich", "dir", "mich", "mir",
    	         "mein", "sein", "kein",
    	         "durch", "wegen", "wird"
    	        };

    /** Builds an analyzer. */
    public Edu_SharingAnalyser()
    {
        this(GERMAN_STOP_WORDS);
    }

    /** Builds an analyzer with the given stop words. */
    public Edu_SharingAnalyser(String[] stopWords)
    {
        stopSet = StopFilter.makeStopSet(stopWords);
    }
	
	
	@Override
	public TokenStream tokenStream(String fieldName, Reader reader) {
		TokenStream result = new StandardTokenizer(reader);
        result = new AlfrescoStandardFilter(result);
        result = new LowerCaseFilter(result);
        result = new StopFilter(result, stopSet);
        result = new ISOLatin1AccentFilter(result);
        result = new GermanStemFilter(result,null);
        result = new LengthFilter(result,2,500);
        return result;
	}

}
