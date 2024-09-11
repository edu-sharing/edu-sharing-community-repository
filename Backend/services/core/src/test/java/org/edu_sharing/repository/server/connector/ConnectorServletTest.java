package org.edu_sharing.repository.server.connector;

import com.github.davidmoten.guavamini.Maps;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.UUID;

class ConnectorServletTest {

    private ConnectorServlet underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConnectorServlet();
    }

    @Test
    @Disabled
    void handleSimpleConnectorTestOauthForm() throws UnsupportedEncodingException {
        SimpleConnector connector = new SimpleConnector();
        SimpleConnector.SimpleConnectorApi api = new SimpleConnector.SimpleConnectorApi();
        connector.setApi(api);
        api.setUrl("https://example.tld");
        api.setMethod(SimpleConnector.SimpleConnectorApi.Method.Post);
        api.setBodyType(SimpleConnector.SimpleConnectorApi.BodyType.Form);
        api.setBody(Map.ofEntries(
                        Map.entry("title", "Sample-Kanban " + UUID.randomUUID()),
                        // Map.entry("owner_cn", "${user.cm:authorityName}")
                )
        );
        SimpleConnector.SimpleConnectorAuthentication authentication = new SimpleConnector.SimpleConnectorAuthentication();
        api.setAuthentication(authentication);
        authentication.setUrl("https://example.tld/oauth");
        authentication.setMethod(SimpleConnector.SimpleConnectorApi.Method.Post);
        authentication.setBodyType(SimpleConnector.SimpleConnectorApi.BodyType.Form);
        authentication.setType(SimpleConnector.SimpleConnectorAuthentication.AuthenticationType.OAuth);
        authentication.setBody(Map.ofEntries(
                        Map.entry("grant_type", "client_credentials"),
                        Map.entry("client_id", "xxx"),
                        Map.entry("client_secret", "xxx")
                )
        );
        connector.setId("test");
        underTest.handleSimpleConnector(Maps.empty(), connector, null);
    }
}