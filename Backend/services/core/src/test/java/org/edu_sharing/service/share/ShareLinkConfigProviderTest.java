package org.edu_sharing.service.share;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareLinkConfigProviderTest {

    @Mock
    private LightbendConfigLoader configLoader;

    private ShareLinkConfigProvider providerFor(String hocon) {
        Config config = ConfigFactory.parseString(hocon);
        when(configLoader.getConfig()).thenReturn(config);
        return new ShareLinkConfigProvider(configLoader);
    }

    @Test
    void getDefaults_withoutConfig_allowsViewAndDownload() {
        // Arrange
        ShareLinkConfigProvider underTest = providerFor("");

        // Act
        ShareLinkConfigProvider.Defaults result = underTest.getDefaults(CCConstants.EDUCONTEXT_DEFAULT);

        // Assert
        assertEquals(new ShareLinkConfigProvider.Defaults(true, true), result);
    }

    @Test
    void getDefaults_withGlobalConfig_usesGlobalValues() {
        // Arrange
        ShareLinkConfigProvider underTest = providerFor("repository.sharing.link { view: true, download: false }");

        // Act
        ShareLinkConfigProvider.Defaults result = underTest.getDefaults(CCConstants.EDUCONTEXT_DEFAULT);

        // Assert
        assertEquals(new ShareLinkConfigProvider.Defaults(true, false), result);
    }

    @Test
    void getDefaults_withContextOverride_overridesOnlyTheGivenValue() {
        // Arrange
        ShareLinkConfigProvider underTest = providerFor("""
                repository.sharing.link { view: true, download: true }
                repository.context.rlp.repository.sharing.link { download: false }
                """);

        // Act
        ShareLinkConfigProvider.Defaults context = underTest.getDefaults("rlp");
        ShareLinkConfigProvider.Defaults other = underTest.getDefaults("other");

        // Assert
        assertEquals(new ShareLinkConfigProvider.Defaults(true, false), context);
        assertEquals(new ShareLinkConfigProvider.Defaults(true, true), other);
    }

    @Test
    void getDefaults_withContextContainingDots_resolvesQuotedPath() {
        // Arrange
        ShareLinkConfigProvider underTest = providerFor("""
                repository.context."a.b".repository.sharing.link { view: false }
                """);

        // Act
        ShareLinkConfigProvider.Defaults result = underTest.getDefaults("a.b");

        // Assert
        assertEquals(new ShareLinkConfigProvider.Defaults(false, true), result);
    }

    @Test
    void getDefaults_withNeitherViewNorDownload_fallsBackToDownload() {
        // Arrange
        ShareLinkConfigProvider underTest = providerFor("repository.sharing.link { view: false, download: false }");

        // Act
        ShareLinkConfigProvider.Defaults result = underTest.getDefaults(CCConstants.EDUCONTEXT_DEFAULT);

        // Assert
        assertEquals(new ShareLinkConfigProvider.Defaults(false, true), result);
    }
}
