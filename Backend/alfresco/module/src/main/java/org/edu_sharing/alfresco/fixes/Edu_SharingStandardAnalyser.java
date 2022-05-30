package org.edu_sharing.alfresco.fixes;

import java.io.Reader;
import java.util.Set;

import org.alfresco.repo.search.impl.lucene.analysis.AlfrescoStandardFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;

/**
 * 
 * @author rudi
 *
 *  this class is nearly the same as the
 *  org.alfresco.repo.search.impl.lucene.analysis.AlfrescoStandardAnalyser
 *  
 *  we have used AlfrescoStandardAnalyser in alfresco/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/model/dataTypeAnalyzers_de.properties
 *  cause the german stemming leads to problems when searching
 *  
 *  but there is also a problem with the accent filter that makes ä to a i.e. so when you serach for "Auge" you also get "Säugetier"
 *  thats not what we want. So this is our Analyzer where we simply comment out
 *  result = new ISOLatin1AccentFilter(result);
 *  
 *  UPDATE 2012/04/23:
 *  can not find www.kaenguru.at
 *  -> Problem is the english stop words contain "at"
 *  -> Solution: comment out
 *  result = new StopFilter(result, stopSet); 
 *  Configuration: 
 *  - alfresco model: tokenized=true
 *  - metadataset: escape=true
 *  - fuzzy wildcard search
 *  
 *  This class has to be deployed in alfresco webapp
 */
public class Edu_SharingStandardAnalyser extends Analyzer {
	
	
	private Log logger = LogFactory.getLog(Edu_SharingStandardAnalyser.class);
	private Set stopSet;

    /**
     * An array containing some common English words that are usually not useful for searching.
     */
    public static final String[] STOP_WORDS = StopAnalyzer.ENGLISH_STOP_WORDS;
    
    
    public Edu_SharingStandardAnalyser() {
    	 this(STOP_WORDS);
    }
    
    public Edu_SharingStandardAnalyser(String[] stopWords) {
    	//dont do this, it's called many times: logger.info("was loaded");
    	stopSet = StopFilter.makeStopSet(stopWords);
	}
	
	public TokenStream tokenStream(String fieldName, Reader reader) {
		
		 TokenStream result = new WhitespaceTokenizer(reader);
		 result = new AlfrescoStandardFilter(result);
	     result = new LowerCaseFilter(result);
	     
	     //result = new StopFilter(result, stopSet);
	     //result = new ISOLatin1AccentFilter(result);
	     return result;
	}
	
}
