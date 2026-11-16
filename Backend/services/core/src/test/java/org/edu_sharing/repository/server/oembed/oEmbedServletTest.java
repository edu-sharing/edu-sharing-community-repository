package org.edu_sharing.repository.server.oembed;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
