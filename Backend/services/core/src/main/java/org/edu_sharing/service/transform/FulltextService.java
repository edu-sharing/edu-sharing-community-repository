package org.edu_sharing.service.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.bapi.BApiProxyConfig;
import org.edu_sharing.service.bapi.BApiProxyService;
import org.edu_sharing.service.nodeservice.annotation.NodeManipulation;
import org.edu_sharing.service.nodeservice.annotation.NodeReferenceOriginal;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulltextService {

    private static final Duration SEMAPHORE_WAIT_TIMEOUT = Duration.ofSeconds(65);
    private static final Duration SEMAPHORE_POLL_INTERVAL = Duration.ofMillis(100);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String BAPI_FULLTEXT_PATH = "api/v1/proxy/text-extraction/from-url";

    private final ContentService contentService;
    private final NodeService nodeService;
    private final TransformService transformService;
    private final BApiProxyConfig bApiProxyConfig;
    private final BApiProxyService bApiProxyService;
    private final SimpleCache<String, String> eduSharingTransformerCache;
    private final RetryingTransactionHelper retryingTransactionHelper;
    private final BehaviourFilter behaviourFilter;
    private final RepositoryCache repositoryCache;

    private static class ExternalTransformException extends RuntimeException {}
    private static class InternalTransformException extends RuntimeException {
        InternalTransformException(Throwable cause) { super(cause); }
    }
    private static class UnsupportedTransformException extends RuntimeException {
        UnsupportedTransformException(Throwable cause) { super(cause); }
    }


    /**
     * Returns the plain text content for a node.
     * Caches the result in ccm:fulltext_content / ccm:fulltext_status.
     * Uses eduSharingTransformerCache (SimpleCache, TTL 60 s) as a semaphore to prevent duplicate runs.
     * When forceUpdate is true the cached status is ignored and text is always re-extracted and re-stored.
     */
    @NodeManipulation
    public String getFulltext(@NodeReferenceOriginal String nodeId, boolean forceUpdate) {
        NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
        if (!forceUpdate) {
            String status = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_STATUS));
            if (status != null) {
                return readCachedFulltext(nodeRef);
            }
        }

        String cacheKey = nodeRef.getId();
        if (!acquireSemaphore(cacheKey)) {
            log.info("Fulltext transformation already in progress for node {}, waiting", nodeRef.getId());
            awaitSemaphoreRelease(cacheKey);
            return readCachedFulltext(nodeRef);
        }

        try {
            String text = extractText(nodeRef);
            persist(nodeRef, text, text != null && !text.isBlank() ? FulltextStatus.CONTENT_AVAILABLE : FulltextStatus.NO_CONTENT);
            return text;
        } catch (ExternalTransformException e) {
            persist(nodeRef, null, FulltextStatus.TRANSFORM_ERROR_EXTERNAL);
            return null;
        } catch (UnsupportedTransformException e) {
            log.debug("Transform unsupported for node {}: {}", nodeRef.getId(), e.getCause().getMessage());
            persist(nodeRef, null, FulltextStatus.TRANSFORM_ERROR_UNSUPPORTED);
            return null;
        } catch (InternalTransformException e) {
            log.warn("Local transform failed for node {}", nodeRef.getId(), e.getCause());
            persist(nodeRef, null, FulltextStatus.TRANSFORM_ERROR_INTERNAL);
            return null;
        } finally {
            releaseSemaphore(cacheKey);
        }
    }

    private boolean acquireSemaphore(String cacheKey) {
        synchronized (eduSharingTransformerCache) {
            if (eduSharingTransformerCache.contains(cacheKey)) {
                return false;
            }
            eduSharingTransformerCache.put(cacheKey, cacheKey);
            return true;
        }
    }

    private void releaseSemaphore(String cacheKey) {
        synchronized (eduSharingTransformerCache) {
            eduSharingTransformerCache.remove(cacheKey);
        }
    }

    private void awaitSemaphoreRelease(String cacheKey) {
        long deadline = System.nanoTime() + SEMAPHORE_WAIT_TIMEOUT.toNanos();
        while (eduSharingTransformerCache.contains(cacheKey)) {
            if (System.nanoTime() >= deadline) {
                log.warn("Timed out waiting for fulltext semaphore on node {}", cacheKey);
                return;
            }
            try {
                Thread.sleep(SEMAPHORE_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private String readCachedFulltext(NodeRef nodeRef) {
        ContentReader reader = contentService.getReader(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_CONTENT));
        if (reader == null || !reader.exists()) {
            return null;
        }
        return reader.getContentString();
    }

    private String extractText(NodeRef nodeRef) {
        if (isLink(nodeRef)) {
            if (bapiFulltextEnabled()) {
                return extractTextViaBapi(nodeRef); // throws ExternalTransformException on HTTP/parse errors
            }
            return null;
        }
        try {
            return transformService.transformToText(nodeRef);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedTransformException(e);
        } catch (Exception e) {
            throw new InternalTransformException(e);
        }
    }

    private boolean bapiFulltextEnabled() {
        return StringUtils.isNotBlank(bApiProxyConfig.getUri())
                && bApiProxyConfig.getFeatures().contains("url-fulltext");
    }

    private boolean isLink(NodeRef nodeRef) {
        String wwwUrl = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
        return wwwUrl != null && !wwwUrl.isEmpty();
    }

    /**
     * Extracts plain text from the URL stored in ccm:wwwurl via the BAPI text-extraction proxy.
     * Only invoked for link-type nodes when the {@code url-fulltext} feature is enabled in
     * {@link BApiProxyConfig}. The remote endpoint crawls the URL itself, so no binary content.
     * Throws {@link ExternalTransformException} on non-2xx HTTP status or unparseable response.
     * Returns null when the page was reachable but yielded no text (stored as NO_CONTENT).
     */
    private String extractTextViaBapi(NodeRef nodeRef) {
        String wwwUrl = (String) nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
        String body;
        try {
            body = OBJECT_MAPPER.writeValueAsString(Map.of("url", wwwUrl, "method", "browser"));
        } catch (JsonProcessingException e) {
            log.error("Failed to build BAPI fulltext request body for node {}", nodeRef.getId(), e);
            throw new ExternalTransformException();
        }

        try (Response response = bApiProxyService.forwardRequest(BAPI_FULLTEXT_PATH, body, null, null, HttpMethod.POST)) {
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                log.warn("BAPI fulltext request failed with status {} for node {}", response.getStatus(), nodeRef.getId());
                throw new ExternalTransformException();
            }
            Object entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            JsonNode json = OBJECT_MAPPER.readTree(entity.toString());
            JsonNode textNode = json.get("text");
            return textNode == null || textNode.isNull() ? null : textNode.asText();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse BAPI fulltext response for node {}", nodeRef.getId(), e);
            throw new ExternalTransformException();
        }
    }

    private void persist(NodeRef nodeRef, String text, FulltextStatus status) {
        AuthenticationUtil.runAsSystem(() ->
                retryingTransactionHelper.doInTransaction(() -> {
                    behaviourFilter.disableBehaviour(nodeRef);
                    try {
                        if (text != null && !text.isBlank()) {
                            ContentWriter writer = contentService.getWriter(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_CONTENT), true);
                            writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
                            writer.setEncoding(StandardCharsets.UTF_8.name());
                            writer.putContent(text);
                        }
                        nodeService.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_STATUS), status.name());
                    } finally {
                        behaviourFilter.enableBehaviour(nodeRef);
                        repositoryCache.remove(nodeRef.getId());
                    }
                    return null;
                })
        );
    }
}
