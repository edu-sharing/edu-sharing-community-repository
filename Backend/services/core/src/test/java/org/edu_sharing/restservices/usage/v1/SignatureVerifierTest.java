package org.edu_sharing.restservices.usage.v1;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.login.v1.model.AuthenticationToken;
import org.edu_sharing.restservices.shared.UserProfileAppAuth;
import org.edu_sharing.restservices.usage.v1.model.CreateUsage;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.junit.jupiter.api.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Integration test for SignatureVerifier, calling the live service
 * Steps:
 *  1. Register a new app via the admin API (uploads an XML with a freshly generated RSA public key)
 *  2. Create 5 content nodes (Basic auth admin:admin)
 *  3. Authenticate via appauth to get a ticket
 *  4. Create a usage for each node
 *  5. Fetch metadata for each node using only the usage signature — no ticket — and assert 200 OK
 */
@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignatureVerifierTest {

    static final String BASE_URL = "http://repository.127.0.0.1.nip.io:8100/edu-sharing/rest";
    static final String PREVIEW_URL = "http://repository.127.0.0.1.nip.io:8100/edu-sharing/preview";
    static final String ADMIN_BASIC = "Basic " + java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes());
    // APP_ID must match ([a-zA-Z0-9\-_.]+)
    static final String APP_ID = "test-sig-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    static final String CONTAINER_ID = UUID.randomUUID().toString();
    /**
     * must be <= MAX_SINGLE_USE_NODEIDS
     */
    static final int NODE_COUNT = 25;
    static final int RETRIES_COUNT = 2;

    static Client client;
    static Client noRedirectClient;
    static WebTarget api;
    static PrivateKey privateKey;
    static String ticket;
    static List<String> contentNodeIds = new ArrayList<>();  // created content nodes
    static List<String> usageAlfNodeIds = new ArrayList<>(); // Alfresco node ids of the usages
    static List<String> resourceIds = new ArrayList<>();     // one unique resourceId per node
    static String testUsername = "sigtest-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    static String testUserPassword = "T-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    static String testUserBasic;

    // -------------------------------------------------------------------------
    // Setup: generate keys, register app, create node
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setup() throws Exception {
        Logger jaxlogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        client = ClientBuilder.newClient(new ClientConfig()
                .register(new LoggingFeature(jaxlogger))
                .register(MultiPartFeature.class));
        noRedirectClient = ClientBuilder.newClient(new ClientConfig()
                .property(org.glassfish.jersey.client.ClientProperties.FOLLOW_REDIRECTS, false));
        api = client.target(BASE_URL);

        // Generate 2048-bit RSA key pair
        KeyPair keyPair = new Signing().generateKeys();
        privateKey = keyPair.getPrivate();
        String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.encodeBase64String(keyPair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----";

        registerApp(publicKeyPem);
        createTestUser();
        testUserBasic = "Basic " + java.util.Base64.getEncoder().encodeToString((testUsername + ":" + testUserPassword).getBytes());
        for (int i = 0; i < NODE_COUNT; i++) {
            resourceIds.add(UUID.randomUUID().toString());
            contentNodeIds.add(createContentNode(i));
        }
    }

    private static void createTestUser() {
        Map<String, String> profile = new HashMap<>();
        profile.put("firstName", "SigTest");
        profile.put("lastName", "User");
        profile.put("email", testUsername + "@test.test");

        Response response = api.path("iam/v1/people/-home-/" + testUsername)
                .queryParam("password", testUserPassword)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", ADMIN_BASIC)
                .post(Entity.entity(profile, MediaType.APPLICATION_JSON));

        Assertions.assertEquals(200, response.getStatus(),
                "User creation failed: " + response.readEntity(String.class));
    }

    private static void registerApp(String publicKeyPem) {
        String appXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<!DOCTYPE properties SYSTEM \"http://java.sun.com/dtd/properties.dtd\">\n"
                + "<properties>\n"
                + "    <entry key=\"appid\">" + APP_ID + "</entry>\n"
                + "    <entry key=\"public_key\">" + publicKeyPem + "</entry>\n"
                + "    <entry key=\"type\">LMS</entry>\n"
                + "    <entry key=\"domain\"></entry>\n"
                + "    <entry key=\"host\">*</entry>\n"
                + "    <entry key=\"trustedclient\">true</entry>\n"
                + "</properties>";

        FormDataMultiPart multiPart = new FormDataMultiPart();
        multiPart.field("xml", appXml, MediaType.TEXT_XML_TYPE);

        Response response = api.path("admin/v1/applications/xml")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", ADMIN_BASIC)
                .put(Entity.entity(multiPart, multiPart.getMediaType()));

        Assertions.assertEquals(200, response.getStatus(),
                "App registration failed: " + response.readEntity(String.class));
    }

    private static String createContentNode(int index) {
        Map<String, String[]> props = new HashMap<>();
        props.put("cm:name",
                new String[]{"SignatureVerifierTest-" + index + "-" + System.currentTimeMillis()});
        props.put("ccm:wwwurl",
                new String[]{"http://SignatureVerifierTest-" + index + "-" + System.currentTimeMillis()});

        String url = BASE_URL + "/node/v1/nodes/-home-/-inbox-/children?type=ccm:io";

        Response response = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", testUserBasic)
                .post(Entity.entity(props, MediaType.APPLICATION_JSON));

        response.bufferEntity();
        Assertions.assertEquals(200, response.getStatus(),
                () -> "Node creation failed for index " + index + ": " + response.readEntity(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> nodeEntry = response.readEntity(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> node = (Map<String, Object>) nodeEntry.get("node");
        @SuppressWarnings("unchecked")
        Map<String, Object> ref = (Map<String, Object>) node.get("ref");
        String id = (String) ref.get("id");
        Assertions.assertNotNull(id, "nodeId must not be null for index " + index);
        return id;
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    @AfterAll
    static void cleanup() {
        if (client != null) {
            for (String nodeId : contentNodeIds) {
                api.path("node/v1/nodes/-home-/" + nodeId)
                        .request()
                        .header("Authorization", ADMIN_BASIC)
                        .delete();
            }
            api.path("admin/v1/applications/" + APP_ID)
                    .request()
                    .header("Authorization", ADMIN_BASIC)
                    .delete();
            api.path("iam/v1/people/-home-/" + testUsername)
                    .queryParam("force", true)
                    .request()
                    .header("Authorization", ADMIN_BASIC)
                    .delete();
            noRedirectClient.close();
            client.close();
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: App-based login to get a ticket
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    void step1_authenticateViaAppAuth() throws Exception {
        String timestamp = "" + System.currentTimeMillis();
        // same pattern as existing UsageApiTestSetUsage: username + appId + timestamp
        String signData = "admin" + APP_ID + timestamp;
        Signing signing = new Signing();
        byte[] sig = signing.sign(privateKey, signData, CCConstants.SECURITY_SIGN_ALGORITHM);
        String sigB64 = new String(new Base64().encode(sig));

        UserProfileAppAuth profile = new UserProfileAppAuth();
        profile.setFirstName("Test");
        profile.setLastName("User");
        profile.setEmail("test@test.de");

        Response response = api.path("authentication/v1/appauth/" + testUsername)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Edu-App-Id", APP_ID)
                .header("X-Edu-App-Sig", sigB64)
                .header("X-Edu-App-Signed", signData)
                .header("X-Edu-App-Ts", timestamp)
                .post(Entity.entity(profile, MediaType.APPLICATION_JSON));

        response.bufferEntity();
        Assertions.assertEquals(200, response.getStatus(),
                () -> "App auth failed: " + response.readEntity(String.class));
        AuthenticationToken token = response.readEntity(AuthenticationToken.class);
        ticket = token.getTicket();
        Assertions.assertNotNull(ticket, "ticket must not be null after appauth");
    }

    // -------------------------------------------------------------------------
    // Step 2: Create a usage for each node
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    void step2_createUsages() throws Exception {
        Assertions.assertNotNull(ticket, "ticket is null — step1 must pass first");
        Assertions.assertEquals(NODE_COUNT, contentNodeIds.size(), "setup must have created all nodes first");

        Signing signing = new Signing();
        for (int i = 0; i < NODE_COUNT; i++) {
            final int idx = i;
            String timestamp = "" + System.currentTimeMillis();
            // PHP plugin pattern: appId + ticket + timestamp
            String signData = APP_ID + ticket + timestamp;
            byte[] sig = signing.sign(privateKey, signData, CCConstants.SECURITY_SIGN_ALGORITHM);
            String sigB64 = new String(new Base64().encode(sig));

            CreateUsage usage = new CreateUsage();
            usage.appId = APP_ID;
            usage.courseId = CONTAINER_ID;
            usage.resourceId = resourceIds.get(i);
            usage.nodeId = contentNodeIds.get(i);

            Response response = api.path("usage/v1/usages/repository/-home-")
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Edu-App-Id", APP_ID)
                    .header("X-Edu-App-Sig", sigB64)
                    .header("X-Edu-App-Signed", signData)
                    .header("X-Edu-App-Ts", timestamp)
                    .header("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + " " + ticket)
                    .post(Entity.entity(usage, MediaType.APPLICATION_JSON));

            response.bufferEntity();
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "Usage creation failed for node " + idx + ": " + response.readEntity(String.class));

            Usages.Usage result = response.readEntity(Usages.Usage.class);
            // nodeId in the response is the Alfresco node id of the usage object itself
            String usageAlfId = result.getNodeId();
            Assertions.assertNotNull(usageAlfId, "usageAlfNodeId must not be null for node " + i);
            usageAlfNodeIds.add(usageAlfId);
        }
    }

    // -------------------------------------------------------------------------
    // Step 3: Fetch metadata for each node using only the usage signature (no ticket)
    // -------------------------------------------------------------------------

    @Test
    @Order(3)
    void step3_getMetadataWithUsageSignature() throws Exception {
        Assertions.assertEquals(NODE_COUNT, usageAlfNodeIds.size(), "step2 must have created all usages first");
        for (int j = 0; j < RETRIES_COUNT; j++) {
            Signing signing = new Signing();

            // Establish a server-side session (simulates the LMS plugin holding a persistent HTTP session).
            // The first signed request causes ContextManagementFilter to create a JSESSIONID and store
            // proxy-user auth in it; all subsequent requests reuse that session.
            String warmTs = "" + System.currentTimeMillis();
            String warmSign = APP_ID + usageAlfNodeIds.get(0) + warmTs;
            byte[] warmSigBytes = signing.sign(privateKey, warmSign, CCConstants.SECURITY_SIGN_ALGORITHM);
            String warmSigB64 = new String(new Base64().encode(warmSigBytes));
            Response warmup = api.path("node/v1/nodes/-home-/" + contentNodeIds.get(0) + "/metadata")
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Edu-App-Id", APP_ID)
                    .header("X-Edu-App-Sig", warmSigB64)
                    .header("X-Edu-App-Signed", warmSign)
                    .header("X-Edu-App-Ts", warmTs)
                    .header("X-Edu-Usage-Node-Id", contentNodeIds.get(0))
                    .header("X-Edu-Usage-Course-Id", CONTAINER_ID)
                    .header("X-Edu-Usage-Resource-Id", resourceIds.get(0))
                    .get();
            Assertions.assertEquals(200, warmup.getStatus(), "Session warm-up request failed");
            jakarta.ws.rs.core.NewCookie jsessionidCookie = warmup.getCookies().get("JSESSIONID");
            Assertions.assertNotNull(jsessionidCookie, "Server must return a JSESSIONID cookie on first signed request");
            final String cookieHeader = "JSESSIONID=" + jsessionidCookie.getValue();

            List<java.util.concurrent.Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < NODE_COUNT; i++) {
                final int idx = i;
                tasks.add(() -> {
                    // Thread.sleep((long) (10 * Math.random()));
                    String timestamp = "" + System.currentTimeMillis();
                    String signData = APP_ID + usageAlfNodeIds.get(idx) + timestamp;
                    byte[] sig = signing.sign(privateKey, signData, CCConstants.SECURITY_SIGN_ALGORITHM);
                    String sigB64 = new String(new Base64().encode(sig));

                    // metadata endpoint
                    Response metaResponse = api.path("node/v1/nodes/-home-/" + contentNodeIds.get(idx) + "/metadata")
                            .request(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Cookie", cookieHeader)
                            .header("X-Edu-App-Id", APP_ID)
                            .header("X-Edu-App-Sig", sigB64)
                            .header("X-Edu-App-Signed", signData)
                            .header("X-Edu-App-Ts", timestamp)
                            .header("X-Edu-Usage-Node-Id", contentNodeIds.get(idx))
                            .header("X-Edu-Usage-Course-Id", CONTAINER_ID)
                            .header("X-Edu-Usage-Resource-Id", resourceIds.get(idx))
                            .get();

                    // preview servlet — raw redirect visible because noRedirectClient disables HttpURLConnection follow
                    String previewTimestamp = "" + System.currentTimeMillis();
                    String previewSignData = APP_ID + usageAlfNodeIds.get(idx) + previewTimestamp;
                    byte[] previewSig = signing.sign(privateKey, previewSignData, CCConstants.SECURITY_SIGN_ALGORITHM);
                    String previewSigB64 = new String(new Base64().encode(previewSig));

                    Response previewResponse = noRedirectClient.target(PREVIEW_URL)
                            .queryParam("nodeId", contentNodeIds.get(idx))
                            .request()
                            .header("Cookie", cookieHeader)
                            .header("X-Edu-App-Id", APP_ID)
                            .header("X-Edu-App-Sig", previewSigB64)
                            .header("X-Edu-App-Signed", previewSignData)
                            .header("X-Edu-App-Ts", previewTimestamp)
                            .header("X-Edu-Usage-Node-Id", contentNodeIds.get(idx))
                            .header("X-Edu-Usage-Course-Id", CONTAINER_ID)
                            .header("X-Edu-Usage-Resource-Id", resourceIds.get(idx))
                            .get();

                    Assertions.assertEquals(302, previewResponse.getStatus(),
                            "Preview via usage signature must redirect for node " + idx);
                    String location = previewResponse.getHeaderString("Location");
                    Assertions.assertNotNull(location, "Preview redirect must have a Location header for node " + idx);
                    Assertions.assertTrue(location.contains("link.svg"),
                            "Preview redirect for a URL node must point to link icon, got: " + location);

                    Assertions.assertEquals(200, metaResponse.getStatus(),
                            "Metadata via usage signature must return 200 for node " + idx + ": " + metaResponse.readEntity(String.class));

                    return null;
                });
            }

            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(NODE_COUNT);
            try {
                List<java.util.concurrent.Future<Void>> futures = pool.invokeAll(tasks);
                List<Throwable> failures = new ArrayList<>();
                for (java.util.concurrent.Future<Void> f : futures) {
                    try {
                        f.get();
                    } catch (java.util.concurrent.ExecutionException e) {
                        failures.add(e.getCause());
                    }
                }
                if (!failures.isEmpty()) {
                    AssertionError combined = new AssertionError(failures.size() + " parallel metadata request(s) failed");
                    failures.forEach(combined::addSuppressed);
                    throw combined;
                }
            } finally {
                pool.shutdown();
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 4: Fetch metadata without any auth — must be 401
    // -------------------------------------------------------------------------

    @Test
    @Order(4)
    void step4_getMetadataWithoutAuth_expects401() {
        Assertions.assertFalse(contentNodeIds.isEmpty(), "setup must have created nodes first");

        for (int i = 0; i < NODE_COUNT; i++) {
            Response response = api.path("node/v1/nodes/-home-/" + contentNodeIds.get(i) + "/metadata")
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .get();

            int status = response.getStatus();
            Assertions.assertTrue(status == 401 || status == 403,
                    "Unauthenticated metadata request must return 401 or 403 for node " + i + ", got " + status);
            response.close();
        }
    }
}