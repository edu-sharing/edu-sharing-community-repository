package org.edu_sharing.repository.server.oembed;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.edu_sharing.restservices.DAOMissingException;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.rendering.RenderingServiceFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class oEmbedServletTest {

    @Test
    void parseRenderUrlReturnsTheNodeId() {
        // Arrange
        String url = "https://host/edu-sharing/components/render/6b8e1c52-aaaa-bbbb-cccc-0123456789ab";

        // Act
        String[] target = oEmbedServlet.parseRenderUrl(url);

        // Assert
        assertArrayEquals(new String[]{"6b8e1c52-aaaa-bbbb-cccc-0123456789ab", null}, target);
    }

    @Test
    void parseRenderUrlReturnsTheVersion() {
        // Arrange
        String url = "https://host/edu-sharing/components/render/6b8e1c52-aaaa-bbbb-cccc-0123456789ab/1.0?x=y";

        // Act
        String[] target = oEmbedServlet.parseRenderUrl(url);

        // Assert
        assertArrayEquals(new String[]{"6b8e1c52-aaaa-bbbb-cccc-0123456789ab", "1.0"}, target);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not a url", "https://host", "https://host/edu-sharing/components/render", "https://host/edu-sharing/components/render/"})
    void parseRenderUrlRejectsMissingAndInvalidUrls(String url) {
        // Arrange (the url of the parameter)

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> oEmbedServlet.parseRenderUrl(url));

        // Assert
        org.junit.jupiter.api.Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void missingUrlParameterIsABadRequest() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        new oEmbedServlet().doGet(request, response);

        // Assert
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
        verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
    }

    @Test
    void missingNodeIsNotFound() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("url")).thenReturn("https://host/edu-sharing/components/render/0000-missing");
        try (MockedStatic<RenderingServiceFactory> renderingFactory = Mockito.mockStatic(RenderingServiceFactory.class);
             MockedStatic<RepositoryDao> repositoryDao = Mockito.mockStatic(RepositoryDao.class);
             MockedStatic<NodeDao> nodeDao = Mockito.mockStatic(NodeDao.class)) {
            renderingFactory.when(RenderingServiceFactory::getInstance).thenReturn(mock(RenderingServiceFactory.class));
            nodeDao.when(() -> NodeDao.getNodeWithVersion(any(), eq("0000-missing"), any()))
                    .thenThrow(new DAOMissingException(new IllegalArgumentException("Node does not exist")));

            // Act
            new oEmbedServlet().doGet(request, response);

            // Assert
            verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
            verify(response, never()).sendError(eq(HttpServletResponse.SC_INTERNAL_SERVER_ERROR), anyString());
        }
    }

    @Test
    void otherErrorsStayInternalServerErrors() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("url")).thenReturn("https://host/edu-sharing/components/render/0000-broken");
        try (MockedStatic<RenderingServiceFactory> renderingFactory = Mockito.mockStatic(RenderingServiceFactory.class);
             MockedStatic<RepositoryDao> repositoryDao = Mockito.mockStatic(RepositoryDao.class);
             MockedStatic<NodeDao> nodeDao = Mockito.mockStatic(NodeDao.class)) {
            renderingFactory.when(RenderingServiceFactory::getInstance).thenReturn(mock(RenderingServiceFactory.class));
            nodeDao.when(() -> NodeDao.getNodeWithVersion(any(), eq("0000-broken"), any()))
                    .thenThrow(new IllegalStateException("backend down"));

            // Act
            new oEmbedServlet().doGet(request, response);

            // Assert
            verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "backend down");
        }
    }

    @Test
    void invalidSizeParameterIsABadRequest() throws Exception {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("url")).thenReturn("https://host/edu-sharing/components/render/abc");
        when(request.getParameter("maxwidth")).thenReturn("wide");

        // Act
        new oEmbedServlet().doGet(request, response);

        // Assert
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }
}
