package org.edu_sharing.service.statusreport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.typesafe.config.ConfigFactory;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.AppAssertionSigner;
import org.edu_sharing.repository.server.tools.security.HomeRepositorySigner;
import org.edu_sharing.service.statusreport.model.v1.Component;
import org.edu_sharing.service.statusreport.model.v1.StatusReportV1;
import org.edu_sharing.service.version.RepositoryVersionInfo;
import org.edu_sharing.service.version.VersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class StatusReportServiceTest {

    private VersionService versionService;
    private HomeRepositorySigner signer;
    private AppAssertionSigner appAssertionSigner;
    private StatusReportService underTest;
    private MockedStatic<ApplicationInfoList> applicationInfoList;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        versionService = Mockito.mock(VersionService.class);
        signer = Mockito.mock(HomeRepositorySigner.class);
        when(signer.appId()).thenReturn("local");
        when(signer.algorithm()).thenReturn("SHA512withRSA");
        when(signer.sign(Mockito.anyString())).thenAnswer(invocation -> "sig(" + invocation.getArgument(0) + ")");
        appAssertionSigner = Mockito.mock(AppAssertionSigner.class);
        when(appAssertionSigner.sign(Mockito.anyString())).thenAnswer(invocation -> "jws(" + invocation.getArgument(0) + ")");
        underTest = new StatusReportService(versionService, signer, appAssertionSigner);
    }

    @AfterEach
    void tearDown() {
        if (applicationInfoList != null) applicationInfoList.close();
        if (server != null) server.stop(0);
    }

    @Test
    void settingsEmptyWhenNotConfigured() {
        assertTrue(underTest.getSettings(ConfigFactory.empty()).isEmpty());
        assertTrue(underTest.getSettings(ConfigFactory.parseString(
                "repository.statusReport { url: null, clientId: null, clientSecret: null }")).isEmpty());
        assertTrue(underTest.getSettings(ConfigFactory.parseString(
                "repository.statusReport { url: \"https://status.example.org\", clientId: \"repo\", clientSecret: \"\" }")).isEmpty());
    }

    @Test
    void settingsFromConfig() {
        StatusReportService.Settings settings = underTest.getSettings(ConfigFactory.parseString(
                "repository.statusReport { url: \"https://status.example.org\", clientId: \"repo\", clientSecret: \"secret\", timeout: \"3s\" }")).orElseThrow();
        assertEquals("https://status.example.org", settings.url());
        assertEquals("repo", settings.clientId());
        assertEquals("secret", settings.clientSecret());
        assertEquals(Duration.ofSeconds(3), settings.timeout());
        assertFalse(settings.toString().contains("secret"));
    }

    @Test
    void buildReport() throws Exception {
        ApplicationInfo home = app("local", ApplicationInfo.TYPE_REPOSITORY, "repo.example.org", null);
        ApplicationInfo rs1 = app("rs1", ApplicationInfo.TYPE_RENDERSERVICE, "rs1.example.org", null);
        ApplicationInfo rs1Other = app("rs1-other", ApplicationInfo.TYPE_RENDERSERVICE, "rs1-other.example.org", null);
        // not reachable (port 1), so the version request fails fast
        ApplicationInfo rs2 = app("rs2", ApplicationInfo.TYPE_RENDERSERVICE_2, null, "http://127.0.0.1:1/rendering");
        ApplicationInfo lms = app("moodle", ApplicationInfo.TYPE_LMS, "moodle.example.org", null);
        ApplicationInfo lms2 = app("ilias", ApplicationInfo.TYPE_LMS, "ilias.example.org", null);
        mockApplicationInfos(home, rs1, rs1Other, rs2, lms, lms2);
        applicationInfoList.when(ApplicationInfoList::getRenderService).thenReturn(rs1);

        Map<String, RepositoryVersionInfo> plugins = Map.of("plugin-x", versionInfo("plugin-x", "1.2.0", "${docker.tag}", "def456"));
        Map<String, RepositoryVersionInfo> all = new LinkedHashMap<>();
        all.put("repository", versionInfo("repository", "9.1.0", "9.1.0", "abc123"));
        all.putAll(plugins);
        when(versionService.getRepositoryVersionInfo()).thenReturn(all);
        when(versionService.getPluginVersionInfo()).thenReturn(plugins);
        when(versionService.getRenderserviceVersion()).thenReturn("9.0.3");

        StatusReportV1 report = underTest.buildReport();

        assertEquals(StatusReportV1.SCHEMA_VERSION, report.getSchemaVersion());
        assertNotNull(report.getGeneratedAt());
        assertEquals("local", report.getInstallation().getAppId());

        Component repository = find(report.getComponents(), "repository");
        assertEquals(Component.TYPE_REPOSITORY, repository.getType());
        assertEquals("9.1.0", repository.getVersion());
        assertEquals("9.1.0", repository.getImageTag());
        assertEquals("abc123", repository.getCommit());

        Component plugin = find(report.getComponents(), "plugin-x");
        assertEquals(Component.TYPE_PLUGIN, plugin.getType());
        // unresolved maven placeholder
        assertNull(plugin.getImageTag());

        assertNull(findOrNull(report.getComponents(), "local"), "home repository must not be reported as connector");
        Component rs1Component = find(report.getComponents(), "rs1");
        assertEquals(Component.TYPE_CONNECTOR, rs1Component.getType());
        assertEquals(ApplicationInfo.TYPE_RENDERSERVICE, rs1Component.getSubtype());
        assertEquals("9.0.3", rs1Component.getVersion());
        // a system that can report a version stays a component when the request fails, so the broken connection is visible
        Component rs2Component = find(report.getComponents(), "rs2");
        assertEquals(ApplicationInfo.TYPE_RENDERSERVICE_2, rs2Component.getSubtype());
        assertNull(rs2Component.getVersion());

        // systems without version (lms, further rendering services 1, ...) are only counted per type
        assertNull(findOrNull(report.getComponents(), "rs1-other"));
        assertNull(findOrNull(report.getComponents(), "moodle"));
        assertNull(findOrNull(report.getComponents(), "ilias"));
        assertEquals(Map.of(ApplicationInfo.TYPE_LMS, 2, ApplicationInfo.TYPE_RENDERSERVICE, 1), report.getConnectedSystems());

        String json = new ObjectMapper().writeValueAsString(report);
        assertFalse(json.contains("secret"), "no config values must be sent");
        assertFalse(json.contains("clientId"), "the installation is identified by the basic auth only");
        assertFalse(json.contains("\"host\""), "no hosts / ips of the connected systems are sent");
        assertFalse(json.contains("moodle.example.org"));
        assertTrue(json.contains("\"connectedSystems\":{\"LMS\":2,\"SERVICE\":1}"), json);
    }

    @Test
    void servicesConnectorVersionIsFetchedSigned() throws Exception {
        Map<String, String> received = new ConcurrentHashMap<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/connector/version.php", exchange -> {
            for (String header : List.of("X-Edu-App-Id", "X-Edu-App-Ts", "X-Edu-App-Signed", "X-Edu-App-Sig", "X-Edu-App-SignedAlg")) {
                received.put(header, exchange.getRequestHeaders().getFirst(header));
            }
            byte[] body = "{\"version\":\"11.0.3\",\"build_date\":\"2026-11-01T10:00:00Z\",\"commit\":\"c0ffee\",\"build_branch\":\"maven/fixes/11.0\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ApplicationInfo home = app("local", ApplicationInfo.TYPE_REPOSITORY, "repo.example.org", null);
        ApplicationInfo connector = app("connector", ApplicationInfo.TYPE_CONNECTOR, "127.0.0.1",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/connector");
        mockApplicationInfos(home, connector);

        Component component = find(underTest.getConnectorComponents(home), "connector");

        assertEquals("11.0.3", component.getVersion());
        assertEquals("c0ffee", component.getCommit());
        assertNull(component.getImageTag(), "the services-connector reports no image tag");
        assertEquals("local", received.get("X-Edu-App-Id"));
        assertEquals("local" + received.get("X-Edu-App-Ts"), received.get("X-Edu-App-Signed"));
        assertEquals("sig(" + received.get("X-Edu-App-Signed") + ")", received.get("X-Edu-App-Sig"));
        assertEquals("SHA512withRSA", received.get("X-Edu-App-SignedAlg"));
    }

    @Test
    void servicesConnectorVersionNullWhenRejected() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/connector/version.php", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        ApplicationInfo home = app("local", ApplicationInfo.TYPE_REPOSITORY, "repo.example.org", null);
        ApplicationInfo connector = app("connector", ApplicationInfo.TYPE_CONNECTOR, "127.0.0.1",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/connector/");
        mockApplicationInfos(home, connector);

        Component component = find(underTest.getConnectorComponents(home), "connector");

        assertNull(component.getVersion());
        assertNull(component.getCommit());
    }

    @Test
    void renderingService2VersionIsFetchedWithTheAppAssertion() throws Exception {
        Map<String, String> received = new ConcurrentHashMap<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/rendering/public/version", exchange -> {
            for (String header : List.of("X-Edu-App-Assertion", "Authorization", "X-Edu-App-Sig")) {
                received.put(header, String.valueOf(exchange.getRequestHeaders().getFirst(header)));
            }
            byte[] body = "{\"version\":\"11.0.4\",\"commit\":\"ad3ffe49\",\"imageTag\":\"11.0.4\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        ApplicationInfo home = app("local", ApplicationInfo.TYPE_REPOSITORY, "repo.example.org", null);
        ApplicationInfo rs2 = app("rendering2", ApplicationInfo.TYPE_RENDERSERVICE_2, "rendering2-service",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/rendering");
        mockApplicationInfos(home, rs2);

        Component component = find(underTest.getConnectorComponents(home), "rendering2");

        assertEquals("11.0.4", component.getVersion());
        assertEquals("ad3ffe49", component.getCommit());
        assertEquals("11.0.4", component.getImageTag());
        assertEquals("jws(rendering-service)", received.get("X-Edu-App-Assertion"));
        assertEquals("null", received.get("Authorization"), "the assertion is no Authorization: the service would read it as the token of a user");
        assertEquals("null", received.get("X-Edu-App-Sig"), "the rendering service 2 gets no app signature headers any more");
    }

    @Test
    void versionUrl() {
        assertEquals("https://example.org/connector/version.php", StatusReportService.getVersionUrl("https://example.org/connector", "version.php"));
        assertEquals("https://example.org/connector/version.php", StatusReportService.getVersionUrl("https://example.org/connector/", "version.php"));
        assertEquals("https://example.org/connector/version.php", StatusReportService.getVersionUrl("https://example.org/connector/index.php?x=1", "version.php"));
        assertEquals("https://example.org/rendering/public/version", StatusReportService.getVersionUrl("https://example.org/rendering", "public/version"));
        assertNull(StatusReportService.getVersionUrl(" ", "version.php"));
    }

    @Test
    void sendUsesBasicAuthAndSchemaHeader() throws IOException {
        Map<String, String> received = new ConcurrentHashMap<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report", exchange -> {
            received.put("method", exchange.getRequestMethod());
            received.put("auth", exchange.getRequestHeaders().getFirst("Authorization"));
            received.put("schema", exchange.getRequestHeaders().getFirst(StatusReportService.HEADER_SCHEMA_VERSION));
            received.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
            received.put("body", new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.start();

        StatusReportService.Settings settings = new StatusReportService.Settings(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/report", "repo-1", "s3cret", Duration.ofSeconds(5));
        assertTrue(underTest.send(settings, "{\"schemaVersion\":\"1\"}"));

        assertEquals("POST", received.get("method"));
        assertEquals("Basic " + Base64.getEncoder().encodeToString("repo-1:s3cret".getBytes(StandardCharsets.UTF_8)), received.get("auth"));
        assertEquals(StatusReportV1.SCHEMA_VERSION, received.get("schema"));
        assertTrue(received.get("contentType").startsWith("application/json"));
        JsonNode body = new ObjectMapper().readTree(received.get("body"));
        assertEquals("1", body.get("schemaVersion").asText());
    }

    @Test
    void sendFailureIsNoException() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/report", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });
        server.start();
        StatusReportService.Settings settings = new StatusReportService.Settings(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/report", "repo-1", "wrong", Duration.ofSeconds(5));
        assertFalse(underTest.send(settings, "{}"));
    }

    @Test
    void filtered() {
        assertNull(StatusReportService.filtered(null));
        assertNull(StatusReportService.filtered(" "));
        assertNull(StatusReportService.filtered("${docker.tag}"));
        assertEquals("9.1.0", StatusReportService.filtered("9.1.0"));
    }

    private void mockApplicationInfos(ApplicationInfo home, ApplicationInfo... others) {
        Map<String, ApplicationInfo> infos = new LinkedHashMap<>();
        infos.put(home.getAppId(), home);
        for (ApplicationInfo other : others) {
            infos.put(other.getAppId(), other);
        }
        applicationInfoList = Mockito.mockStatic(ApplicationInfoList.class);
        applicationInfoList.when(ApplicationInfoList::getHomeRepository).thenReturn(home);
        applicationInfoList.when(ApplicationInfoList::getApplicationInfos).thenReturn(infos);
    }

    private static ApplicationInfo app(String appId, String type, String host, String contentUrl) {
        ApplicationInfo app = Mockito.mock(ApplicationInfo.class);
        when(app.getAppId()).thenReturn(appId);
        when(app.getType()).thenReturn(type);
        when(app.getHost()).thenReturn(host);
        when(app.getContentUrl()).thenReturn(contentUrl);
        return app;
    }

    private static RepositoryVersionInfo versionInfo(String name, String version, String imageTag, String commit) {
        RepositoryVersionInfo info = new RepositoryVersionInfo();
        info.repository = name;
        info.version = new RepositoryVersionInfo.Version();
        info.version.full = version;
        info.docker = new RepositoryVersionInfo.VersionDocker();
        info.docker.tag = imageTag;
        info.git = new RepositoryVersionInfo.VersionGit();
        info.git.commit = new RepositoryVersionInfo.VersionGit.VersionGitCommit();
        info.git.commit.id = commit;
        return info;
    }

    private static Component find(List<Component> components, String name) {
        Component component = findOrNull(components, name);
        assertNotNull(component, "component " + name + " missing");
        return component;
    }

    private static Component findOrNull(List<Component> components, String name) {
        return components.stream().filter(c -> name.equals(c.getName())).findFirst().orElse(null);
    }
}
