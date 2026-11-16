package org.edu_sharing.service.bapi;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.alfresco.service.guest.GuestService;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.security.RequestUserSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.edu_sharing.repository.server.tools.security.SigningTestVector.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** What the proxy to the B-API really sends: the key of the repository, the user it signed itself and nothing a client says about either. */
class BApiProxyServiceSigningTest {

    private HttpServer server;
    private MockedStatic<AuthenticationUtil> authentication;

    /** The Alfresco context is not running in a unit test: the authenticated user is what the static call answers. */
    private void loginAs(String user) {
        authentication = Mockito.mockStatic(AuthenticationUtil.class);
        authentication.when(AuthenticationUtil::getFullyAuthenticatedUser).thenReturn(user);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
        if (authentication != null) authentication.close();
    }

    private BApiProxyService service(String uri, ApplicationInfo home) {
        BApiProxyConfig config = new BApiProxyConfig();
        config.setUri(uri);
        config.setAuthUserApiKey(API_KEY);
        config.setGuestUserApiKey("guest-key");
        config.setCallTimeout("PT10S");
        return new BApiProxyService(config, Mockito.mock(GuestService.class), new RequestUserSigner(signerOf(home, "SHA512withRSA")));
    }

    @Test
    void theProxyDropsEveryXEduHeaderAndTheKeyOfTheClientAndSignsTheUserItself() throws Exception {
        Map<String, List<String>> seen = new ConcurrentHashMap<>();
        String[] seenTarget = new String[1];
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getRequestHeaders().forEach((k, v) -> seen.put(k.toLowerCase(), v));
            seenTarget[0] = exchange.getRequestURI().getRawPath() + (exchange.getRequestURI().getRawQuery() != null ? "?" + exchange.getRequestURI().getRawQuery() : "");
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        ApplicationInfo home = Mockito.mock(ApplicationInfo.class);
        when(home.getAppId()).thenReturn(APP_ID);
        when(home.getPrivateKey()).thenReturn(privateKeyPem());
        BApiProxyService service = service("http://127.0.0.1:" + server.getAddress().getPort(), home);

        loginAs("Jürgen");
        MultivaluedMap<String, String> sent = new MultivaluedHashMap<>();
        sent.add("X-Edu-User-Id", "admin");
        sent.add("x-edu-app-sig", "forged");
        sent.add("X-EDU-APP-ID", "forged");
        sent.add("X-Edu-Anything", "x");
        sent.add("X-API-KEY", "stolen-key");
        sent.add("Authorization", "EDU-TICKET secret");
        sent.add("X-Other", "kept");
        HttpHeaders headers = Mockito.mock(HttpHeaders.class);
        when(headers.getRequestHeaders()).thenReturn(sent);

        Response response = service.forwardRequest("api/v1/chat?a=b%20c", "{}", headers, HttpMethod.POST);

        assertEquals(200, response.getStatus());
        assertEquals(List.of(API_KEY), seen.get("x-api-key"), "the key of the repository, not the one of the client");
        assertEquals(List.of("kept"), seen.get("x-other"));
        assertNull(seen.get("authorization"));
        assertNull(seen.get("x-edu-anything"));
        // one header carries the user: the assertion, signed by the repository for exactly this request
        for (String legacy : List.of("x-edu-user-id", "x-edu-app-id", "x-edu-app-ts", "x-edu-app-nonce", "x-edu-app-sig", "x-edu-app-signedalg")) {
            assertNull(seen.get(legacy), legacy + " is not part of the protocol any more");
        }
        List<String> assertions = seen.get("x-edu-user-assertion");
        assertEquals(1, assertions.size());
        String[] parts = assertions.get(0).split("\\.");
        java.security.Signature signature = java.security.Signature.getInstance("SHA512withRSA");
        signature.initVerify(publicKey());
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertTrue(signature.verify(Base64.getUrlDecoder().decode(parts[2])), "signed as it went over the wire: " + seenTarget[0]);
        ObjectMapper json = new ObjectMapper();
        JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
        assertEquals("Jürgen", claims.get("sub").asText(), "the authenticated user, not what the client sent");
        assertEquals(APP_ID, claims.get("iss").asText());
        assertEquals("bildungs-api", claims.get("aud").asText());
        assertEquals("POST", claims.get("htm").asText());
        assertEquals("/api/v1/chat?a=b%20c", seenTarget[0]);
        assertEquals(seenTarget[0], claims.get("hpq").asText());
        assertEquals(ATH, claims.get("ath").asText());
        assertTrue(Math.abs(System.currentTimeMillis() / 1000 - claims.get("iat").asLong()) < 10);
        assertEquals("bapi-user+jwt", json.readTree(Base64.getUrlDecoder().decode(parts[0])).get("typ").asText(), "the B-API checks for this type");
    }

    @Test
    void withoutAKeyOfItsOwnTheRepositoryAnswersUnavailableAndSendsNothing() {
        ApplicationInfo home = Mockito.mock(ApplicationInfo.class);
        when(home.getAppId()).thenReturn(APP_ID);
        when(home.getPrivateKey()).thenReturn(null);
        BApiProxyService service = service("http://127.0.0.1:1", home);
        loginAs("alice");
        assertEquals(503, service.forwardRequest("x", null, null, HttpMethod.GET).getStatus());
    }
}
