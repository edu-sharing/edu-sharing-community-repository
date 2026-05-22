package org.edu_sharing.service.transform;

import jakarta.ws.rs.core.Response;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.bapi.BApiProxyConfig;
import org.edu_sharing.service.bapi.BApiProxyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FulltextServiceTest {

    private static final QName STATUS_QNAME = QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_STATUS);
    private static final QName CONTENT_QNAME = QName.createQName(CCConstants.CCM_PROP_IO_FULLTEXT_CONTENT);
    private static final QName WWWURL_QNAME = QName.createQName(CCConstants.CCM_PROP_IO_WWWURL);

    private ContentService contentService;
    private NodeService nodeService;
    private TransformService transformService;
    private BApiProxyConfig bApiProxyConfig;
    private BApiProxyService bApiProxyService;
    private SimpleCache<String, String> transformerCache;
    private RetryingTransactionHelper retryingTransactionHelper;
    private BehaviourFilter behaviourFilter;
    private ContentReader contentReader;
    private ContentWriter contentWriter;

    private MockedStatic<AlfAppContextGate> alfAppContextGateMock;
    private MockedStatic<AuthenticationUtil> authUtilMock;
    private MockedConstruction<RepositoryCache> repositoryCacheMock;

    private FulltextService underTest;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        contentService = Mockito.mock(ContentService.class);
        nodeService = Mockito.mock(NodeService.class);
        transformService = Mockito.mock(TransformService.class);
        bApiProxyConfig = Mockito.mock(BApiProxyConfig.class);
        bApiProxyService = Mockito.mock(BApiProxyService.class);
        transformerCache = Mockito.mock(SimpleCache.class);
        retryingTransactionHelper = Mockito.mock(RetryingTransactionHelper.class);
        behaviourFilter = Mockito.mock(BehaviourFilter.class);
        contentReader = Mockito.mock(ContentReader.class);
        contentWriter = Mockito.mock(ContentWriter.class);

        ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
        when(applicationContext.getBean("eduSharingPropertiesCache")).thenReturn(Mockito.mock(SimpleCache.class));
        alfAppContextGateMock = Mockito.mockStatic(AlfAppContextGate.class);
        alfAppContextGateMock.when(AlfAppContextGate::getApplicationContext).thenReturn(applicationContext);

        repositoryCacheMock = Mockito.mockConstruction(RepositoryCache.class);
        authUtilMock = Mockito.mockStatic(AuthenticationUtil.class);
        authUtilMock.when(() -> AuthenticationUtil.runAsSystem(any())).thenAnswer(invocation ->
                ((AuthenticationUtil.RunAsWork<?>) invocation.getArgument(0)).doWork()
        );
        when(retryingTransactionHelper.doInTransaction(any())).thenAnswer(invocation ->
                ((RetryingTransactionHelper.RetryingTransactionCallback<?>) invocation.getArgument(0)).execute()
        );

        underTest = new FulltextService(contentService, nodeService, transformService, bApiProxyConfig, bApiProxyService, transformerCache, retryingTransactionHelper, behaviourFilter);
    }

    @AfterEach
    void afterEach() {
        repositoryCacheMock.close();
        authUtilMock.close();
        alfAppContextGateMock.close();
    }

    // --- cache-hit path ---

    @Test
    void getFulltext_returnsCachedContent_whenStatusIsSet() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(FulltextStatus.CONTENT_AVAILABLE.name());
        when(contentService.getReader(any(), eq(CONTENT_QNAME))).thenReturn(contentReader);
        when(contentReader.exists()).thenReturn(true);
        when(contentReader.getContentString()).thenReturn("cached text");

        String result = underTest.getFulltext(nodeId, false);

        assertEquals("cached text", result);
        verify(transformService, never()).transformToText(any());
    }

    @Test
    void getFulltext_returnsNull_whenCacheHitButNoReader() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(FulltextStatus.NO_CONTENT.name());
        when(contentService.getReader(any(), eq(CONTENT_QNAME))).thenReturn(null);

        assertNull(underTest.getFulltext(nodeId, false));
        verify(transformService, never()).transformToText(any());
    }

    // --- file node path ---

    @Test
    void getFulltext_extractsAndPersistsFileContent() {
        String nodeId = UUID.randomUUID().toString();
        String extractedText = "extracted plain text";
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn(null);
        when(transformService.transformToText(any())).thenReturn(extractedText);
        when(contentService.getWriter(any(), eq(CONTENT_QNAME), eq(true))).thenReturn(contentWriter);

        String result = underTest.getFulltext(nodeId, false);

        assertEquals(extractedText, result);
        verify(contentWriter).putContent(extractedText);
        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.CONTENT_AVAILABLE.name()));
    }

    @Test
    void getFulltext_persistsNoContent_whenTransformReturnsNull() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn(null);
        when(transformService.transformToText(any())).thenReturn(null);

        assertNull(underTest.getFulltext(nodeId, false));

        verify(contentService, never()).getWriter(any(), any(), anyBoolean());
        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.NO_CONTENT.name()));
    }

    @Test
    void getFulltext_persistsInternalError_whenTransformThrows() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn(null);
        when(transformService.transformToText(any())).thenThrow(new RuntimeException("transform failed"));

        assertNull(underTest.getFulltext(nodeId, false));

        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.TRANSFORM_ERROR_INTERNAL.name()));
    }

    @Test
    void getFulltext_persistsUnsupportedError_whenTransformUnsupported() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn(null);
        when(transformService.transformToText(any())).thenThrow(
                new UnsupportedOperationException("Local transform text/plain/image/jpeg from image/jpeg is unsupported"));

        assertNull(underTest.getFulltext(nodeId, false));

        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.TRANSFORM_ERROR_UNSUPPORTED.name()));
    }

    // --- link node / BAPI path ---

    @Test
    void getFulltext_extractsViaBapi_forLinkNode() {
        String nodeId = UUID.randomUUID().toString();
        String url = "https://example.com";
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn(url);
        when(bApiProxyConfig.getUri()).thenReturn("https://bapi.example.com");
        when(bApiProxyConfig.getFeatures()).thenReturn(List.of("url-fulltext"));

        Response response = Mockito.mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity()).thenReturn("{\"text\":\"bapi extracted text\"}");
        when(bApiProxyService.forwardRequest(anyString(), anyString(), isNull(), eq(HttpMethod.POST))).thenReturn(response);
        when(contentService.getWriter(any(), eq(CONTENT_QNAME), eq(true))).thenReturn(contentWriter);

        String result = underTest.getFulltext(nodeId, false);

        assertEquals("bapi extracted text", result);
        verify(bApiProxyService).forwardRequest(contains("from-url"), contains(url), isNull(), eq(HttpMethod.POST));
        verify(contentWriter).putContent("bapi extracted text");
    }

    @Test
    void getFulltext_returnsNull_whenLinkNodeAndBapiDisabled() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn("https://example.com");
        when(bApiProxyConfig.getUri()).thenReturn(null);

        assertNull(underTest.getFulltext(nodeId, false));

        verify(bApiProxyService, never()).forwardRequest(any(), any(), any(), any());
        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.NO_CONTENT.name()));
    }

    @Test
    void getFulltext_persistsExternalError_whenBapiReturnsErrorStatus() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        when(nodeService.getProperty(any(), eq(WWWURL_QNAME))).thenReturn("https://example.com");
        when(bApiProxyConfig.getUri()).thenReturn("https://bapi.example.com");
        when(bApiProxyConfig.getFeatures()).thenReturn(List.of("url-fulltext"));

        Response errorResponse = Mockito.mock(Response.class);
        when(errorResponse.getStatus()).thenReturn(500);
        when(bApiProxyService.forwardRequest(any(), any(), any(), any())).thenReturn(errorResponse);

        assertNull(underTest.getFulltext(nodeId, false));

        verify(transformService, never()).transformToText(any());
        verify(nodeService).setProperty(any(), eq(STATUS_QNAME), eq(FulltextStatus.TRANSFORM_ERROR_EXTERNAL.name()));
    }

    // --- semaphore path ---

    @Test
    @SuppressWarnings("unchecked")
    void getFulltext_waitsAndReadsCacheWhenSemaphoreAlreadyHeld() {
        String nodeId = UUID.randomUUID().toString();
        when(nodeService.getProperty(any(), eq(STATUS_QNAME))).thenReturn(null);
        // Semaphore already held: contains returns true once (blocks acquire), then false (exits wait loop)
        when(transformerCache.contains(nodeId)).thenReturn(true, false);
        when(contentService.getReader(any(), eq(CONTENT_QNAME))).thenReturn(contentReader);
        when(contentReader.exists()).thenReturn(true);
        when(contentReader.getContentString()).thenReturn("waited text");

        String result = underTest.getFulltext(nodeId, false);

        assertEquals("waited text", result);
        verify(transformService, never()).transformToText(any());
    }
}