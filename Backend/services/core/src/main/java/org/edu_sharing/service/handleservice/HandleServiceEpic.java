package org.edu_sharing.service.handleservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.spring.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * handle service implementation using the ePIC API (European Persistent Identifier Consortium), e.g. https://pid.gwdg.de/handles
 * <p>
 * Authentication is done via http basic auth with the credentials of the ePIC account.
 * The admin values (HS_ADMIN) of the handles are managed by ePIC, so only URL and EMAIL are written.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandleServiceEpic implements HandleService {

    private final HandleConfig handleConfig;
    private final HandleServiceImpl handleServiceImpl;

    private RestTemplate template = HandleServiceRest.createRestTemplate(false);

    @PostConstruct
    @EventListener(RefreshScopeRefreshedEvent.class)
    public void loadConfig() {
        template = HandleServiceRest.createRestTemplate(Boolean.TRUE.equals(getEpicConfig().getTrustAllCertificates()));
        try {
            if (handleConfig.isEnabled() && handleServiceImpl.getMode() == HandleConfig.Mode.epic) {
                Objects.requireNonNull(getEpicConfig().getUrl(), "repository.handleservice.epic.url not set");
                Objects.requireNonNull(getEpicConfig().getUsername(), "repository.handleservice.epic.username not set");
                Objects.requireNonNull(getEpicConfig().getPassword(), "repository.handleservice.epic.password not set");
            }
        } catch (Throwable t) {
            log.error("Could not initialize ePIC handle service properly cause of error in config, please check config for \"repository.handleservice\"", t);
        }
    }

    @Override
    public boolean enabled() {
        return handleConfig.isEnabled();
    }

    @Override
    public boolean available() {
        try {
            URI uri = getUrl(UriUtils.encodePathSegment(handleConfig.getPrefix(), StandardCharsets.UTF_8) + "/?limit=1");
            ResponseEntity<String> response = template.exchange(uri, HttpMethod.GET, new HttpEntity<>(getHeaders()), String.class);
            return response.getStatusCode().is2xxSuccessful();
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
        return putHandle(handleId, getContentLink(properties));
    }

    @Override
    public String update(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        return putHandle(handleId, getContentLink(properties));
    }

    @Override
    public String sync(String handleId, String nodeId, Map<QName, Serializable> properties) throws Exception {
        // handleservice has no option to manage metadata
        return handleId;
    }

    @Override
    public String delete(String handleId, String nodeId) throws Exception {
        //not implemented, ePIC does not allow deleting production handles
        return "";
    }

    @Override
    public String getHandleIdProperty() {
        return CCConstants.CCM_PROP_PUBLISHED_HANDLE_ID;
    }

    /**
     * creates the handle with the given (self generated) suffix or replaces its values
     */
    private String putHandle(String handleId, String url) throws IOException {
        JsonArray values = new JsonArray();
        values.add(getValue("URL", url));
        values.add(getValue("EMAIL", handleServiceImpl.getEmail()));

        URI uri = getUrl(UriUtils.encodePath(handleId, StandardCharsets.UTF_8));
        try {
            template.exchange(uri, HttpMethod.PUT, new HttpEntity<>(values.toString(), getHeaders()), String.class);
        } catch (HttpStatusCodeException e) {
            throw new IOException("ePIC api PUT " + uri.getPath() + " failed with status "
                    + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        }
        return handleId;
    }

    private JsonObject getValue(String type, String data) {
        JsonObject value = new JsonObject();
        value.addProperty("type", type);
        value.addProperty("parsed_data", data);
        return value;
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(getEpicConfig().getUsername(), getEpicConfig().getPassword(), StandardCharsets.UTF_8);
        return headers;
    }

    private URI getUrl(String path) {
        return URI.create(StringUtils.removeEnd(getEpicConfig().getUrl(), "/") + "/" + path);
    }

    private HandleConfig.Epic getEpicConfig() {
        return handleConfig.getEpic() == null ? new HandleConfig.Epic() : handleConfig.getEpic();
    }
}
