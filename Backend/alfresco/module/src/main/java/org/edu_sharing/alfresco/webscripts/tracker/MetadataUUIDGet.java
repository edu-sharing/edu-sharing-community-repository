package org.edu_sharing.alfresco.webscripts.tracker;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.domain.permissions.AclDAO;
import org.alfresco.repo.security.permissions.impl.model.PermissionModel;
import org.alfresco.repo.solr.MetaDataResultsFilter;
import org.alfresco.repo.solr.NodeMetaData;
import org.alfresco.repo.solr.NodeMetaDataParameters;
import org.alfresco.repo.solr.SOLRTrackingComponent;
import org.alfresco.repo.web.scripts.solr.NodesMetaDataGet;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.*;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

public class MetadataUUIDGet extends DeclarativeWebScript {

    protected static final Log logger = LogFactory.getLog(MetadataUUIDGet.class);


    private NodeService nodeService;

    private SOLRTrackingComponent solrTrackingComponent;
    // SolrSerializer
    private Object solrSerializer;


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
        String uuid = req.getParameter("uuid");
        if (uuid == null)
        {
            throw new WebScriptException("no uuid found");
        }
        Long dbid = (Long) nodeService.getProperty(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, uuid),
                ContentModel.PROP_NODE_DBID
        );

        NodeMetaDataParameters params = new NodeMetaDataParameters();
        params.setNodeIds(Collections.singletonList(dbid));
        final NodesMetaDataGet.FreemarkerNodeMetaData[] result = new NodesMetaDataGet.FreemarkerNodeMetaData[1];
        solrTrackingComponent.getNodesMetadata(params, null, new SOLRTrackingComponent.NodeMetaDataQueryCallback() {
            @Override
            public boolean handleNodeMetaData(NodeMetaData nodeMetaData)
            {
                // need to perform data structure conversions that are compatible with Freemarker
                // e.g. Serializable -> String, QName -> String (because map keys must be string, number)
                try
                {
                    Class<?> c = NodesMetaDataGet.FreemarkerNodeMetaData.class;
                    Constructor<?> constructor = c.getConstructor(Class.forName("org.alfresco.repo.web.scripts.solr.SOLRSerializer"), nodeMetaData.getClass());
                    //NodesMetaDataGet.FreemarkerNodeMetaData fNodeMetaData = new NodesMetaDataGet.FreemarkerNodeMetaData(solrSerializer, nodeMetaData);
                    result[0] = (NodesMetaDataGet.FreemarkerNodeMetaData) constructor.newInstance(solrSerializer, nodeMetaData);

                }
                catch(Exception e)
                {
                    throw new AlfrescoRuntimeException("Problem converting to Freemarker using node " + nodeMetaData.getNodeRef().toString(), e);
                }
                return true;
            }
        });
        Map<String, Object> model = new HashMap<>(1, 1.0F);
        model.put("nodeMetaData", result[0]);
        model.put("filter", new MetaDataResultsFilter());
        return model;
    }


    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setSolrTrackingComponent(SOLRTrackingComponent solrTrackingComponent) {
        this.solrTrackingComponent = solrTrackingComponent;
    }

    public void setSolrSerializer(Object solrSerializer) {
        this.solrSerializer = solrSerializer;
    }
}
