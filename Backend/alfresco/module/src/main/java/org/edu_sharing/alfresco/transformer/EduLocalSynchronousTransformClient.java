package org.edu_sharing.alfresco.transformer;

import org.alfresco.repo.rendition2.LocalSynchronousTransformClient;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

public class EduLocalSynchronousTransformClient extends LocalSynchronousTransformClient {

    NodeService nodeService;

    private static Log logger = LogFactory.getLog(EduLocalSynchronousTransformClient.class);

    @Override
    public boolean isSupported(String sourceMimetype, long sourceSizeInBytes, String contentUrl, String targetMimetype, Map<String, String> actualOptions, String transformName, NodeRef sourceNodeRef) {
        return super.isSupported(sourceMimetype, sourceSizeInBytes, contentUrl, targetMimetype, adaptOptions(actualOptions,sourceNodeRef), transformName, sourceNodeRef);
    }

    @Override
    public void transform(ContentReader reader, ContentWriter writer, Map<String, String> actualOptions, String transformName, NodeRef sourceNodeRef) {
        super.transform(reader, writer,  adaptOptions(actualOptions,sourceNodeRef), transformName, sourceNodeRef);
    }

    private Map<String, String> adaptOptions(Map<String, String> options, NodeRef sourceNodeRef){
       return EduTransformerTool.adaptOptions(nodeService,options,sourceNodeRef);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }
}
