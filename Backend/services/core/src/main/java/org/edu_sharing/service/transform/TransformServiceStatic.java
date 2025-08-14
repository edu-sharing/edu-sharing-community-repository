package org.edu_sharing.service.transform;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

@Slf4j
@Service("EduTransformServiceStatic")
public class TransformServiceStatic {

    @Autowired
    ContentService contentService;

    @Autowired
    Properties globalProperties;

    @Getter
    public enum TransformerId {
        EDU_SHARING("edu-sharing"),
        CORE_AIO("core-aio"),
        JODCONVERTER("jodconverter");

        private final String value;
        TransformerId(String s) {
            this.value = s;
        }
    }

    /**
     *
     * @param nodeRef
     * @param targetMimetype i.e: "alfresco-metadata-extract"
     * @param transformerId
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T callTransformer(NodeRef nodeRef, String targetMimetype, TransformerId transformerId, Class<T> clazz) {
        ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
        String sourceMimetype = reader.getMimetype();
        return callTransformer(reader.getContentInputStream(),reader.getSize(),sourceMimetype,targetMimetype,transformerId,clazz);
    }

    public <T> T callTransformer(InputStream inputStream, long contentLength, String sourceMimetype, String targetMimetype, TransformerId transformerId, Class<T> clazz) {
        InputStreamResource resource = getInputStreamResource(inputStream, contentLength);

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("sourceMimetype", sourceMimetype);
        body.add("targetMimetype", targetMimetype);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Send it
        ResponseEntity<T> response = restTemplate.exchange(
                globalProperties.getProperty("localTransform." + transformerId.getValue() + ".url")+"transform",
                HttpMethod.POST,
                requestEntity,
                clazz
        );

        return response.getBody();
    }


    /**
     *
     * @param inputStream
     * @param contentLength: Must be set to avoid chunked uploads
     * @return
     */
    @NotNull
    private static InputStreamResource getInputStreamResource( InputStream inputStream, long contentLength ) {

        // required for multipart/form-data
        return new InputStreamResource(inputStream) {
            @Override
            public String getFilename() {
                return "content.bin";  // required for multipart/form-data
            }

            @Override
            public long contentLength() {
                return contentLength;
            }
        };
    }
}
