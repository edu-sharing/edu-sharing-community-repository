package org.edu_sharing.metadataset.v2;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.io.Serializable;
import java.util.Map;

public class QueryUtils {
    private static Logger logger = Logger.getLogger(QueryUtils.class);
    private static ThreadLocal<Map<String, Serializable>> userInfo = new ThreadLocal<>();

    public static String replaceCommonQueryParams(String query, ReplaceInterface replacer) {
        if(query==null)
            return query;
        try {

            for(Map.Entry<String, Serializable> prop : getUserInfo().entrySet()){
                if(prop.getValue()==null) {
                    continue;
                }
                query = replacer.replaceString(query, "${user."+CCConstants.getValidLocalName(prop.getKey())+"}",
                        prop.getValue().toString());
            }
        } catch (Exception e) {
            logger.warn("Could not replace user data in search query, search might fail",e);
        }

        query = replacer.replaceString(query,"${educontext}", NodeCustomizationPolicies.getEduSharingContext());
query = replacer.replaceString(query, "${authority}", AuthenticationUtil.getFullyAuthenticatedUser());
        return query;
    }

    public static ReplaceInterface luceneReplacer = (str, search, replace) -> str.replace(search, QueryParser.escape(replace));

    public static void setUserInfo(Map<String, Serializable> userInfo) {
        QueryUtils.userInfo.set(userInfo);
    }

    public static Map<String, Serializable> getUserInfo() {
        return userInfo.get();
    }

    private interface ReplaceInterface {
        String replaceString(String str, String search, String replace);
    }
}
