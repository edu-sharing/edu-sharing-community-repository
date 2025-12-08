package org.edu_sharing.service.transform;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service class for handling representation-related operations, specifically for managing and updating
 * PDF representations of nodes including content transformations and metadata processing.
 * <p>
 * This service interacts with node association, content transformation, and template rendering components.
 * It provides functionality to either update existing PDF nodes or create new ones based on the specific
 * parameters of the original content node.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepresentationService {

    private final NodeService nodeService;
    private final ContentService contentService;
    private final TransformService eduTransformService;

    private final TransformServiceStatic eduTransformServiceStatic;

    private final MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();

    private final TemplateEngine templateEngine;

    public static final String METADATA_PIXEL_X = "{http://www.alfresco.org/model/exif/1.0}pixelXDimension";
    public static final String METADATA_PIXEL_Y = "{http://www.alfresco.org/model/exif/1.0}pixelYDimension";


    private String getImageWrappedInHtml(NodeRef nodeRef, ContentReader contentReader) {
        try (InputStream is = contentReader.getContentInputStream()) {
            final var bytes = is.readAllBytes();

            String b64Image = Base64.getEncoder().encodeToString(bytes);

            Context context = new Context();
            context.setVariable("imageData", b64Image);
            context.setVariable("mimeType", contentReader.getMimetype());

            // width and height aren't already set in node metadata so call transformer here to get dimensions
            Map<String, Serializable> result = eduTransformService.transform(nodeRef, "alfresco-metadata-extract", Map.class);
            String width, height;
            if (result.containsKey(METADATA_PIXEL_X)) {
                width = (String) result.get(METADATA_PIXEL_X);
                context.setVariable("pageWidth", width);
            }

            if (result.containsKey(METADATA_PIXEL_Y)) {
                height = (String) result.get(METADATA_PIXEL_Y);
                // + 10 to prevent the second blank page
                context.setVariable("pageHeight", "" + (Integer.parseInt(height) + 10));
            }

            return templateEngine.process("html/transform/image-template.html", context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Updates the child PDF node associated with the specified origin node.
     *
     * @param originNodeRef    the reference to the original node
     * @param childName        the name of the child node
     * @param nodeType         the type of the node
     * @param childAssociation the type of the child association
     * @return the reference to the updated child PDF node, or null if the update is unsuccessful
     */
    public NodeRef updateChildPdf(@NonNull @NotNull NodeRef originNodeRef, String childName, @NonNull @NotNull String nodeType, @NonNull @NotNull String childAssociation) {

        List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(originNodeRef, Set.of(QName.createQName(nodeType)));
        childAssocs = childAssocs
                .stream()
                .filter(c -> c.getTypeQName().equals(QName.createQName(childAssociation))
                        && nodeService.hasAspect(c.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_REPRESENTATION)))
                .toList();
        if (childAssocs.size() > 1) {
            log.warn("to many children found for criteria");
            return null;
        }

        NodeRef pdfNodeRef = childAssocs.stream().findFirst().map(ChildAssociationRef::getChildRef).orElse(null);
        return updateChildPdf(originNodeRef, originNodeRef, pdfNodeRef, childName, nodeType, childAssociation);
    }

    /**
     * Updates or creates a PDF representation of a child node based on the provided parameters. If a PDF node reference is
     * not provided, a new PDF node is created as a child of the specified parent node. The method also handles the content
     * transformation from the original node to a PDF format.
     *
     * @param originNodeRef    the node reference of the original content node to be transformed to PDF
     * @param parentNodeRef    the node reference of the parent node under which the child PDF node will reside
     * @param pdfNodeRef       an optional node reference for an existing PDF node to update; if null, a new node is created
     * @param childName        the optional name of the child PDF node; if null, the name is derived from the original node
     * @param nodeType         the type of the child node to create if a new node is required
     * @param childAssociation the child association type to use when creating the new node
     * @return the reference to the created or updated PDF node, or null if the transformation or creation fails
     */
    public NodeRef updateChildPdf(@NonNull @NotNull NodeRef originNodeRef, @NonNull @NotNull NodeRef parentNodeRef, NodeRef pdfNodeRef, String childName, @NonNull @NotNull String nodeType, @NonNull @NotNull String childAssociation) {

        if(childName == null) {
            String name = (String) nodeService.getProperty(originNodeRef, ContentModel.PROP_NAME);
            childName =  name + ".pdf";
        }

        ContentReader contentReader = contentService.getReader(originNodeRef, ContentModel.PROP_CONTENT);
        if (contentReader == null) {
            return null;
        }
        String imageInHtml = null;
        if (contentReader.getMimetype().startsWith("image")) {
            imageInHtml = getImageWrappedInHtml(originNodeRef, contentReader);
        }

        try (InputStream inputStream = (imageInHtml != null)
                ? getHtmlToPDF(imageInHtml)
                : eduTransformService.getInputStream(originNodeRef, MimetypeMap.MIMETYPE_PDF)) {

            if (inputStream == null) {
                log.warn("transform {} to pdf failed", originNodeRef.getId());
                return null;
            }

            if (pdfNodeRef == null) {
                String assocName = QName.createValidLocalName(childName);
                assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;
                pdfNodeRef = nodeService.createNode(parentNodeRef, QName.createQName(childAssociation), QName.createQName(assocName), QName.createQName(nodeType), Map.of(ContentModel.PROP_NAME, childName)).getChildRef();
                nodeService.addAspect(pdfNodeRef, QName.createQName(CCConstants.CCM_ASPECT_REPRESENTATION), null);
            }

            apiClient.writeContent(pdfNodeRef.getStoreRef(), pdfNodeRef.getId(), inputStream, MimetypeMap.MIMETYPE_PDF, null, CCConstants.CM_PROP_CONTENT);
            return pdfNodeRef;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedOperationException e) {
            log.warn("can not transform {} to pdf", originNodeRef.getId());
            return null;
        }
    }

    public InputStream getHtmlToPDF(String imageInHtml) {
        try (InputStream isHtml = IOUtils.toInputStream(imageInHtml, StandardCharsets.UTF_8)) {
            return eduTransformServiceStatic.callTransformerForStream(isHtml, imageInHtml.getBytes(StandardCharsets.UTF_8).length, "text/html", "application/pdf", TransformServiceStatic.TransformerId.EDU_SHARING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
