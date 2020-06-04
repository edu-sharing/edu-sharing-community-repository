package org.edu_sharing.service.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public class CMISSearchHelper {

    private static Logger logger= Logger.getLogger(CMISSearchHelper.class);

    public static ResultSet fetchNodesByTypeAndFilters(String nodeType, Map<String,String> filters, int from, int pageSize, int maxPermissionChecks){
    	logger.info("from: "+from+ " pageSize:"+ pageSize);
    	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

        SearchParameters params=new SearchParameters();
        params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        // will use the database
        params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        
        params.setSkipCount(from);
        params.setMaxItems(pageSize);
        params.setMaxPermissionChecks(maxPermissionChecks);
        String tableName=CCConstants.getValidLocalName(nodeType);
        String tableNameAlias=tableName.split(":")[1];
        StringBuilder join= new StringBuilder();
        StringBuilder where= new StringBuilder();
        if(filters!=null && filters.size()>0){
            List<String> joinedTable = new ArrayList<>();
            int filterCount= 0;
            for(Map.Entry<String,String> filter : filters.entrySet()){
                if(filter.getKey().startsWith("cmis")){
                    if(where.length() > 0) {
                        where.append(" AND ");
                    }else {
                        where.append(" WHERE ");
                    }
                    where.append(tableNameAlias).append(".").append(filter.getKey()).append(" = ").append(escape(filter.getValue()));
                }
                else{
                    // join the needed aspect, and access this ones value
                    PropertyDefinition property = serviceRegistry.getDictionaryService().getProperty(QName.createQName(filter.getKey()));
                    String aspectTable=CCConstants.getValidLocalName(property.getContainerClass().getName().toString());
                    String aspectTableAlias = property.getContainerClass().getName().getLocalName();
                    aspectTableAlias +=filterCount;
                    if(!joinedTable.contains(aspectTable)) {
                        join.append("JOIN ").append(aspectTable).append(" AS ").append(aspectTableAlias)
                                .append(" ON ").append(aspectTableAlias).append(".cmis:objectId = ").append(tableNameAlias).append(".cmis:objectId ");
                        joinedTable.add(aspectTable);
                    }
                    if(where.length() > 0) {
                        where.append(" AND ");
                    }else {
                        where.append(" WHERE ");
                    }
                    where.append(aspectTableAlias).append(".").append(CCConstants.getValidLocalName(filter.getKey()));
                    if(filter.getValue()==null) {
                        where.append(" IS NULL");
                    }else{
                        where.append(" = ").append(escape(filter.getValue()));
                    }
                }
                filterCount++;
            }
        }
        String query="SELECT "+tableNameAlias+".cmis:name FROM "+ tableName + " AS " + tableNameAlias + " " + join + where;
        params.setQuery(query);
        ResultSet result = serviceRegistry.getSearchService().query(params);
        
        logger.info(query+": "+result.getNumberFound() +" "+ result.length() +" "+ result.getClass().getName() +" getBulkFetchSize: "+ result.getBulkFetchSize()+" "+result);
        return result;
    }
    
    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,String> filters, int maxPermissionChecks){
    	List<NodeRef> result = new ArrayList<NodeRef>();
        
        int from = 0;
        
        int pageSize = 1000;
        
        ResultSet resultSet = null;
        do {
     	   resultSet = fetchNodesByTypeAndFilters(nodeType, filters, from, pageSize, maxPermissionChecks);
     	   result.addAll(resultSet.getNodeRefs());
     	   from += pageSize;
        }while(resultSet.length() > 0);
          
        logger.info("result:" + result.size());
        return result;
    }
    
    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,String> filters){
       return fetchNodesByTypeAndFilters(nodeType,filters,1000);
    }

    /**
     * from QueryStatementImpl
     */
    private static String escape(String str) {
        StringBuilder sb = new StringBuilder(str.length() + 16);
        sb.append('\'');

        for(int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == '\'' || c == '\\') {
                sb.append('\\');
            }

            sb.append(c);
        }

        sb.append('\'');
        return sb.toString();
    }
}
