package org.edu_sharing.service.bapi;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class BApiConfigProviderTest {

    private BApiConfigProvider provider(String hocon) {
        Config config = ConfigFactory.parseString(hocon);
        LightbendConfigLoader loader = Mockito.mock(LightbendConfigLoader.class);
        when(loader.getConfig()).thenReturn(config);
        return new BApiConfigProvider(loader);
    }

    private static final String GLOBAL = """
            repository.bapi { uri: "https://bapi", authUserApiKey: "A", guestUserApiKey: "G" }
            """;

    @Test
    void defaultWithoutContext() {
        BApiProxyConfig config = provider(GLOBAL).getConfig(null);
        assertEquals("A", config.getAuthUserApiKey());
        assertEquals("G", config.getGuestUserApiKey());
    }

    @Test
    void missingBapiPathYieldsEmptyConfig() {
        BApiProxyConfig config = provider("repository {}").getConfig("ctx");
        assertNull(config.getUri());
        assertNull(config.getAuthUserApiKey());
    }

    @Test
    void contextOverridesKeysAndInheritsUri() {
        BApiProxyConfig config = provider(GLOBAL + """
                repository.context.ctx.repository.bapi { authUserApiKey: "A2", guestUserApiKey: "G2" }
                """).getConfig("ctx");
        assertEquals("A2", config.getAuthUserApiKey());
        assertEquals("G2", config.getGuestUserApiKey());
        assertEquals("https://bapi", config.getUri());
    }

    @Test
    void unknownContextFallsBackToDefault() {
        BApiProxyConfig config = provider(GLOBAL).getConfig("unknown");
        assertEquals("A", config.getAuthUserApiKey());
    }

    @Test
    void globalFallbackDisabledDoesNotInheritKeysButKeepsUri() {
        BApiProxyConfig config = provider("""
                repository.bapi { uri: "https://bapi", authUserApiKey: "A", guestUserApiKey: "G", fallback: false }
                repository.context.ctx.repository.bapi { authUserApiKey: "A2" }
                """).getConfig("ctx");
        assertEquals("A2", config.getAuthUserApiKey());
        assertNull(config.getGuestUserApiKey());
        assertEquals("https://bapi", config.getUri());
    }

    @Test
    void globalFallbackDisabledWithoutContextBlockYieldsNoKeys() {
        BApiProxyConfig config = provider("""
                repository.bapi { uri: "https://bapi", authUserApiKey: "A", fallback: false }
                """).getConfig("ctx");
        assertEquals("https://bapi", config.getUri());
        assertNull(config.getAuthUserApiKey());
    }

    @Test
    void contextOverridesFeatures() {
        BApiProxyConfig config = provider("""
                repository.bapi { uri: "https://bapi", features: ["url-fulltext"] }
                repository.context.ctx.repository.bapi { features: ["other"] }
                """).getConfig("ctx");
        assertEquals(java.util.List.of("other"), config.getFeatures());
    }

    @Test
    void featuresInheritedWithFallback() {
        BApiProxyConfig config = provider("""
                repository.bapi { uri: "https://bapi", features: ["url-fulltext"] }
                repository.context.ctx.repository.bapi { authUserApiKey: "A2" }
                """).getConfig("ctx");
        assertEquals(java.util.List.of("url-fulltext"), config.getFeatures());
    }

    @Test
    void featuresNotInheritedWithoutFallback() {
        BApiProxyConfig config = provider("""
                repository.bapi { uri: "https://bapi", features: ["url-fulltext"], fallback: false }
                repository.context.ctx.repository.bapi { authUserApiKey: "A2" }
                """).getConfig("ctx");
        assertTrue(config.getFeatures().isEmpty());
    }

    @Test
    void contextIdWithDot() {
        BApiProxyConfig config = provider(GLOBAL + """
                repository.context."my.ctx".repository.bapi { authUserApiKey: "A3" }
                """).getConfig("my.ctx");
        assertEquals("A3", config.getAuthUserApiKey());
    }
}
