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
import org.edu_sharing.service.permission.annotation.Permission;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BApiProxyService {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private final BApiProxyConfig bApiProxyConfig;

    private final GuestService guestService;

    private final List<String> ignoreHeader = List.of(
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.COOKIE,
            "JSESSIONID",
            "accept-encoding",
            "host"
    );

    /**
     * forward including auth headers based on user
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
                if (ignoreHeader.stream().anyMatch(key::equalsIgnoreCase)) {
                    return;
                }
                values.forEach(value -> requestBuilder.header(key, value));
            });
        }

        requestBuilder.header("X-API-KEY", apiKey);
        requestBuilder.header("X-Edu-User-Id", authenticatedUser);

        try (okhttp3.Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
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
