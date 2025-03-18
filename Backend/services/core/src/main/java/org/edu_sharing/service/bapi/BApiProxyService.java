package org.edu_sharing.service.bapi;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.permission.annotation.Permission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BApiProxyService {

    @Value("${repository.bapi.uri:}")
    private String bapiUri;

    @Value("${repository.bapi.apiKey:}")
    private String apiKey;

    @Permission(value = CCConstants.CCM_VALUE_TOOLPERMISSION_BAPI, requiresUser = true)
    public Response forwardRequest(String path, String body, HttpHeaders headers, HttpMethod method) {
        if (StringUtils.isBlank(bapiUri) || StringUtils.isBlank(apiKey)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        OkHttpClient okHttpClient = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(bapiUri.concat(path));

        RequestBody requestBody = null;
        if (StringUtils.isNotBlank(body)) {
            requestBody = RequestBody.create(body, MediaType.parse("application/json"));
        }

        requestBuilder.method(method.name(), requestBody);
        headers.getRequestHeaders().forEach((key, values) -> {
            if (key.equalsIgnoreCase(HttpHeaders.AUTHORIZATION) || key.equalsIgnoreCase(HttpHeaders.COOKIE) || key.equalsIgnoreCase("JSESSIONID")) {
                return;
            }
            values.forEach(value -> requestBuilder.header(key, value));
        });
        requestBuilder.header("X-API-KEY", apiKey);

        try (okhttp3.Response response = okHttpClient.newCall(requestBuilder.build()).execute()) {
            Response.ResponseBuilder result = Response.status(response.code());
            if (response.body() != null) {
                result.entity(response.body().string());
            }
            return result.build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
