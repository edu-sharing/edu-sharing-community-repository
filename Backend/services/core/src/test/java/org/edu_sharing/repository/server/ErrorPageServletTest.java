package org.edu_sharing.repository.server;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ErrorPageServletTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        body = new StringWriter();
        when(request.getContextPath()).thenReturn("/edu-sharing");
        when(request.getMethod()).thenReturn("GET");
        when(response.getWriter()).thenReturn(new PrintWriter(body, true));
    }

    private void errorDispatch(int statusCode, String message, String accept) {
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(statusCode);
        when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(message);
        when(request.getHeader("accept")).thenReturn(accept);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 405, 500})
    void browsersKeepTheStatusAndAreSentToTheRelativeErrorPage(int statusCode) {
        errorDispatch(statusCode, "failed", "text/html,application/xhtml+xml");

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(statusCode);
        assertEquals("<head><meta http-equiv=\"refresh\" content=\"0;url=/edu-sharing/components/error/" + statusCode + "\"></head>",
                body.toString());
    }

    @Test
    void htmlNeedsNoNonceAndCarriesNoScript() {
        errorDispatch(404, "failed", "text/html");

        new ErrorPageServlet().service(request, response);

        assertFalse(body.toString().contains("<script"));
        assertFalse(body.toString().contains("nonce"));
    }

    @Test
    void jsonClientsGetTheErrorResponse() {
        errorDispatch(404, "not here", "application/json");

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(404);
        verify(response).setHeader("Content-Type", "application/json");
        assertTrue(body.toString().contains("\"error\":\"404\""), body.toString());
    }

    @Test
    void everythingElseGetsPlainText() {
        errorDispatch(403, "denied", null);

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(403);
        verify(response).setHeader("Content-Type", "plain/text");
    }

    /**
     * the windows WebDAV client breaks if a 404 turns into 501 (HttpServlet's answer to an unknown method)
     */
    @ParameterizedTest
    @ValueSource(strings = {"PROPFIND", "PROPPATCH", "MKCOL", "COPY", "MOVE", "LOCK", "UNLOCK", "PATCH", "OPTIONS"})
    void everyMethodKeepsTheOriginalStatus(String method) {
        when(request.getMethod()).thenReturn(method);
        errorDispatch(404, "not here", null);

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(404);
    }

    @Test
    void aDirectRequestWithoutErrorAttributesStillGetsAnAnswer() {
        when(request.getHeader("accept")).thenReturn("text/html");

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(500);
        assertTrue(body.toString().contains("/edu-sharing/components/error/500"), body.toString());
    }

    @Test
    void theStatusOfAnErrorFilterExceptionIsUsedWithoutStatusAttribute() {
        when(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(new ErrorFilter.ErrorFilterException(403));
        when(request.getHeader("accept")).thenReturn("application/json");

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(403);
    }

    @Test
    void statusAttributeAsString() {
        assertEquals(404, ErrorPageServlet.statusCode("404", null));
        assertEquals(500, ErrorPageServlet.statusCode("x", null));
    }

    @Test
    void detailsAreEncodedAndEscaped() {
        when(request.getHeader("accept")).thenReturn("text/html");

        ErrorFilter.handleError(request, response, new SimpleErrorWithDetailsException("a \"b\" <c>"), 500);

        assertEquals("<head><meta http-equiv=\"refresh\" content=\"0;url=/edu-sharing/components/error/OTHER/a%20%22b%22%20%3Cc%3E\"></head>",
                body.toString());
    }

    /**
     * the angular routes are not served by the backend - redirecting to components/error/<code> would loop
     */
    @Test
    void angularRoutesReachingTheBackendGetAStaticPageInsteadOfARefreshLoop() {
        errorDispatch(404, "not here", "text/html");
        when(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)).thenReturn("/edu-sharing/components/error/404");

        new ErrorPageServlet().service(request, response);

        verify(response).setStatus(404);
        assertFalse(body.toString().contains("http-equiv=\"refresh\""), body.toString());
        assertTrue(body.toString().contains("<h1>404</h1>"), body.toString());
    }
}
