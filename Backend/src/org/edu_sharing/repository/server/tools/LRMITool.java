package org.edu_sharing.repository.server.tools;

import com.google.gson.JsonObject;
import com.google.gwt.json.client.JSONNull;
import net.sf.vcard4j.java.VCard;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.metadata.ValueTool;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class LRMITool {
    public static JSONObject getLRMIJson(String nodeId) throws Throwable {
        String propFile = "org/edu_sharing/repository/server/tools/lrmi.properties";
        JSONObject lrmi=new JSONObject();
        // TODO: This probably has to work for remote repos in future
        HashMap<String, Object> props = NodeServiceFactory.getLocalService().getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
        java.util.Properties lrmiProps = PropertiesHelper.getProperties(propFile, PropertiesHelper.TEXT);
        lrmi.put("@context","http://schema.org/");
        lrmi.put("@type","WebPage");
        Object creator=getFromVCard(CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR,props);
        lrmi.put("creator",creator);
        for(String prop : lrmiProps.stringPropertyNames()){
            List<String> propsList = new ArrayList<>(
                    Arrays.asList(lrmiProps.getProperty(prop).split(",")));
            propsList=propsList.stream().map(CCConstants::getValidGlobalName)
                .collect(Collectors.toList());

            lrmi.put(prop,getProperty(props,propsList));
        }
        /*
        lrmi.put("name",getProperty(props,CCConstants.LOM_PROP_GENERAL_TITLE,CCConstants.CM_NAME));
        lrmi.put("about",getProperty(props,CCConstants.LOM_PROP_GENERAL_DESCRIPTION));
        lrmi.put("learningResourceType",getProperty(props,CCConstants.LOM_PROP_EDUCATIONAL_LEARNINGRESOURCETYPE));
        lrmi.put("typicalAgeRange",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        lrmi.put("timeRequired",getProperty(props,CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALLEARNINGTIME));
        */
        return lrmi;
    }

    private static Object getFromVCard(String key,HashMap<String,Object> props) throws JSONException {
        if(props.get(key)==null)
            return null;
        String[] values=new ValueTool().getMultivalue(props.get(key).toString());
        JSONArray result=new JSONArray();
        for(String v: values) {
            HashMap<String, Object> vcard = VCardConverter.getVCardHashMap(null, key, v);
            if (vcard == null || vcard.size() == 0)
                return null;

            JSONObject json = new JSONObject();
            if (VCardConverter.isPersonVCard(key, vcard)) {
                json.put("@type", "Person");
                json.put("givenName", vcard.get(key + CCConstants.VCARD_GIVENNAME));
                json.put("familyName", vcard.get(key + CCConstants.VCARD_SURNAME));
            } else {
                json.put("@type", "Organization");
                json.put("legalName", vcard.get(key + CCConstants.VCARD_ORG));
            }
            result.put(json);
        }
        if(result.length()>1)
            return result;
        else
            return result.getJSONObject(0);
    }

    private static Object getProperty(HashMap<String, Object> props, List<String> keys) {
        for(String key : keys){
            if(!props.containsKey(key) || props.get(key)==null)
                continue;
            if(props.get(key) instanceof String){
                if(((String)props.get(key)).isEmpty()){
                    continue;
                }
            }
                return props.get(key);
        }
        return null;
    }
}
