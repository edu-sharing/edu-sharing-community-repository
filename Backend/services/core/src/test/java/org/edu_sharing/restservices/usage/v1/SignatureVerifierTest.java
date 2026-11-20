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
    static final int NODE_COUNT = 5;
    static final int RETRIES_COUNT = 2;
    // remote node (pixabay) used to test prepareUsage
    static final String REMOTE_REPOSITORY = "pixabay";
    static final String REMOTE_NODE_ID = "3518251";

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
    // second user: accesses (reads) the usages created by the first user
    static String testUsername2 = "sigtest2-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    static String testUserPassword2 = "T-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

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
        createTestUser(testUsername, testUserPassword);
        createTestUser(testUsername2, testUserPassword2);
        testUserBasic = "Basic " + java.util.Base64.getEncoder().encodeToString((testUsername + ":" + testUserPassword).getBytes());
        for (int i = 0; i < NODE_COUNT; i++) {
            resourceIds.add(UUID.randomUUID().toString());
            contentNodeIds.add(createContentNode(i));
        }
    }

    private static void createTestUser(String username, String password) {
        Map<String, String> profile = new HashMap<>();
        profile.put("firstName", "SigTest");
        profile.put("lastName", "User");
        profile.put("email", username + "@test.test");

        Response response = api.path("iam/v1/people/-home-/" + username)
                .queryParam("password", password)
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
            for (String username : new String[]{testUsername, testUsername2}) {
                api.path("iam/v1/people/-home-/" + username)
                        .queryParam("force", true)
                        .request()
                        .header("Authorization", ADMIN_BASIC)
                        .delete();
            }
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
        ticket = authenticate(testUsername);
        Assertions.assertNotNull(ticket, "ticket must not be null after appauth");
    }

    /**
     * Authenticate a user via appauth (trusted app signature) and return the resulting ticket.
     */
    private static String authenticate(String username) throws Exception {
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

        Response response = api.path("authentication/v1/appauth/" + username)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Edu-App-Id", APP_ID)
                .header("X-Edu-App-Sig", sigB64)
                .header("X-Edu-App-Signed", signData)
                .header("X-Edu-App-Ts", timestamp)
                .post(Entity.entity(profile, MediaType.APPLICATION_JSON));

        response.bufferEntity();
        Assertions.assertEquals(200, response.getStatus(),
                () -> "App auth failed for " + username + ": " + response.readEntity(String.class));
        AuthenticationToken token = response.readEntity(AuthenticationToken.class);
        return token.getTicket();
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

    // -------------------------------------------------------------------------
    // Step 5: The first user (owner of the usages) fetches the usages of each
    //         node via GET /usage/v1/usages/node/{nodeId} and must see the usage
    //         created in step2 (no 403).
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    void step5_getUsagesByNode() {
        Assertions.assertNotNull(ticket, "ticket is null — step1 must pass first");
        Assertions.assertEquals(NODE_COUNT, contentNodeIds.size(), "setup must have created all nodes first");
        Assertions.assertEquals(NODE_COUNT, usageAlfNodeIds.size(), "step2 must have created all usages first");

        for (int i = 0; i < NODE_COUNT; i++) {
            final int idx = i;
            Response response = api.path("usage/v1/usages/node/" + contentNodeIds.get(i))
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + " " + ticket)
                    .get();

            response.bufferEntity();
            Assertions.assertEquals(200, response.getStatus(),
                    () -> "Get usages by node failed for node " + idx + ": " + response.readEntity(String.class));

            Usages usages = response.readEntity(Usages.class);
            Assertions.assertNotNull(usages.getUsages(), "usages list must not be null for node " + i);

            Usages.Usage usage = usages.getUsages().stream()
                    .filter(u -> resourceIds.get(idx).equals(u.getResourceId()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(usage,
                    "previously created usage (resourceId=" + resourceIds.get(idx) + ") must be returned for node " + i);
            Assertions.assertEquals(APP_ID, usage.getAppId(), "usage appId mismatch for node " + i);
            Assertions.assertEquals(CONTAINER_ID, usage.getCourseId(), "usage courseId mismatch for node " + i);
            Assertions.assertEquals(usageAlfNodeIds.get(i), usage.getNodeId(),
                    "usage (alfresco) nodeId mismatch for node " + i);
        }
    }

    // -------------------------------------------------------------------------
    // Step 6: prepareUsage for a remote node (pixabay) with the first user, who
    //         also creates the usage. A second (different) user then reads the
    //         usage list, followed by the signature-based access check.
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    void step6_prepareUsageForRemoteNode() throws Exception {
        Assertions.assertNotNull(ticket, "ticket is null — step1 must pass first");
        Signing signing = new Signing();

        // 1. prepareUsage: create the local remote object for the pixabay node
        Response prepareResponse = api.path("node/v1/nodes/" + REMOTE_REPOSITORY + "/" + REMOTE_NODE_ID + "/prepareUsage")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + " " + ticket)
                .post(Entity.entity("", MediaType.APPLICATION_JSON));

        prepareResponse.bufferEntity();
        Assertions.assertEquals(200, prepareResponse.getStatus(),
                () -> "prepareUsage failed: " + prepareResponse.readEntity(String.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> remoteEntry = prepareResponse.readEntity(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> localNode = (Map<String, Object>) remoteEntry.get("remote");
        Assertions.assertNotNull(localNode, "prepareUsage must return a local node");
        @SuppressWarnings("unchecked")
        Map<String, Object> localRef = (Map<String, Object>) localNode.get("ref");
        Assertions.assertNotNull(localRef, "local node must have a ref");
        String localNodeId = (String) localRef.get("id");
        Assertions.assertNotNull(localNodeId, "local node id must not be null");

        // 2. Create a usage for the prepared (local) node
        String resourceId = UUID.randomUUID().toString();
        String createTimestamp = "" + System.currentTimeMillis();
        String createSignData = APP_ID + ticket + createTimestamp;
        byte[] createSig = signing.sign(privateKey, createSignData, CCConstants.SECURITY_SIGN_ALGORITHM);
        String createSigB64 = new String(new Base64().encode(createSig));

        CreateUsage usage = new CreateUsage();
        usage.appId = APP_ID;
        usage.courseId = CONTAINER_ID;
        usage.resourceId = resourceId;
        usage.nodeId = localNodeId;

        Response createResponse = api.path("usage/v1/usages/repository/-home-")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Edu-App-Id", APP_ID)
                .header("X-Edu-App-Sig", createSigB64)
                .header("X-Edu-App-Signed", createSignData)
                .header("X-Edu-App-Ts", createTimestamp)
                .header("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + " " + ticket)
                .post(Entity.entity(usage, MediaType.APPLICATION_JSON));

        createResponse.bufferEntity();
        Assertions.assertEquals(200, createResponse.getStatus(),
                () -> "Usage creation failed for remote node: " + createResponse.readEntity(String.class));
        String usageAlfNodeId = createResponse.readEntity(Usages.Usage.class).getNodeId();
        Assertions.assertNotNull(usageAlfNodeId, "usageAlfNodeId must not be null for remote node");

        // 3. getUsage: a *different* user (testUsername2) fetches the usages of the local node.
        //    The usage was created by the first user, yet the second user must still see it.
        String ticket2 = authenticate(testUsername2);
        Assertions.assertNotNull(ticket2, "ticket for second user must not be null");

        Response usagesResponse = api.path("usage/v1/usages/node/" + localNodeId)
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + " " + ticket2)
                .get();

        usagesResponse.bufferEntity();
        Assertions.assertEquals(200, usagesResponse.getStatus(),
                () -> "Get usages by node failed for remote node: " + usagesResponse.readEntity(String.class));
        Usages usages = usagesResponse.readEntity(Usages.class);
        Assertions.assertNotNull(usages.getUsages(), "usages list must not be null for remote node");
        Usages.Usage created = usages.getUsages().stream()
                .filter(u -> resourceId.equals(u.getResourceId()))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(created,
                "previously created usage (resourceId=" + resourceId + ") must be returned for remote node");
        Assertions.assertEquals(APP_ID, created.getAppId(), "usage appId mismatch for remote node");
        Assertions.assertEquals(CONTAINER_ID, created.getCourseId(), "usage courseId mismatch for remote node");
        Assertions.assertEquals(usageAlfNodeId, created.getNodeId(), "usage (alfresco) nodeId mismatch for remote node");

        // 4. access: fetch metadata of the local node using only the usage signature (no ticket)
        String accessTimestamp = "" + System.currentTimeMillis();
        String accessSignData = APP_ID + usageAlfNodeId + accessTimestamp;
        byte[] accessSig = signing.sign(privateKey, accessSignData, CCConstants.SECURITY_SIGN_ALGORITHM);
        String accessSigB64 = new String(new Base64().encode(accessSig));

        Response metaResponse = api.path("node/v1/nodes/-home-/" + localNodeId + "/metadata")
                .request(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Edu-App-Id", APP_ID)
                .header("X-Edu-App-Sig", accessSigB64)
                .header("X-Edu-App-Signed", accessSignData)
                .header("X-Edu-App-Ts", accessTimestamp)
                .header("X-Edu-Usage-Node-Id", localNodeId)
                .header("X-Edu-Usage-Course-Id", CONTAINER_ID)
                .header("X-Edu-Usage-Resource-Id", resourceId)
                .get();

        metaResponse.bufferEntity();
        Assertions.assertEquals(200, metaResponse.getStatus(),
                () -> "Metadata via usage signature must return 200 for remote node: " + metaResponse.readEntity(String.class));

        // cleanup the locally created remote object
        api.path("node/v1/nodes/-home-/" + localNodeId)
                .request()
                .header("Authorization", ADMIN_BASIC)
                .delete();
    }
}