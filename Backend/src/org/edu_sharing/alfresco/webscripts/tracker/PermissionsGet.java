package org.edu_sharing.alfresco.webscripts.tracker;

import org.alfresco.repo.domain.permissions.AclDAO;
import org.alfresco.repo.security.permissions.AccessControlEntry;
import org.alfresco.repo.security.permissions.AccessControlList;
import org.alfresco.repo.web.scripts.solr.AclsReadersGet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionsGet extends DeclarativeWebScript {

    protected static final Log logger = LogFactory.getLog(PermissionsGet.class);


    private AclDAO aclDAO;

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        try
        {
            Map<String, Object> model = buildModel(req);
            if (logger.isDebugEnabled())
            {
                logger.debug("Result: \n\tRequest: " + req + "\n\tModel: " + model);
            }
            return model;
        }
        catch(IOException e)
        {
            throw new WebScriptException("IO exception parsing request", e);
        }
        catch(JSONException e)
        {
            throw new WebScriptException("Invalid JSON", e);
        }
    }

    private Map<String, Object> buildModel(WebScriptRequest req) throws JSONException, IOException
    {
        List<Long> aclIds = null;

        Content content = req.getContent();
        if (content == null)
        {
            throw new WebScriptException("Request content is empty");
        }
        JSONObject o = new JSONObject(content.getContent());
        JSONArray aclIdsJSON = o.has("aclIds") ? o.getJSONArray("aclIds") : null;
        if (aclIdsJSON == null)
        {
            throw new WebScriptException(
                    Status.STATUS_BAD_REQUEST,
                    "Parameter 'aclIds' not provided in request content.");
        }
        else if (aclIdsJSON.length() == 0)
        {
            throw new WebScriptException(
                    Status.STATUS_BAD_REQUEST,
                    "Parameter 'aclIds' must hold from 1 or more IDs.");
        }

        aclIds = new ArrayList<Long>(aclIdsJSON.length());
        for (int i = 0; i < aclIdsJSON.length(); i++)
        {
            aclIds.add(aclIdsJSON.getLong(i));
        }

        List<Acl> accessControlLists = new ArrayList<>();
        for(long aclId : aclIds){
            AccessControlList accessControlList = aclDAO.getAccessControlList(aclId);
            Acl acl = new Acl();
            acl.setAclId(aclId);
            List<Ace> aces = new ArrayList<>();
            acl.setAces(aces);
            for(AccessControlEntry entry : accessControlList.getEntries()){
                Ace ace = new Ace();
                ace.setAuthority(entry.getAuthority());
                ace.setPermission(entry.getPermission().getName());
                aces.add(ace);
            }
            accessControlLists.add(acl);
        }


        Map<String, Object> model = new HashMap<String, Object>(1, 1.0f);
        model.put("accessControlLists", accessControlLists);

        if (logger.isDebugEnabled())
        {
            logger.debug("Result: \n\tRequest: " + req + "\n\tModel: " + model);
        }

        return model;
    }

    public void setAclDAO(AclDAO aclDAO) {
        this.aclDAO = aclDAO;
    }
}
