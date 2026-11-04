package org.edu_sharing.service.handleservice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.handle.hdllib.Common;
import net.handle.hdllib.GsonUtility;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.Util;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * handle service implementation using the handle.net HTTP JSON REST API (handle server >= v8)
 * see chapter 14 of the HANDLE.NET technical manual
 * <p>
 * Supports two authentication methods:
 * <ul>
 *     <li>pubkey: challenge-response (HS_PUBKEY) via the sessions api, using the configured private key</li>
 *     <li>basic: http basic auth (HS_SECKEY) using the configured secret key</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleServiceRest implements HandleService {

    private static final int RESPONSE_CODE_SUCCESS = 1;

    private final HandleConfig handleConfig;
    private final HandleServiceImpl handleServiceImpl;

    private RestTemplate template = createRestTemplate(false);

    @PostConstruct
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void loadConfig() {
        template = createRestTemplate(Boolean.TRUE.equals(getRestConfig().getTrustAllCertificates()));
        try {
            if (handleConfig.isEnabled() && handleServiceImpl.getMode() == HandleConfig.Mode.rest) {
                Objects.requireNonNull(getRestConfig().getUrl(), "repository.handleservice.rest.url not set");
                if (isBasicAuth()) {
                    Objects.requireNonNull(getRestConfig().getSecretKey(), "repository.handleservice.rest.secretKey not set");
                } else {
                    Objects.requireNonNull(handleConfig.getPrivkey(), "repository.handleservice.privkey not set");
                }
            }
        } catch (Throwable t) {
            log.error("Could not initialize handle rest service properly cause of error in config, please check config for \"repository.handleservice\"", t);
        }
    }

    @Override
    public boolean enabled() {
        return handleConfig.isEnabled();
    }

    /**
     * checks that the server is reachable and the configured identity can authenticate (like the native HSAdapterFactory does)
     */
    @Override
    public boolean available() {
        try {
            if (isBasicAuth()) {
                HttpHeaders headers = getJsonHeaders();
                headers.set(HttpHeaders.AUTHORIZATION, getBasicAuthorization());
                exchange(getHandleUrl(handleServiceImpl.getAdminHandle(), "?publicOnly=false"), HttpMethod.GET, new HttpEntity<>(headers));
            } else {
                closeSession(openSession());
            }
            return true;
        } catch (Exception e) {
            log.info(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String generateId() throws Exception {
        return handleServiceImpl.generateHandle();
    }

    @Override
    public String create(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        // do not overwrite an already existing handle
        putHandle(handleId, handleServiceImpl.getDefautValues(getContentLink(properties)), "?overwrite=false");
        return handleId;
    }

    @Override
    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        // only add or replace the given values, like HSAdapter.updateHandleValues does
        putHandle(handleId, handleServiceImpl.getDefautValues(getContentLink(properties)), "?index=various&overwrite=true");
        return handleId;
    }

    @Override
    public String sync(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        // handleservice has no option to manage metadata
        return handleId;
    }

    @Override
    public String delete(String handleId, String nodeId) throws Exception {
        //not implemented
        return "";
    }

    @Override
    public String getHandleIdProperty() {
        return CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID;
    }

    public void deleteHandle(String handleId) throws Exception {
        execute(HttpMethod.DELETE, getHandleUrl(handleId, ""), null);
    }

    private void putHandle(String handleId, HandleValue[] values, String query) throws Exception {
        JsonObject body = new JsonObject();
        body.add("values", GsonUtility.getGson().toJsonTree(values));
        execute(HttpMethod.PUT, getHandleUrl(handleId, query), body.toString());
    }

    private void execute(HttpMethod method, URI uri, String body) throws Exception {
        String sessionId = isBasicAuth() ? null : openSession();
        try {
            HttpHeaders headers = getJsonHeaders();
            if (sessionId != null) {
                headers.set(HttpHeaders.AUTHORIZATION, getSessionAuthorization(sessionId));
            } else {
                headers.set(HttpHeaders.AUTHORIZATION, getBasicAuthorization());
            }
            ResponseEntity<String> response = exchange(uri, method, new HttpEntity<>(body, headers));
            checkResponse(method + " " + uri.getPath(), response.getBody());
        } finally {
            if (sessionId != null) {
                closeSession(sessionId);
            }
        }
    }

    /**
     * challenge-response authentication via the sessions api (HS_PUBKEY)
     *
     * @return the id of the authenticated session
     */
    private String openSession() throws Exception {
        ResponseEntity<String> created = exchange(getApiUrl("/api/sessions"), HttpMethod.POST, new HttpEntity<>("{}", getJsonHeaders()));
        JsonObject session = JsonParser.parseString(created.getBody()).getAsJsonObject();
        String sessionId = session.get("sessionId").getAsString();
        byte[] nonce = Base64.getDecoder().decode(session.get("nonce").getAsString());
        byte[] cnonce = new byte[16];
        new SecureRandom().nextBytes(cnonce);

        PrivateKey privateKey = handleServiceImpl.getPrivateKey();
        Signature signer = Signature.getInstance(Util.getSigIdFromHashAlgId(new byte[]{Common.HASH_CODE_SHA256}, privateKey.getAlgorithm()));
        signer.initSign(privateKey);
        signer.update(nonce);
        signer.update(cnonce);

        JsonObject auth = new JsonObject();
        auth.addProperty("sessionId", sessionId);
        // must not be percent-encoded in a json entity
        auth.addProperty("id", getAuthId());
        auth.addProperty("type", "HS_PUBKEY");
        auth.addProperty("cnonce", Base64.getEncoder().encodeToString(cnonce));
        auth.addProperty("alg", "SHA256");
        auth.addProperty("signature", Base64.getEncoder().encodeToString(signer.sign()));

        HttpHeaders headers = getJsonHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, getSessionAuthorization(sessionId));
        ResponseEntity<String> authenticated = exchange(getApiUrl("/api/sessions/this"), HttpMethod.PUT, new HttpEntity<>(auth.toString(), headers));
        JsonElement result = JsonParser.parseString(authenticated.getBody()).getAsJsonObject().get("authenticated");
        if (result == null || !result.getAsBoolean()) {
            throw new IOException("handle rest api authentication failed for " + getAuthId() + ": " + authenticated.getBody());
        }
        return sessionId;
    }

    private void closeSession(String sessionId) {
        try {
            HttpHeaders headers = getJsonHeaders();
            headers.set(HttpHeaders.AUTHORIZATION, getSessionAuthorization(sessionId));
            template.exchange(getApiUrl("/api/sessions/this"), HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            log.debug("could not delete handle rest api session: {}", e.getMessage());
        }
    }

    private ResponseEntity<String> exchange(URI uri, HttpMethod method, HttpEntity<String> entity) throws IOException {
        try {
            return template.exchange(uri, method, entity, String.class);
        } catch (HttpStatusCodeException e) {
            throw new IOException("handle rest api " + method + " " + uri.getPath() + " failed with status "
                    + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
    }

    private void checkResponse(String request, String body) throws IOException {
        if (StringUtils.isBlank(body)) {
            return;
        }
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        if (json.has("responseCode") && json.get("responseCode").getAsInt() != RESPONSE_CODE_SUCCESS) {
            throw new IOException("handle rest api " + request + " returned " + body);
        }
    }

    private HandleConfig.Rest getRestConfig() {
        return handleConfig.getRest() == null ? new HandleConfig.Rest() : handleConfig.getRest();
    }

    private boolean isBasicAuth() {
        return getRestConfig().getAuth() == HandleConfig.Auth.basic;
    }

    /**
     * @return handle identity of the caller as {index}:{handle}
     */
    private String getAuthId() {
        return handleServiceImpl.getAdminIndex() + ":" + handleServiceImpl.getAdminHandle();
    }

    private String getSessionAuthorization(String sessionId) {
        return "Handle sessionId=\"" + sessionId + "\"";
    }

    private String getBasicAuthorization() {
        // username is {index}:{handle}, percent signs and colons have to be percent-encoded
        String handle = handleServiceImpl.getAdminHandle().replace("%", "%25").replace(":", "%3A");
        String username = handleServiceImpl.getAdminIndex() + "%3A" + handle;
        String credentials = username + ":" + getRestConfig().getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private HttpHeaders getJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private URI getApiUrl(String path) {
        return URI.create(StringUtils.removeEnd(getRestConfig().getUrl(), "/") + path);
    }

    private URI getHandleUrl(String handleId, String query) {
        // keep the slash between prefix and suffix, the api expects /api/handles/{prefix}/{suffix}
        return getApiUrl("/api/handles/" + UriUtils.encodePath(handleId, StandardCharsets.UTF_8) + query);
    }

    /**
     * handle servers generally use self-signed certificates, so optionally skip certificate and hostname validation
     */
    static RestTemplate createRestTemplate(boolean trustAllCertificates) {
        if (!trustAllCertificates) {
            return new RestTemplate(new SimpleClientHttpRequestFactory());
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            return new RestTemplate(new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                    if (connection instanceof HttpsURLConnection https) {
                        https.setSSLSocketFactory(sslContext.getSocketFactory());
                        https.setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("could not create trust all ssl context", e);
        }
    }
}
