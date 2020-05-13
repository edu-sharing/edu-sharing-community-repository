package org.edu_sharing.service.search;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.AspectDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.chemistry.opencmis.client.api.QueryStatement;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.SqlParameterValue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CMISSearchHelper {

    private static Logger logger= Logger.getLogger(CMISSearchHelper.class);

    public static List<NodeRef> fetchNodesByTypeAndFilters(String nodeType, Map<String,String> filters){
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

        SearchParameters params=new SearchParameters();
        params.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
        // will use the database
        params.setLanguage(SearchService.LANGUAGE_CMIS_ALFRESCO);
        params.setMaxItems(Integer.MAX_VALUE);
        String tableName=CCConstants.getValidLocalName(nodeType);
        String tableNameAlias=tableName.split(":")[1];
        StringBuilder join= new StringBuilder();
        StringBuilder where= new StringBuilder();
        if(filters!=null && filters.size()>0){
            List<String> joinedTable = new ArrayList<>();
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
            }
        }
        String query="SELECT "+tableNameAlias+".cmis:name FROM "+ tableName + " AS " + tableNameAlias + " " + join + where;
        params.setQuery(query);
        ResultSet result = serviceRegistry.getSearchService().query(params);
        logger.info(query+": "+result.getNodeRefs().size());
        return result.getNodeRefs();
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
