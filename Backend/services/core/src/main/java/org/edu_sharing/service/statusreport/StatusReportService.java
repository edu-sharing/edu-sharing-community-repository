package org.edu_sharing.service.statusreport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigRenderOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.HomeRepositorySigner;
import org.edu_sharing.service.statusreport.model.v1.Component;
import org.edu_sharing.service.statusreport.model.v1.Installation;
import org.edu_sharing.service.statusreport.model.v1.StatusReportV1;
import org.edu_sharing.service.version.RepositoryVersionInfo;
import org.edu_sharing.service.version.VersionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Builds and sends the status report (versions of the repository, its plugins and the connected systems) to a central
 * server, used there to track the cve state of all installations. Library details are resolved by the central server
 * via image tag / commit, so the payload is kept small.
 *
 * Config: {@code repository.statusReport}, triggered by the {@link org.edu_sharing.repository.server.jobs.quartz.StatusReportJob}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatusReportService {
    public static final String CONFIG_PATH = "repository.statusReport";
    public static final String HEADER_SCHEMA_VERSION = "X-ES-Schema-Version";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration CONNECTOR_VERSION_TIMEOUT = Duration.ofSeconds(5);

    private final VersionService versionService;
    private final HomeRepositorySigner signer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @return the settings, or empty if the status report is not (completely) configured
     */
    public Optional<Settings> getSettings(Config config) {
        if (!config.hasPath(CONFIG_PATH)) {
            return Optional.empty();
        }
        Config reportConfig = config.getConfig(CONFIG_PATH);
        String url = getString(reportConfig, "url");
        String clientId = getString(reportConfig, "clientId");
        String clientSecret = getString(reportConfig, "clientSecret");
        if (StringUtils.isAnyBlank(url, clientId, clientSecret)) {
            return Optional.empty();
        }
        Duration timeout = reportConfig.hasPath("timeout") ? reportConfig.getDuration("timeout") : DEFAULT_TIMEOUT;
        return Optional.of(new Settings(url, clientId, clientSecret, timeout));
    }

    /**
     * builds the report and sends it if the status report is configured
     *
     * @param dryRun only build and return the report, do not send it
     * @return the json payload, or null if the status report is not configured (and it is no dry run)
     */
    public String run(boolean dryRun) throws JsonProcessingException {
        Config config = LightbendConfigLoader.get();
        Optional<Settings> settings = getSettings(config);
        if (settings.isEmpty() && !dryRun) {
            log.info("status report is not configured ({}.url, clientId, clientSecret), nothing to send", CONFIG_PATH);
            return null;
        }
        String payload = objectMapper.writeValueAsString(buildReport(config));
        if (dryRun) {
            log.info("status report (dry run, not sent): {}", payload);
            return payload;
        }
        send(settings.get(), payload);
        return payload;
    }

    public StatusReportV1 buildReport(Config config) {
        StatusReportV1 report = new StatusReportV1();
        report.setGeneratedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        ApplicationInfo home = ApplicationInfoList.getHomeRepository();
        if (home != null) {
            report.setInstallation(new Installation(home.getAppId()));
        }
        report.setConfigHash(getConfigHash(config));
        report.getComponents().addAll(getVersionComponents());
        report.getComponents().addAll(getConnectorComponents(home));
        return report;
    }

    /**
     * sends the payload, failures are only logged, the job will try again on its next run
     *
     * @return true if the central server accepted the report
     */
    public boolean send(Settings settings, String payload) {
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout((int) settings.timeout().toMillis());
            requestFactory.setReadTimeout((int) settings.timeout().toMillis());
            ResponseEntity<Void> response = RestClient.builder()
                    .requestFactory(requestFactory)
                    .build()
                    .post()
                    .uri(settings.url())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(settings.clientId(), settings.clientSecret(), StandardCharsets.UTF_8))
                    .header(HEADER_SCHEMA_VERSION, StatusReportV1.SCHEMA_VERSION)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("status report sent to {}, status {}", settings.url(), response.getStatusCode().value());
            return true;
        } catch (Exception e) {
            log.warn("could not send status report to {}: {}", settings.url(), e.getMessage());
            log.debug(e.getMessage(), e);
            return false;
        }
    }

    List<Component> getVersionComponents() {
        List<Component> result = new ArrayList<>();
        try {
            Set<String> plugins = versionService.getPluginVersionInfo().keySet();
            for (Map.Entry<String, RepositoryVersionInfo> entry : versionService.getRepositoryVersionInfo().entrySet()) {
                RepositoryVersionInfo info = entry.getValue();
                result.add(Component.builder()
                        .type(plugins.contains(entry.getKey()) ? Component.TYPE_PLUGIN : Component.TYPE_REPOSITORY)
                        .name(entry.getKey())
                        .version(info.version != null ? filtered(info.version.full) : null)
                        .imageTag(info.docker != null ? filtered(info.docker.tag) : null)
                        .commit(info.git != null && info.git.commit != null ? filtered(info.git.commit.id) : null)
                        .build());
            }
        } catch (Exception e) {
            log.warn("could not read version infos: {}", e.getMessage(), e);
        }
        return result;
    }

    List<Component> getConnectorComponents(ApplicationInfo home) {
        List<Component> result = new ArrayList<>();
        // the version can only be fetched from the (single) rendering service the repository uses
        ApplicationInfo renderService = ApplicationInfoList.getRenderService();
        for (ApplicationInfo app : ApplicationInfoList.getApplicationInfos().values()) {
            if (app == null || (home != null && StringUtils.equals(app.getAppId(), home.getAppId()))) {
                continue;
            }
            Component.ComponentBuilder component = Component.builder()
                    .type(Component.TYPE_CONNECTOR)
                    .subtype(app.getType())
                    .name(app.getAppId())
                    .host(getHost(app));
            addConnectorVersion(component, app, renderService);
            result.add(component.build());
        }
        result.sort(Comparator.comparing(Component::getName, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    /**
     * only systems which can be queried for their version are filled, all other ones stay null
     *
     * {@link VersionService#getRenderserviceVersion()} always queries the rendering service of
     * {@link ApplicationInfoList#getRenderService()}, so only that app gets the version, further rendering services
     * (type {@link ApplicationInfo#TYPE_RENDERSERVICE}) are reported without one
     */
    private void addConnectorVersion(Component.ComponentBuilder component, ApplicationInfo app, ApplicationInfo renderService) {
        if (renderService != null && StringUtils.equals(app.getAppId(), renderService.getAppId())) {
            try {
                component.version(versionService.getRenderserviceVersion());
            } catch (Exception e) {
                log.warn("could not fetch version of rendering service {}: {}", app.getAppId(), e.getMessage());
            }
        } else if (ApplicationInfo.TYPE_CONNECTOR.equals(app.getType())) {
            addSignedVersion(component, app, getVersionUrl(app.getContentUrl(), "version.php"));
        } else if (ApplicationInfo.TYPE_RENDERSERVICE_2.equals(app.getType())) {
            addSignedVersion(component, app, getVersionUrl(app.getContentUrl(), "public/version"));
        }
    }

    /**
     * fetches the build info of a connected system with a request signed by this repository (app signature headers),
     * the system verifies it with the public key of this repository (services-connector: from the repository metadata,
     * rendering service 2: from its registration)
     *
     * Every system answers with the same fields {@code {"version": "...", "commit": "...", "imageTag": "..."}},
     * fields it does not know are missing or null (e.g. the services-connector has no image tag).
     */
    private void addSignedVersion(Component.ComponentBuilder component, ApplicationInfo app, String url) {
        if (url == null) {
            return;
        }
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout((int) CONNECTOR_VERSION_TIMEOUT.toMillis());
            requestFactory.setReadTimeout((int) CONNECTOR_VERSION_TIMEOUT.toMillis());
            String body = RestClient.builder()
                    .requestFactory(requestFactory)
                    .build()
                    .get()
                    .uri(url)
                    .headers(this::addSignatureHeaders)
                    .retrieve()
                    .body(String.class);
            JsonNode info = objectMapper.readTree(body);
            component.version(filtered(info.path("version").asText(null)));
            component.commit(filtered(info.path("commit").asText(null)));
            component.imageTag(filtered(info.path("imageTag").asText(null)));
        } catch (Exception e) {
            log.warn("could not fetch version of {} {} from {}: {}", app.getType(), app.getAppId(), url, e.getMessage());
        }
    }

    /**
     * the app signature headers, same scheme the connector uses for its calls to the repository
     */
    void addSignatureHeaders(HttpHeaders headers) {
        String appId = signer.appId();
        String ts = String.valueOf(System.currentTimeMillis());
        String signed = appId + ts;
        headers.set("X-Edu-App-Id", appId);
        headers.set("X-Edu-App-Ts", ts);
        headers.set("X-Edu-App-Signed", signed);
        headers.set("X-Edu-App-Sig", signer.sign(signed));
        headers.set("X-Edu-App-SignedAlg", signer.algorithm());
    }

    /**
     * @param contentUrl base url of the connected system, e.g. "https://example.org/edu-sharing-connector/"
     *                   or "https://example.org/rendering"
     * @param path       path of the version endpoint relative to the base url
     */
    static String getVersionUrl(String contentUrl, String path) {
        if (StringUtils.isBlank(contentUrl)) {
            return null;
        }
        String url = StringUtils.substringBefore(contentUrl.trim(), "?");
        url = StringUtils.removeEnd(url, "index.php");
        return StringUtils.appendIfMissing(url, "/") + path;
    }

    static String getHost(ApplicationInfo app) {
        if (StringUtils.isNotBlank(app.getHost())) {
            return app.getHost();
        }
        for (String url : new String[]{app.getContentUrl(), app.getLtitoolUrl()}) {
            if (StringUtils.isNotBlank(url)) {
                try {
                    String host = URI.create(url.trim()).getHost();
                    if (host != null) {
                        return host;
                    }
                } catch (IllegalArgumentException ignored) {
                    // not a valid url, try the next one
                }
            }
        }
        return null;
    }

    static String getConfigHash(Config config) {
        return "sha256:" + DigestUtils.sha256Hex(config.root().render(ConfigRenderOptions.concise()));
    }

    /**
     * maven leaves unknown properties unresolved (e.g. "${docker.tag}" if the build did not set it)
     */
    static String filtered(String value) {
        if (StringUtils.isBlank(value) || value.contains("${")) {
            return null;
        }
        return value;
    }

    private static String getString(Config config, String path) {
        return config.hasPath(path) ? config.getString(path) : null;
    }

    public record Settings(String url, String clientId, String clientSecret, Duration timeout) {
        @Override
        public String toString() {
            // never log the secret
            return "Settings[url=" + url + ", clientId=" + clientId + ", timeout=" + timeout + "]";
        }
    }
}
