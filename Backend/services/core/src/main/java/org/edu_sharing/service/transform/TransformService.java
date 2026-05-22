package org.edu_sharing.service.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.transform.LocalTransform;
import org.alfresco.repo.rendition2.*;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.alfresco.model.ContentModel.PROP_CONTENT;

@Slf4j
@Service("EduTransformService")
public class TransformService {

    @Autowired
    LocalTransformClient localTransformClient;

    @Autowired
    RenditionDefinitionRegistry2Impl renditionRegistry;

    ObjectMapper jsonObjectMapper = new ObjectMapper();

    @Autowired
    ContentService contentService;

    @Autowired
    NodeService nodeService;

    /**
     *
     * @param nodeRef
     * @param targetMimetype i.e. "alfresco-metadata-extract"
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T transform(NodeRef nodeRef, String targetMimetype, Class<T> clazz) {
        InputStream is = getInputStream(nodeRef, targetMimetype);
        if(is != null) {
            try {
                T value = jsonObjectMapper.readValue(is, clazz);
                is.close();
                return value;
            } catch (IOException e) {
                log.error(e.getMessage(),e);
            }
        }
        return null;
    }
    public InputStream getInputStream(NodeRef nodeRef, String targetMimetype) {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if (reader == null) {
            return null;
        }
        String transformName = targetMimetype + '/' + reader.getMimetype();
        String renditionName = TransformDefinition.convertToRenditionName(transformName);

        RenditionDefinition2 renditionDefinition = renditionRegistry.getRenditionDefinition(renditionName);
        if(renditionDefinition == null){
            log.info("Rendition definition not found in  registry: " + renditionName);
            renditionDefinition = new TransformDefinition(transformName, targetMimetype, Map.of(), null,
                    null, null, renditionRegistry);
        }

        ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
        if (contentData != null && contentData.getContentUrl() != null) {
            String contentUrl = contentData.getContentUrl();
            String sourceMimetype = contentData.getMimetype();

            // needs to be called so that correct client is used
            localTransformClient.checkSupported(nodeRef,renditionDefinition,sourceMimetype,reader.getSize(),contentUrl);
            LocalTransform localTransform = getLocalTransform(localTransformClient);
            ContentWriter writer = contentService.getTempWriter();
            writer.setMimetype(targetMimetype);
            localTransform.transform(reader,writer,renditionDefinition.getTransformOptions(),renditionName,nodeRef);
            return writer.getReader().getContentInputStream();
        }
        return null;
    }

    LocalTransform getLocalTransform(LocalTransformClient client){
        try {
            Field transformField = null;
            transformField = LocalTransformClient.class.getDeclaredField("transform");
            transformField.setAccessible(true);  // Make it accessible
            // Read the ThreadLocal<LocalTransform>
            ThreadLocal<?> threadLocal = (ThreadLocal<?>) transformField.get(client);
            // Read the current LocalTransform value (for the current thread)
            Object localTransform = threadLocal.get();
            return (LocalTransform) localTransform;
        } catch (NoSuchFieldException e) {
            log.error(e.getMessage(),e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(),e);
        }
        return null;
    }

    private Map<String, Serializable> readMetadata(InputStream transformInputStream)
    {
        try
        {

            TypeReference<HashMap<String, Serializable>> typeRef = new TypeReference<HashMap<String, Serializable>>() {};
            return jsonObjectMapper.readValue(transformInputStream, typeRef);
        }
        catch (IOException e)
        {
            log.error("Failed to read metadata from transform result", e);
            return null;
        }
    }

    public String transformToText(NodeRef nodeRef) {
        try (InputStream is = getInputStream(nodeRef, "text/plain")) {
            if (is == null) {
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to transform node to plain text: {}", nodeRef.getId(), e);
            throw new RuntimeException("Transform to text failed for node " + nodeRef.getId(), e);
        }
    }

    public static final int SOURCE_HAS_NO_CONTENT = -1;

    private int getSourceContentHashCode(NodeRef sourceNodeRef)
    {
        int hashCode = SOURCE_HAS_NO_CONTENT;
        ContentData contentData = DefaultTypeConverter.INSTANCE.convert(ContentData.class, nodeService.getProperty(sourceNodeRef, PROP_CONTENT));
        if (contentData != null)
        {
            // Originally we used the contentData URL, but that is not enough if the mimetype changes.
            String contentString = contentData.getContentUrl()+contentData.getMimetype();
            if (contentString != null)
            {
                hashCode = contentString.hashCode();
            }
        }
        return hashCode;
    }
}
