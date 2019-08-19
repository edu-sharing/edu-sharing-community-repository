package org.edu_sharing.repository.server.remote;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.authentication.RemoteAuthDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.HttpQueryTool;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Rocketchat {
    private final ApplicationInfo info;
    private Logger logger = Logger.getLogger(Rocketchat.class);

    /**
     * Returns the instance of the connected rocketchat or null, if non is configured
     *
     * @return
     */
    public static Rocketchat init() {
        ApplicationInfo info = ApplicationInfoList.getRepositoryInfoByType(ApplicationInfo.TYPE_ROCKETCHAT);
        if (info == null)
            return null;
        return new Rocketchat(info);
    }

    private Rocketchat(ApplicationInfo info) {
        this.info = info;
    }

    private JSONObject callApi(String endpoint, String methodType, String postData, boolean asAdmin) throws Exception {
        String url = info.getString(ApplicationInfo.KEY_API_URL, null) + "/" + endpoint;
        HttpMethodBase method = new GetMethod(url);
        if (methodType.equals("POST")) {
            method = new PostMethod(url);
            ((PostMethod) method).setRequestEntity(new StringRequestEntity(postData, "application/json", "UTF-8"));
        }
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        if (asAdmin) {
            JSONObject admin = getAdminDetails();
            headers.put("X-Auth-Token", admin.getString("authToken"));
            headers.put("X-User-Id", admin.getString("userId"));
        }
        return new JSONObject(new HttpQueryTool().query(url, headers, method));
    }

    private JSONObject getAdminDetails() throws Exception {
        JSONObject json = callApi("v1/login", "POST", new JSONObject().
                        put("username", info.getUsername()).
                        put("password", info.getPassword()).
                        toString()
                , false);
        return json.getJSONObject("data");
    }

    public String getAuthToken() throws Exception {
        String present = fetchUser();
        if (present != null) {
            return present;
        } else {
            return createUser();
        }
    }
    private String getCleanedUsername(){
        String name=AuthenticationUtil.getFullyAuthenticatedUser();
        String escaped="";
        //@TODO in a non-perfect world, this could in theory cause collisions for specific usernames
        for(int i=0;i<name.length();i++){
            char c=name.charAt(i);
            if(!(Character.isLetter(c) || Character.isDigit(c) || c=='_' || c=='-')){
                escaped+="_";
            }
            else{
                escaped+=c;
            }
        }
        return escaped;
    }
    private String fetchUser() {
        try {
            JSONObject json = callApi("v1/users.createToken", "POST",
                    new JSONObject()
                            .put("username", getCleanedUsername())
                            .toString()
                    , true);
            return json.getJSONObject("data").getString("authToken");
        } catch (Exception e) {
            logger.info("Can not fetch rocketchat user: " + e.getMessage());
            return null;
        }
    }

    private String createUser() throws Exception {
        Map<String, Serializable> user = AuthorityServiceFactory.getLocalService().getUserInfo(AuthenticationUtil.getFullyAuthenticatedUser());
        logger.info("Creating rocketchat user");
        JSONObject json = callApi("v1/users.create", "POST",
                new JSONObject()
                        .put("username", getCleanedUsername())
                        .put("email", user.get(CCConstants.PROP_USER_EMAIL))
                        .put("name", (user.get(CCConstants.PROP_USER_FIRSTNAME) + " " + user.get(CCConstants.PROP_USER_LASTNAME)).trim())
                        .put("password", UUID.randomUUID().toString())
                        .put("sendWelcomeEmail", false)
                        .put("verified", false)
                        .toString()
                , true);
        return fetchUser();
    }

    public RemoteAuthDescription getAuthDescription() {
        RemoteAuthDescription desc=new RemoteAuthDescription();
        try {
            String api = info.getString(ApplicationInfo.KEY_API_URL, null);
            desc.setUrl(api.substring(0,api.length()-4));
            desc.setToken(getAuthToken());
            return desc;
        } catch (Throwable t) {
            logger.warn(t.getMessage(),t);
            return null;
        }
    }
}