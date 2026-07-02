package org.alfresco.repo.webdav.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin client for the logineo-idm-rest authentication endpoint (see openapi.yaml).
 *
 * <p>Performs {@code POST /api/v1/authenticate} against the configured IDM-REST service,
 * authenticating itself with the technical user's HTTP Basic credentials and sending the end
 * user's principal name and password in the JSON body.</p>
 *
 * <p>This class deliberately has <b>no</b> dependency on Alfresco or the servlet API so it can be
 * unit tested in isolation via Spring's {@code MockRestServiceServer}. The mapping of the
 * authenticated principal to an Alfresco authority is the responsibility of
 * {@link IDMRestAuthenticationFilter}.</p>
 */
public class IDMRestClient {

    /** Outcome of a {@code /api/v1/authenticate} call, mapped from the HTTP status code. */
    public enum Result {
        /** {@code 204} – credentials valid. */
        AUTHENTICATED,
        /** {@code 422} – principal authentication failed (wrong password / blocked / unknown). */
        REJECTED,
        /** {@code 401} – the technical user's Basic Auth credentials are invalid (config error). */
        TECHNICAL_AUTH_FAILED,
        /** {@code 400} – malformed request. */
        BAD_REQUEST,
        /** I/O error, timeout or unexpected status code. */
        ERROR
    }

    static final String AUTHENTICATE_PATH = "/api/v1/authenticate";

    private final String authenticateUrl;
    private final String basicAuthHeader;
    private final RestClient restClient;

    /**
     * Production constructor – builds a {@link RestClient} with connect/read timeouts.
     */
    public IDMRestClient(String baseUrl, String technicalUser, String technicalPassword, Duration timeout) {
        this(baseUrl, technicalUser, technicalPassword, RestClient.builder().requestFactory(defaultRequestFactory(timeout)));
    }

    /**
     * Test seam – pass a {@link RestClient.Builder} that has been bound to a
     * {@code MockRestServiceServer} so the HTTP exchange can be stubbed without a real socket.
     */
    public IDMRestClient(String baseUrl, String technicalUser, String technicalPassword, RestClient.Builder builder) {
        String url = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.authenticateUrl = url + AUTHENTICATE_PATH;
        this.basicAuthHeader = "Basic " + Base64.getEncoder()
                .encodeToString((technicalUser + ":" + technicalPassword).getBytes(StandardCharsets.UTF_8));
        this.restClient = builder.build();
    }

    private static ClientHttpRequestFactory defaultRequestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        return factory;
    }

    /**
     * Verify the given principal/password against the IDM-REST service.
     *
     * @return the mapped {@link Result}; never {@code null}
     */
    public Result authenticate(String principalName, String password) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("principalName", principalName);
        payload.put("password", password);

        try {
            // exchange() gives raw access to the status code and, importantly, does NOT apply the
            // default 4xx/5xx error handlers, so 401/422/400 do not throw.
            int status = restClient.post()
                    .uri(authenticateUrl)
                    .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> response.getStatusCode().value());

            switch (status) {
                case 204:
                    return Result.AUTHENTICATED;
                case 422:
                    return Result.REJECTED;
                case 401:
                    return Result.TECHNICAL_AUTH_FAILED;
                case 400:
                    return Result.BAD_REQUEST;
                default:
                    return Result.ERROR;
            }
        } catch (Exception e) {
            // connection refused / timeout / unexpected – caller decides how to log
            return Result.ERROR;
        }
    }

    /** Full URL ({@code <baseUrl>/api/v1/authenticate}) this client posts to. */
    public String getAuthenticateUrl() {
        return authenticateUrl;
    }
}
