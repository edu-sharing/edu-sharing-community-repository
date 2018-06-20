package org.edu_sharing.repository.server.tools;

import org.alfresco.service.cmr.repository.StoreRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.json.JSONObject;

import java.util.HashMap;

public class LRMITool {
    public static JSONObject getLRMIJson(String nodeId) throws Throwable {
        JSONObject lrmi=new JSONObject();
        // TODO: This probably has to work for remote repos in future
        HashMap<String, Object> props = NodeServiceFactory.getLocalService().getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
        lrmi.put("@context","http://schema.org/");
        lrmi.put("@type","WebPage");
        lrmi.put("name",getProperty(props,CCConstants.LOM_PROP_GENERAL_TITLE,CCConstants.CM_NAME));
        lrmi.put("about",getProperty(props,CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        lrmi.put("learningResourceType",getProperty(props,CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE));
        lrmi.put("typicalAgeRange",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        lrmi.put("timeRequired",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME));
        lrmi.put("timeRequired",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME));
        return lrmi;
    }

    private static Object getProperty(HashMap<String, Object> props, String... keys) {
        for(String key : keys){
            if(props.containsKey(key))
                return props.get(key);
        }
        return null;
    }
}
