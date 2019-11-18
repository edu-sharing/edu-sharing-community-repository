package org.edu_sharing.service.search;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.XApiTool;
import org.edu_sharing.service.stream.StreamServiceFactory;

import java.util.*;

public class SearchRelevancyTool {

    public static String getLuceneQuery() throws Exception {
        /**
         * property to look for successors
         */
        String property = StreamServiceFactory.getConfig().getString("relevancy.property");
        if(property==null){
            throw new IllegalArgumentException("No property for relevancy specified");
        }
        /**
         * how many successors (depth) should be used
         * 1 means just the immediate successors, while 2 means immediate + all successor of the immediate sucessors, and so one
         */
        int maxDepth = StreamServiceFactory.getConfig().getInt("relevancy.depth");
        /**
         * limit the amount of the facettes by the count of the last actions
         */
        int lastActionsLimit =  StreamServiceFactory.getConfig().getInt("relevancy.actionLimit");


        //throw new NotImplementedException("SearchRelevancyTool.getLuceneQuery is not implemented for this repository");
        MetadataSetV2 mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id);
        // fetch all facettes for already viewed contents from xapi
        List<String> facettes = XApiTool.getFacettesFromStore(AuthenticationUtil.getFullyAuthenticatedUser(), property,lastActionsLimit);
        // propably nothing viewed or xapi store failed, return empty query
        if(facettes==null)
            return "";
        // get the valuespace from the property
        Map<String, MetadataKey> values = mds.findWidget(property).getValuesAsMap();
        Set<String> valuesToQuery=new HashSet<>();
        boolean hasPrecedes=values.entrySet().stream().anyMatch((e)->e.getValue().getPreceds()!=null);
        if(!hasPrecedes){
            throw new IllegalArgumentException("Specified "+property+" to use for relevancy, but the valuespace does not include any precedes relations");
        }
        // map all viewed facettes, find their succesors (by checking which other values this one preceds)
        Set<String> searchPool=new HashSet<>(facettes);
        for(int i=0;i<maxDepth;i++) {
            for (String key : new HashSet<>(searchPool)) {
                MetadataKey mdsKey = values.get(key);
                if (mdsKey != null && mdsKey.getPreceds() != null) {
                    valuesToQuery.addAll(mdsKey.getPreceds());
                    // add them for the next cycle
                    searchPool.addAll(mdsKey.getPreceds());
                }
            }
        }
        valuesToQuery.addAll(facettes);
        StringBuilder luceneQuery = toLuceneQuery(property,valuesToQuery);
        return luceneQuery.toString();
    }

    private static StringBuilder toLuceneQuery(String property,Collection<String> valuesToQuery) {
        StringBuilder luceneQuery=new StringBuilder();
        for(String value : valuesToQuery){
            if(luceneQuery.length()>0){
                luceneQuery.append(" OR ");
            }
            luceneQuery.
                    append("@").
                    append(QueryParser.escape(property)).
                    append(":\"").
                    append(QueryParser.escape(value)).
                    append("\"");
        }
        return luceneQuery;
    }
}
