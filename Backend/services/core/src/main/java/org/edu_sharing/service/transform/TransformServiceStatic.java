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
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
        MultiValueMap<String, Object> body = getMultiValueMap(inputStream, contentLength, sourceMimetype, targetMimetype);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Send it
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<T> response = restTemplate.exchange(
                globalProperties.getProperty("localTransform." + transformerId.getValue() + ".url")+"transform",
                HttpMethod.POST,
                requestEntity,
                clazz
        );

        return response.getBody();
    }

    public InputStream callTransformerForStream(
            InputStream inputStream,
            long contentLength,
            String sourceMimetype,
            String targetMimetype,
            TransformerId transformerId) {

        MultiValueMap<String, Object> body = getMultiValueMap(inputStream, contentLength, sourceMimetype, targetMimetype);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // IMPORTANT: Accept binary stream
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM));

        HttpEntity<MultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

        // Expect a Resource as response
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Resource> response = restTemplate.exchange(
                globalProperties.getProperty("localTransform." + transformerId.getValue() + ".url") + "transform",
                HttpMethod.POST,
                requestEntity,
                Resource.class
        );

        // Convert the returned Resource to InputStream
        try {
            return response.getBody().getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read transformer response stream", e);
        }
    }

    @NotNull
    private static MultiValueMap<String, Object> getMultiValueMap(InputStream inputStream, long contentLength, String sourceMimetype, String targetMimetype) {
        InputStreamResource resource = getInputStreamResource(inputStream, contentLength);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("sourceMimetype", sourceMimetype);
        body.add("targetMimetype", targetMimetype);
        return body;
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
