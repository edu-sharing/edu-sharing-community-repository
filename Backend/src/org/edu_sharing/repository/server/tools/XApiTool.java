package org.edu_sharing.repository.server.tools;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataSetV2;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.tracking.TrackingService;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

public class XApiTool {
    private static Logger logger = Logger.getLogger(XApiTool.class);

    public static String VERB_OPEN="http://adlnet.gov/expapi/verbs/open";
    public static String VERB_DOWNLOADED="http://adlnet.gov/expapi/verbs/downloaded";

    public static String sendToXApi(String nodeId, JSONObject xApiData) throws Throwable {
        if(AuthorityServiceFactory.getLocalService().isGuest()){
            throw new IllegalArgumentException("user is guest, will not track xApi data");
        }
        JSONObject data = addData(nodeId, xApiData);
        return sendToStore(data);
    }
    public static void trackActivity(String activityVerb,String nodeId,String nodeVersion){
        ApplicationInfo learningLocker = ApplicationInfoList.getLearningLocker();
        if(learningLocker==null)
            return;
        try {
            JSONObject data = getTemplate(nodeId, nodeVersion);
            data.getJSONObject("verb").put("id", activityVerb);
            sendToXApi(nodeId, data);
        }catch(IllegalArgumentException e){
            logger.info(e);
        }catch(Throwable t){
            logger.warn("Tracking via xApi failed",t);
        }
    }

    private static JSONObject getTemplate(String nodeId, String nodeVersion) throws JSONException {
        return new JSONObject()
            .put("actor",new JSONObject())
            .put("verb",new JSONObject())
            .put("object",new JSONObject()
                .put("definition",new JSONObject())
                .put("id",URLTool.getNgRenderNodeUrl(nodeId,nodeVersion)));
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
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        HashMap<String, String[]> props = NodeServiceHelper.getPropertiesMultivalue(nodeRef);
        //xApiData.getJSONObject("object").getJSONObject("definition").put("name","");
        if(!xApiData.getJSONObject("object").getJSONObject("definition").has("extensions"))
            xApiData.getJSONObject("object").getJSONObject("definition").put("extensions",new JSONObject());
        JSONObject propsData = new JSONObject();
        List<MetadataWidget> widgets = MetadataHelper.getWidgetsByNode(nodeRef);
        // widgets objects to unique id set
        Set<String> storedProperties = widgets.stream().map((w) -> CCConstants.getValidGlobalName(w.getId())).collect(Collectors.toSet());
        props.entrySet().stream().
                filter((entry)->storedProperties.contains(entry.getKey())).
                forEach((entry)->{
                    try {
                        propsData.put(entry.getKey(),entry.getValue());
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

    public static String mapActivityVerb(TrackingService.EventType type) {
        switch(type){
            case DOWNLOAD_MATERIAL:
                return VERB_DOWNLOADED;
            case VIEW_MATERIAL:
                return VERB_OPEN;
        }
        return null;
    }
}
