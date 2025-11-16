package org.edu_sharing.service.transform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.IOUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
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


    String getImageWrappedInHtml(NodeRef nodeRef,ContentReader contentReader) throws IOException {
        try(InputStream is = contentReader.getContentInputStream()){
            final var bytes = is.readAllBytes();

            String b64Image = Base64.getEncoder().encodeToString(bytes);

            Context context = new Context();
            context.setVariable("imageData", b64Image);
            context.setVariable("mimeType",contentReader.getMimetype());

            // width and height not already set in node metadata so call transformer here to get dimensions
            Map<String, Serializable> result  = eduTransformService.transform(nodeRef, "alfresco-metadata-extract", Map.class);
            String width,  height = null;
            if(result.containsKey(METADATA_PIXEL_X)){
                width = (String)result.get(METADATA_PIXEL_X);
                context.setVariable("pageWidth",width);
            }

            if(result.containsKey(METADATA_PIXEL_Y)){
                height = (String)result.get(METADATA_PIXEL_Y);
                // + 10 to prevent second blank page
                context.setVariable("pageHeight",""+(Integer.parseInt(height)+10));
            }

            return templateEngine.process("html/transform/image-template.html", context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public NodeRef updateChildPdf(NodeRef nodeRef, String childName, String nodeType, String childAssociation) throws Throwable {

        String name = (String)nodeService.getProperty(nodeRef,ContentModel.PROP_NAME);
        String newName = (childName == null) ? name + ".pdf" : childName;

        ContentReader contentReader =  contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        if(contentReader == null){
            return null;
        }
        String imageInHtml = null;
        if(contentReader.getMimetype().startsWith("image")){
            imageInHtml =  getImageWrappedInHtml(nodeRef,contentReader);
        }

        try(InputStream inputStream = (imageInHtml != null)
                ? getHtmlToPDF(imageInHtml)
                : eduTransformService.getInputStream(nodeRef, MimetypeMap.MIMETYPE_PDF)){

            if(inputStream == null){
                log.warn("transform " + name +" to pdf failed");
                return null;
            }

            List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef, Set.of(QName.createQName(nodeType)));
            childAssocs = childAssocs.stream().filter(c ->
                    c.getTypeQName().equals(QName.createQName(childAssociation)) &&
                            nodeService.hasAspect(c.getChildRef(),QName.createQName(CCConstants.CCM_ASPECT_REPRESENTATION))).toList();
            if(childAssocs.size() > 1){
                log.warn("to many childs found for criterias");
                return null;
            }


            NodeRef child = (childAssocs.size() == 0) ? null : childAssocs.get(0).getChildRef();
            if(child == null){
                String assocName = QName.createValidLocalName(newName);
                assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + assocName;
                child = nodeService.createNode(nodeRef,QName.createQName(childAssociation), QName.createQName(assocName),QName.createQName(nodeType),Map.of(ContentModel.PROP_NAME,newName)).getChildRef();
                nodeService.addAspect(child,QName.createQName(CCConstants.CCM_ASPECT_REPRESENTATION),null);
            }


            apiClient.writeContent(child.getStoreRef(),child.getId(),inputStream,MimetypeMap.MIMETYPE_PDF,null,CCConstants.CM_PROP_CONTENT);
            return child;
        }catch(java.lang.UnsupportedOperationException e){
            log.warn("can not transform " + name +" to pdf");
            return null;
        }
    }

    public InputStream getHtmlToPDF(String imageInHtml){
        try(InputStream isHtml = IOUtils.toInputStream(imageInHtml, StandardCharsets.UTF_8)){
            return eduTransformServiceStatic.callTransformerForStream(isHtml,imageInHtml.getBytes(StandardCharsets.UTF_8).length,"text/html","application/pdf", TransformServiceStatic.TransformerId.EDU_SHARING);
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
