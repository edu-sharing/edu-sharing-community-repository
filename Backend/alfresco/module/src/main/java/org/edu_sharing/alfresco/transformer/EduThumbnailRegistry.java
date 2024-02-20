package org.edu_sharing.alfresco.transformer;

import org.alfresco.repo.content.transform.TransformerDebug;
import org.alfresco.repo.rendition2.RenditionDefinition2;
import org.alfresco.repo.rendition2.RenditionDefinitionRegistry2;
import org.alfresco.repo.rendition2.TransformationOptionsConverter;
import org.alfresco.repo.thumbnail.ThumbnailDefinition;
import org.alfresco.repo.thumbnail.ThumbnailRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.transform.registry.TransformServiceRegistry;

import java.util.Map;

public class EduThumbnailRegistry extends ThumbnailRegistry {

    private TransformServiceRegistry transformServiceRegistry;
    private RenditionDefinitionRegistry2 renditionDefinitionRegistry2;

    private TransformServiceRegistry localTransformServiceRegistry;

    private TransformationOptionsConverter converter;

    NodeService nodeService;

    @Override
    public boolean isThumbnailDefinitionAvailable(String sourceUrl, String sourceMimetype, long sourceSize, NodeRef sourceNodeRef, ThumbnailDefinition thumbnailDefinition){
        // Use RenditionService2 if it knows about the definition, otherwise use localTransformServiceRegistry.
        // Needed as disabling local transforms should not disable thumbnails if they can be done remotely.
        boolean supported = false;
        String targetMimetype = thumbnailDefinition.getMimetype();
        RenditionDefinition2 renditionDefinition = getEquivalentRenditionDefinition2(thumbnailDefinition);
        if (renditionDefinition != null)
        {
            Map<String, String> options = renditionDefinition.getTransformOptions();
            //fix add eduResourceType if available
            options = EduTransformerTool.adaptOptions(nodeService,options,sourceNodeRef);
            String renditionName = renditionDefinition.getRenditionName();
            supported = transformServiceRegistry.isSupported(sourceMimetype, sourceSize, targetMimetype,
                    options, renditionName);
        }
        else
        {
            boolean orig = TransformerDebug.setDebugOutput(false);
            try
            {
                TransformationOptions transformationOptions = thumbnailDefinition.getTransformationOptions();
                String renditionName = thumbnailDefinition.getName();
                Map<String, String> options = converter.getOptions(transformationOptions, sourceMimetype, targetMimetype);
                supported = localTransformServiceRegistry.isSupported(sourceMimetype, sourceSize, targetMimetype,
                        options, renditionName);
            }
            finally
            {
                TransformerDebug.setDebugOutput(orig);
            }
        }
        return supported;
    }

    @Override
    public void setTransformServiceRegistry(TransformServiceRegistry transformServiceRegistry) {
        this.transformServiceRegistry = transformServiceRegistry;
        super.setTransformServiceRegistry(transformServiceRegistry);
    }

    @Override
    public void setRenditionDefinitionRegistry2(RenditionDefinitionRegistry2 renditionDefinitionRegistry2) {
        this.renditionDefinitionRegistry2 = renditionDefinitionRegistry2;
        super.setRenditionDefinitionRegistry2(renditionDefinitionRegistry2);
    }

    @Override
    public void setConverter(TransformationOptionsConverter converter) {
        this.converter = converter;
        super.setConverter(converter);
    }

    @Override
    public void setLocalTransformServiceRegistry(TransformServiceRegistry localTransformServiceRegistry) {
        this.localTransformServiceRegistry = localTransformServiceRegistry;
        super.setLocalTransformServiceRegistry(localTransformServiceRegistry);
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    private RenditionDefinition2 getEquivalentRenditionDefinition2(ThumbnailDefinition thumbnailDefinition)
    {
        String renditionName = thumbnailDefinition.getName();
        RenditionDefinition2 renditionDefinition = renditionDefinitionRegistry2.getRenditionDefinition(renditionName);
        if (renditionDefinition != null)
        {
            String thumbnailTargetMimetype = thumbnailDefinition.getMimetype();
            String renditionTargetMimetype = renditionDefinition.getTargetMimetype();
            if (!renditionTargetMimetype.equals(thumbnailTargetMimetype))
            {
                renditionDefinition = null;
            }
        }
        return renditionDefinition;
    }


}
