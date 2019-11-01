package org.edu_sharing.metadataset.v2;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;

import java.util.ArrayList;
import java.util.List;

public class MetadataQueryBase {
    protected String basequery;
    private List<MetadataQueryCondition> conditions=new ArrayList<>();

    public static String replaceCommonQueryParams(String query) {
        if(query==null)
            return query;
        return query
                .replace("${educontext}", QueryParser.escape(NodeCustomizationPolicies.getEduSharingContext()))
                .replace("${authority}",QueryParser.escape(AuthenticationUtil.getFullyAuthenticatedUser()));
    }

    public void addCondition(MetadataQueryCondition condition) {
        conditions.add(condition);
    }

    public Iterable<MetadataQueryCondition> getConditions() {
        return conditions;
    }

    public String getBasequery() {
        return replaceCommonQueryParams(basequery);
    }

    public void setBasequery(String basequery) {
        this.basequery = basequery;
    }
}
