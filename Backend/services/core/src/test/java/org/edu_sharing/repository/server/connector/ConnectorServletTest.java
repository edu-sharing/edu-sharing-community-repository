package org.edu_sharing.repository.server.connector;

import com.github.davidmoten.guavamini.Maps;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.alfresco.service.connector.SimpleConnector;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SimpleErrorWithDetailsException;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;

class ConnectorServletTest {

    private ConnectorServlet underTest;

    @BeforeEach
    void setUp() {
        underTest = new ConnectorServlet();
    }

    @Test
    @Disabled
    void handleSimpleConnectorTestOauthFormCurriculum() throws UnsupportedEncodingException, SimpleErrorWithDetailsException {
        SimpleConnector connector = new SimpleConnector();
        SimpleConnector.SimpleConnectorApi api = new SimpleConnector.SimpleConnectorApi();
        connector.setApi(api);
        api.setUrl("https://curriculum-dev.schulcampus-rlp.de/api/v1/kanbans");
        api.setMethod(SimpleConnector.SimpleConnectorApi.Method.Post);
        api.setPostRequestHandler("org.edu_sharing.alfresco.service.connector.defaulthandler.CurriculumPostRequestHandler");
        api.setBodyHandler("org.edu_sharing.alfresco.service.connector.bodyhandler.CurriculumBodyHandler");
        api.setBodyType(SimpleConnector.SimpleConnectorApi.BodyType.Form);
        api.setBody(Map.ofEntries(
                        Map.entry("owner_cn", "uuid")
                )
        );
        SimpleConnector.SimpleConnectorAuthentication authentication = new SimpleConnector.SimpleConnectorAuthentication();
        api.setAuthentication(authentication);
        authentication.setUrl("https://curriculum-dev.schulcampus-rlp.de/oauth/token");
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
        HashMap<String, Serializable> result = underTest.handleSimpleConnector(Map.ofEntries(
                Map.entry("curriculum_title", new String[]{"Sample-Kanban " + UUID.randomUUID()}),
                Map.entry("curriculum_description", new String[]{"Sample-Kanban Description " + UUID.randomUUID()})
        ), connector, null);
        assertThat(result.get(CCConstants.CCM_PROP_IO_WWWURL).toString(), CoreMatchers.containsString("https://curriculum-dev.schulcampus-rlp.de/kanbans/"));
    }

    @Test
    void handleSimpleConnectorUrlWithoutApi() {
        SimpleConnector connector = simpleConnectorWithUrl("https://example.tld/md-editor/?nodeId={{nodeId}}&hidePicker=true");

        HashMap<String, Serializable> result = underTest.handleSimpleConnectorUrl(
                Map.of("nodeId", new String[]{"4711"}), connector
        );

        assertThat(result.get(CCConstants.CCM_PROP_IO_WWWURL),
                CoreMatchers.is("https://example.tld/md-editor/?nodeId=4711&hidePicker=true"));
        assertThat(result.get(CCConstants.CCM_PROP_CCRESSOURCETYPE),
                CoreMatchers.is(RessourceInfoExecuter.CCM_RESSOURCETYPE_CONNECTOR));
        assertThat(result.get(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE), CoreMatchers.is("markdown"));
    }

    @Test
    void handleSimpleConnectorUrlDropsUnknownVariables() {
        SimpleConnector connector = simpleConnectorWithUrl("https://example.tld/md-editor/?nodeId={{unknown}}");

        HashMap<String, Serializable> result = underTest.handleSimpleConnectorUrl(Map.of(), connector);

        assertThat(result.get(CCConstants.CCM_PROP_IO_WWWURL), CoreMatchers.is("https://example.tld/md-editor/?nodeId="));
    }

    @Test
    void extractSimpleConnectorTargetRedirectModeRedirect() {
        SimpleConnector connector = simpleConnectorWithUrl("https://example.tld/md-editor/");
        connector.setRedirectMode(SimpleConnector.RedirectMode.Redirect);
        HashMap<String, Serializable> properties = underTest.handleSimpleConnectorUrl(Map.of(), connector);

        String redirect = underTest.extractSimpleConnectorTarget(connector, properties);

        assertThat(redirect, CoreMatchers.is("https://example.tld/md-editor/"));
        // the target must not be stored on the element, but the connector markers must remain
        assertThat(properties.containsKey(CCConstants.CCM_PROP_IO_WWWURL), CoreMatchers.is(false));
        assertThat(properties.get(CCConstants.CCM_PROP_CCRESSOURCESUBTYPE), CoreMatchers.is("markdown"));
    }

    @Test
    void extractSimpleConnectorTargetRedirectModeLinkIsDefault() {
        SimpleConnector connector = simpleConnectorWithUrl("https://example.tld/md-editor/");
        HashMap<String, Serializable> properties = underTest.handleSimpleConnectorUrl(Map.of(), connector);

        String redirect = underTest.extractSimpleConnectorTarget(connector, properties);

        // the target is resolved from the element, so it has to be kept in the properties
        assertThat(connector.getRedirectMode(), CoreMatchers.is(SimpleConnector.RedirectMode.Link));
        assertThat(redirect, CoreMatchers.nullValue());
        assertThat(properties.get(CCConstants.CCM_PROP_IO_WWWURL), CoreMatchers.is("https://example.tld/md-editor/"));
    }

    private SimpleConnector simpleConnectorWithUrl(String url) {
        SimpleConnector connector = new SimpleConnector();
        connector.setId("markdown");
        connector.setUrl(url);
        return connector;
    }
}
