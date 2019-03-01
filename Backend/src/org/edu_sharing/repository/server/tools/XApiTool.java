package org.edu_sharing.repository.server.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class XApiTool {
    public static String sendToXApi(String nodeId, JSONObject xApiData) throws Throwable {
        JSONObject data = addData(nodeId, xApiData);
        return sendToStore(data);
    }

    private static String sendToStore(JSONObject data) throws Exception {
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        if(learningLocker==null)
            throw new Exception("No application with type LEARNING_LOCKER registered. Please register a xApi storage first");
        String uuid = UUID.randomUUID().toString();
        String url = learningLocker.getBaseUrl();
        url += "/data/xAPI/statements?statementId="+URLEncoder.encode(uuid);
        Map<String, String> header=new HashMap<>();
        header.put("X-Experience-API-Version","1.0.3");
        header.put("Content-Type","application/json");
        header.put("Authorization","Basic "+learningLocker.getApiKey());
        PostMethod method=new PostMethod(url);
        method.setRequestEntity(new ByteArrayRequestEntity(data.toString().getBytes()));
        return new HttpQueryTool().query(url, header, method);
    }

    private static JSONObject addData(String nodeId, JSONObject xApiData) throws Throwable {
        HashMap<String, Object> props = NodeServiceFactory.getLocalService().getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
        if(!xApiData.getJSONObject("object").getJSONObject("definition").has("extensions"))
            xApiData.getJSONObject("object").getJSONObject("definition").put("extensions",new JSONObject());
        JSONObject propsData = new JSONObject();
        props.entrySet().stream().forEach((entry)->{
            try {
                propsData.put(CCConstants.getValidLocalName(entry.getKey()),entry.getValue());
            } catch (JSONException e) {}
        });
        xApiData.getJSONObject("object").getJSONObject("definition").getJSONObject("extensions").put("http://edu-sharing.net/x-api",propsData);

        // add the user account data
        xApiData.getJSONObject("actor").put("account",new JSONObject()
                .put("name",AuthenticationUtil.getFullyAuthenticatedUser())
                .put("homePage",ApplicationInfoList.getHomeRepository().getClientBaseUrl())
        );
        return xApiData;
    }
}
