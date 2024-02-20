package org.edu_sharing.alfresco.transformer;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.HashMap;
import java.util.Map;

import static org.edu_sharing.alfresco.transformer.EduLocalTransformServiceRegistry.TRANSFORM_OPTION_RESOURCETYPE;

public class EduTransformerTool {

    private static Log logger = LogFactory.getLog(EduTransformerTool.class);

    public static Map<String, String> adaptOptions(NodeService nodeService, Map<String, String> options, NodeRef sourceNodeRef){
        String resourceType = (String)nodeService.getProperty(sourceNodeRef, QName.createQName(CCConstants.CCM_PROP_CCRESSOURCETYPE));

        if(resourceType != null && !resourceType.trim().isEmpty()) {
            logger.info("adding edu-sharing option resourceType:" + resourceType);
            options = new HashMap<>(options);
            options.put(TRANSFORM_OPTION_RESOURCETYPE, resourceType);
        }
        return options;
    }
}
