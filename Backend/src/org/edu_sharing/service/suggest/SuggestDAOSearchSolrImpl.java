package org.edu_sharing.service.suggest;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.SuggestFacetDTO;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

import com.google.gwt.user.client.ui.SuggestOracle;

/**
 * define a custom query, and custom facette
 * 
 * @author mv
 *
 */
public class SuggestDAOSearchSolrImpl implements SuggestDAO {

	Logger logger = Logger.getLogger(SuggestDAOSearchSolrImpl.class);
		
	private final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private final static int maxItems = 100; 
	
	private final ApplicationContext applicationContext;
	private final ServiceRegistry serviceRegistry;
		
	private MetadataSetBaseProperty property;

	public SuggestDAOSearchSolrImpl() {
	
		applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}

	
	@Override
	public List<? extends  SuggestOracle.Suggestion> query(String query)  {
		
		
		if(query.equals("-all-")){
			query = ""; // use the * 
		}
		
		
		query = query.toLowerCase();
		
		String suggestCheck = new String(query);
		
		query = query+"*";
		
		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();

		try {
			
			SearchService searchService = serviceRegistry.getSearchService();
			SearchParameters searchParameters = new SearchParameters();
			searchParameters.addStore(storeRef);
	
			searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
	
			searchParameters.setSkipCount(0);
			searchParameters.setMaxItems(1);
	
			String queryString = property.getParam("query");
			queryString = queryString.replace("${value}", query);
			searchParameters.setQuery(queryString);
			
			String facet = property.getParam("facet");
			String[] facets = facet.split(",");
			for(String f : facets){
				FieldFacet fieldFacet = new FieldFacet("@"+f);
				fieldFacet.setLimit(maxItems);
				fieldFacet.setMinCount(1);
				searchParameters.addFieldFacet(fieldFacet);
			}
			
			

			ResultSet rs = searchService.query(searchParameters);
			
			for(String f : facets){
				List<Pair<String, Integer>> facettPairs = rs.getFieldFacet("@"+f);
				
				
				for (Pair<String, Integer> pair : facettPairs) {
					
					//solr 4 bug: leave out zero values
					if(pair.getSecond() == 0){
						continue;
					}
		
					String value = new String(pair.getFirst().getBytes(), "UTF-8");
					
					if(suggestCheck.trim().equals("")){
						SuggestFacetDTO dto = new SuggestFacetDTO();
						dto.setFacet(value);
							
						result.add(dto);
					}else{
						/**
						 * prevent that multivalue attributes deliver facettes that do not contain the query 
						 */
						if(value.toLowerCase().contains(suggestCheck)){
							
							SuggestFacetDTO dto = new SuggestFacetDTO();
							dto.setFacet(value);
								
							result.add(dto);
						}
					}
					
				}
			}
			
		} catch (Throwable t) {
			
			logger.error(t.getMessage(), t);
		}
		
		return result;
		
	}
	
	@Override
	public String getValue(String key) {
		
		return key;
	}
	
	@Override
	public void setMetadataProperty(MetadataSetBaseProperty property) {
		this.property = property;
		
	}	
	
}
