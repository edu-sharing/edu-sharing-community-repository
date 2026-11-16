package org.edu_sharing.service.bapi;

import co.elastic.clients.util.ContentType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.security.HomeRepositorySigner;
import org.edu_sharing.repository.server.tools.security.RequestUserSigner;
import org.edu_sharing.service.permission.annotation.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BApiProxyService {

    private static final Logger log = LoggerFactory.getLogger(BApiProxyService.class);

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final BApiProxyConfig bApiProxyConfig;

    private final GuestService guestService;

    private final RequestUserSigner requestUserSigner;

    private final List<String> ignoreHeader = List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "JSESSIONID",
            "accept-encoding",
            "host"
    );

    /**
     * Headers of the client that never go on to the B-API: the repository sets these itself. {@code X-Edu-*} is what the B-API takes as the word
     * of the repository (the user assertion it vouches with), {@code X-API-KEY} is the key of the repository.
     */
    private static boolean isReservedHeader(String name) {
        return name.regionMatches(true, 0, "x-edu-", 0, 6) || "x-api-key".equalsIgnoreCase(name);
    }

    /**
     * forward including auth headers based on user. The user the B-API acts for is signed with the key of this repository (see
     * {@link RequestUserSigner}, header {@code X-Edu-User-Assertion}): without it the B-API does not accept it.
     * @param path bapi target path (without leading "/")
     */
    @Permission(value = CCConstants.CCM_VALUE_TOOLPERMISSION_BAPI)
    public Response forwardRequest(String path, String body, HttpHeaders headers, HttpMethod method) {
        String authenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
        String apiKey = guestService.isGuestUser(authenticatedUser) ?
                bApiProxyConfig.getGuestUserApiKey() : bApiProxyConfig.getAuthUserApiKey();

        if (StringUtils.isBlank(bApiProxyConfig.getUri()) || StringUtils.isBlank(apiKey)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .callTimeout(Duration.parse(bApiProxyConfig.getCallTimeout()))
                .connectTimeout(Duration.parse(bApiProxyConfig.getCallTimeout()))
                .writeTimeout(Duration.parse(bApiProxyConfig.getCallTimeout()))
                .readTimeout(Duration.parse(bApiProxyConfig.getCallTimeout()))
                // a second try would send the same jti again, which the B-API refuses as a replay
                .retryOnConnectionFailure(false)
                .build();
        String uri = bApiProxyConfig.getUri();
        if(!uri.endsWith("/")) {
            uri = uri + "/";
        }
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(uri.concat(path));

        RequestBody requestBody = null;
        if (StringUtils.isNotBlank(body)) {
            requestBody = RequestBody.create(body, MediaType.parse("application/json"));
        }

        requestBuilder.method(method.name(), requestBody);
        if (headers != null) {
            headers.getRequestHeaders().forEach((key, values) -> {
                if (ignoreHeader.stream().anyMatch(key::equalsIgnoreCase) || isReservedHeader(key)) {
                    return;
                }
                values.forEach(value -> requestBuilder.header(key, value));
            });
        }

        requestBuilder.header("X-API-KEY", apiKey);
        Request unsigned = requestBuilder.build();

        Request request;
        try {
            okhttp3.HttpUrl url = unsigned.url();
            String pathAndQuery = url.encodedPath() + (url.encodedQuery() != null ? "?" + url.encodedQuery() : "");
            String assertion = requestUserSigner.sign(bApiProxyConfig.getAudience(), apiKey, method.name(), pathAndQuery, authenticatedUser);
            request = unsigned.newBuilder().header(RequestUserSigner.HEADER, assertion).build();
        } catch (HomeRepositorySigner.SigningFailedException e) {
            // this repository cannot vouch for the user (no key): nothing is sent, the B-API would refuse it anyway
            log.error("bapi proxy: the user could not be signed: {}", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        }

        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            Response.ResponseBuilder result = Response.status(response.code());
            if (response.body() != null) {
                result.entity(response.body().string());
                if(response.body().contentType() != null) {
                    result.type(String.valueOf(response.body().contentType()));
                } else {
                    result.type(ContentType.APPLICATION_JSON);
                }
            }
            return result.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
