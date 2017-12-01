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

public class SuggestDAOSearchImpl implements SuggestDAO {

	Logger logger = Logger.getLogger(SuggestDAOSearchImpl.class);
		
	private final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	private final static int maxItems = 100; 
	
	private final ApplicationContext applicationContext;
	private final ServiceRegistry serviceRegistry;
	
	SearchService searchService = null;
		
	private MetadataSetBaseProperty property;

	public SuggestDAOSearchImpl() {
	
		applicationContext = AlfAppContextGate.getApplicationContext();
		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		searchService = (SearchService)applicationContext.getBean("scopedSearchService");
	}

	
	@Override
	public List<? extends  SuggestOracle.Suggestion> query(String query)  {
		
		
		if(query.equals("-all-")){
			query = ""; // use the * 
		}
		
		query = query.toLowerCase();
		
		List<SuggestOracle.Suggestion> result = new ArrayList<SuggestOracle.Suggestion>();

		try {
			
			SearchParameters searchParameters = new SearchParameters();
			searchParameters.addStore(storeRef);
	
			searchParameters.setLanguage(SearchService.LANGUAGE_LUCENE);
	
			searchParameters.setSkipCount(0);
			searchParameters.setMaxItems(1);
	
			String propType = property.getType();
			
			if(propType == null){
				propType = CCConstants.CCM_TYPE_IO;
			}
			
			String propName = "@" + CCConstants.getValidLocalName(property.getName()).replace(":", "\\:");
			
			searchParameters.setQuery(
					"TYPE:\"" + propType + "\"" 
					+ " AND " + propName + ":\"*" + query + "*\"");
			
			logger.debug("Query:"+searchParameters.getQuery());
	
			String facetName = "@" + property.getName();// + ".__.u";
			
			logger.debug("Facette:"+facetName);

			FieldFacet fieldFacet = new FieldFacet(facetName);
			fieldFacet.setLimit(maxItems);
			fieldFacet.setMinCount(1);
			searchParameters.addFieldFacet(fieldFacet);

			ResultSet rs = searchService.query(searchParameters);
			
			List<Pair<String, Integer>> facettPairs = rs.getFieldFacet(facetName);
			
			logger.debug("found " + facettPairs.size() + " facettpairs");
			
			for (Pair<String, Integer> pair : facettPairs) {
				
				//solr 4 bug: leave out zero values
				if(pair.getSecond() == 0){
					continue;
				}
	
				String value = pair.getFirst(); // new String(pair.getFirst().getBytes(), "UTF-8");
				
				if(value.toLowerCase().contains(query)){
				
					SuggestFacetDTO dto = new SuggestFacetDTO();
					dto.setFacet(value);
					
					result.add(dto);
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
