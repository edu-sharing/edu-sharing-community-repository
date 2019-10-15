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

import java.util.*;

public class SearchRelevancyTool {
    /**
     * property to look for successors
     */
    private static String PROPERTY = CCConstants.CCM_PROP_IO_COMPETENCE_DIGITAL2;
    /**
     * how many successors (depth) should be used
     * 1 means just the immediate successors, while 2 means immediate + all successor of the immediate sucessors, and so one
     */
    private static int MAX_DEPTH = 3;
    public static String getLuceneQuery() throws Exception {
        //throw new NotImplementedException("SearchRelevancyTool.getLuceneQuery is not implemented for this repository");
        MetadataSetV2 mds = MetadataHelper.getMetadataset(ApplicationInfoList.getHomeRepository(), CCConstants.metadatasetdefault_id);
        // fetch all facettes for already viewed contents from xapi
        List<String> facettes = XApiTool.getFacettesFromStore(AuthenticationUtil.getFullyAuthenticatedUser(), PROPERTY);
        // get the valuespace from the property
        Map<String, MetadataKey> values = mds.findWidget(CCConstants.getValidLocalName(PROPERTY)).getValuesAsMap();
        Set<String> valuesToQuery=new HashSet<>();
        // map all viewed facettes, find their succesors (by checking which other values this one preceds)
        Set<String> searchPool=new HashSet<>(facettes);
        for(int i=0;i<MAX_DEPTH;i++) {
            for (String key : new HashSet<>(searchPool)) {
                MetadataKey mdsKey = values.get(key);
                if (mdsKey != null && mdsKey.getPreceds() != null) {
                    valuesToQuery.addAll(mdsKey.getPreceds());
                    // add them for the next cycle
                    searchPool.addAll(mdsKey.getPreceds());
                }
            }
        }
        StringBuilder luceneQuery = toLuceneQuery(valuesToQuery);
        return luceneQuery.toString();
    }

    private static StringBuilder toLuceneQuery(Collection<String> valuesToQuery) {
        StringBuilder luceneQuery=new StringBuilder();
        for(String value : valuesToQuery){
            if(luceneQuery.length()>0){
                luceneQuery.append(" OR ");
            }
            luceneQuery.
                    append("@").
                    append(QueryParser.escape(CCConstants.getValidLocalName(PROPERTY))).
                    append(":\"").
                    append(QueryParser.escape(value)).
                    append("\"");
        }
        return luceneQuery;
    }
}
