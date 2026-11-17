package org.edu_sharing.spring.security.oauth2;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ClientProperties;
import org.edu_sharing.spring.security.oauth2.config.OAuth2ConfigProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The silent login must never be switched off for a browser, so most of the tests are about what is not skipped.
 */
class SilentLoginModeRedirectSkipClientsTest {

    private static final String SILENT_LOGIN_REDIRECT = "/edu-sharing/oauth2/authorization/dev2-mediathek_openIdConnect?prompt=none";

    private static final List<String> BROWSERS = List.of(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/25.0 Chrome/121.0.0.0 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 10; CUBOT_X30) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36",
            // the in-app browsers of social networks and messengers are browsers, a person uses them
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 [FBAN/FBIOS;FBAV/470.0.0.40.101;FBBV/6000;FBDV/iPhone15,2]",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 330.0.0.20.114",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 [Pinterest/iOS]",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 LinkedInApp/9.30",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/126.0.0.0 Mobile Safari/537.36");

    private static final List<String> DEFAULT_NAMES = List.of(
            "Googlebot", "Google-InspectionTool", "GoogleOther", "Google-Read-Aloud", "FeedFetcher-Google", "Chrome-Lighthouse",
            "bingbot", "BingPreview", "DuckDuckBot", "Applebot", "YandexBot", "Baiduspider", "PetalBot", "Bytespider", "ia_archiver",
            "facebookexternalhit", "Twitterbot", "LinkedInBot", "Slackbot", "TelegramBot", "Discordbot", "WhatsApp", "Embedly",
            "AhrefsBot", "SemrushBot", "MJ12bot", "SiteimproveBot", "Screaming Frog", "UptimeRobot", "Pingdom",
            "curl/", "Wget/", "python-requests", "Go-http-client");

    // ---- the names

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Google-InspectionTool/1.0)",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36 (compatible; GoogleOther)",
            "Mozilla/5.0 (Linux; Android 11; moto g power (2022)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Mobile Safari/537.36 Chrome-Lighthouse",
            "Mozilla/5.0 (Windows Phone 8.1; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 530) like Gecko BingPreview/1.0b",
            "WhatsApp/2.23.20.0 A",
            "curl/8.19.0",
            "Wget/1.21.4",
            "python-requests/2.31.0",
            "Go-http-client/2.0",
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "FEEDFETCHER-GOOGLE; (+http://www.google.com/feedfetcher.html)"
    })
    void namesOfTheListAreFoundInTheUserAgent(String userAgent) {
        // Arrange (the names of the default config)

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName(userAgent, DEFAULT_NAMES);

        // Assert
        assertTrue(matches, userAgent);
    }

    @Test
    void noBrowserMatchesTheNamesOfTheList() {
        // Arrange (the names of the default config, the user agents of common browsers and in-app browsers)

        // Act
        List<String> matching = BROWSERS.stream()
                .filter(userAgent -> SilentLoginModeRedirect.matchesName(userAgent, DEFAULT_NAMES))
                .toList();

        // Assert
        assertEquals(List.of(), matching);
    }

    @Test
    void noBrowserIsACrawlerEither() {
        // Arrange (the user agents of common browsers and in-app browsers)

        // Act
        List<String> crawlers = BROWSERS.stream().filter(SilentLoginModeRedirect::isCrawler).toList();

        // Assert
        assertEquals(List.of(), crawlers);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mozilla", "Mozilla/5.0", "Chrome", "Safari", "Gecko", "AppleWebKit", "Windows", "Android", "iPhone", "Firefox", "KHTML", "Version"})
    void aNameThatMatchesABrowserIsIgnored(String name) {
        // Arrange
        String chrome = BROWSERS.get(0);

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName(chrome, List.of(name));

        // Assert
        assertFalse(matches, name + " would switch the silent login off for the browsers");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "a", "ab", " ab "})
    void aBlankOrVeryShortNameIsIgnored(String name) {
        // Arrange
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        names.add(name);

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName(BROWSERS.get(0), names);

        // Assert
        assertFalse(matches);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "a", " ab ", "-"})
    void aVeryShortNameIsIgnoredEvenIfTheUserAgentContainsIt(String name) {
        // Arrange (a user agent that is no browser and contains the name, so only the length can keep it from matching)
        String userAgent = "Some-Client/1.0 (ab - a)";

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName(userAgent, List.of(name));

        // Assert
        assertFalse(matches, "'" + name + "' is too short to name a client");
    }

    @Test
    void aNameOfThreeCharactersIsEnough() {
        // Arrange
        String userAgent = "Some-Client/1.0 (abc)";

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName(userAgent, List.of("abc"));

        // Assert
        assertTrue(matches);
    }

    @Test
    void anIgnoredNameDoesNotKeepTheOtherNamesFromWorking() {
        // Arrange
        List<String> names = List.of("Mozilla", "", "Google-InspectionTool");

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName("Mozilla/5.0 (compatible; Google-InspectionTool/1.0)", names);

        // Assert
        assertTrue(matches);
    }

    @Test
    void theNamesAreNotCaseSensitive() {
        // Arrange
        List<String> names = List.of("googlebot");

        // Act
        boolean matches = SilentLoginModeRedirect.matchesName("Mozilla/5.0 (compatible; GOOGLEBOT/2.1)", names);

        // Assert
        assertTrue(matches);
    }

    @Test
    void withoutUserAgentOrNamesNothingMatches() {
        // Arrange

        // Act & Assert
        assertFalse(SilentLoginModeRedirect.matchesName(null, DEFAULT_NAMES));
        assertFalse(SilentLoginModeRedirect.matchesName("", DEFAULT_NAMES));
        assertFalse(SilentLoginModeRedirect.matchesName("curl/8.19.0", List.of()));
        assertFalse(SilentLoginModeRedirect.matchesName("curl/8.19.0", null));
    }

    @Test
    void theDefaultConfigHasNoNameThatIsIgnored() throws Exception {
        // Arrange
        Path file = Path.of("../../../config/defaults/src/main/resources/edu-sharing.reference.conf");
        Assumptions.assumeTrue(Files.exists(file), "the reference config is not at " + file.toAbsolutePath());
        List<String> names = ConfigFactory.parseFile(file.toFile()).resolve().getStringList("security.sso.silentLoginSkipUserAgents");

        // Act
        List<String> notFound = names.stream()
                .filter(name -> !SilentLoginModeRedirect.matchesName("Mozilla/5.0 (compatible; " + name + "/1.0)", List.of(name)))
                .toList();

        // Assert
        assertEquals(DEFAULT_NAMES, names, "the list of the tests and the one of the reference config differ");
        assertEquals(List.of(), notFound, "these names are ignored (blank, short or the name of a browser)");
    }

    // ---- the redirect

    @Test
    void aBrowserGetsTheSilentLogin() throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        boolean redirected = process(BROWSERS.get(0), configWith("\"Google-InspectionTool\""), response);

        // Assert
        assertTrue(redirected);
        verify(response).sendRedirect(SILENT_LOGIN_REDIRECT);
    }

    @Test
    void everyBrowserGetsTheSilentLogin() throws Exception {
        // Arrange
        Config config = configWith(DEFAULT_NAMES.stream().map(name -> "\"" + name + "\"").reduce((a, b) -> a + "," + b).orElseThrow());

        // Act
        List<String> notRedirected = BROWSERS.stream().filter(userAgent -> {
            try {
                return !process(userAgent, config, mock(HttpServletResponse.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).toList();

        // Assert
        assertEquals(List.of(), notRedirected);
    }

    /**
     * clients the pattern of the crawlers does not know, so only the names of the config keep them from the redirect
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Google-InspectionTool/1.0)",
            "Mozilla/5.0 (Linux; Android 11; moto g power (2022)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Mobile Safari/537.36 Chrome-Lighthouse",
            "Mozilla/5.0 (Windows Phone 8.1; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 530) like Gecko BingPreview/1.0b",
            "WhatsApp/2.23.20.0 A",
            "curl/8.19.0",
            "Wget/1.21.4",
            "python-requests/2.31.0",
            "Go-http-client/2.0"
    })
    void aNameOfTheListGetsNoRedirect(String userAgent) throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Config config = configWith(DEFAULT_NAMES.stream().map(name -> "\"" + name + "\"").reduce((a, b) -> a + "," + b).orElseThrow());

        // Act
        boolean redirected = process(userAgent, config, response);

        // Assert
        assertFalse(redirected, userAgent);
        verify(response, never()).sendRedirect(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Google-InspectionTool/1.0", "WhatsApp/2.23.20.0 A", "curl/8.19.0"})
    void withoutTheNameInTheConfigTheSameClientGetsTheRedirect(String userAgent) throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        boolean redirected = process(userAgent, configWith("\"Googlebot\""), response);

        // Assert
        assertTrue(redirected, userAgent);
        verify(response).sendRedirect(SILENT_LOGIN_REDIRECT);
    }

    @Test
    void aNameThatIsNotInTheListGetsTheRedirect() throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act (the pattern of the crawlers does not match "Google-InspectionTool" either)
        boolean redirected = process("Mozilla/5.0 (compatible; Google-InspectionTool/1.0)", configWith(""), response);

        // Assert
        assertTrue(redirected);
        verify(response).sendRedirect(SILENT_LOGIN_REDIRECT);
    }

    @Test
    void aBrowserKeepsTheSilentLoginAlthoughTheConfigHasABrowserName() throws Exception {
        // Arrange (a typo in the application.conf)
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        boolean redirected = process(BROWSERS.get(0), configWith("\"Mozilla\", \"Chrome\", \"\", \"  \""), response);

        // Assert
        assertTrue(redirected);
        verify(response).sendRedirect(SILENT_LOGIN_REDIRECT);
    }

    @Test
    void theCrawlersOfThePatternStillGetNoRedirectWithoutAConfig() throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);
        Config configWithoutTheKey = ConfigFactory.empty();

        // Act
        boolean redirected = process("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)", configWithoutTheKey, response);

        // Assert
        assertFalse(redirected);
        verify(response, never()).sendRedirect(anyString());
    }

    @Test
    void aClientWithoutAUserAgentGetsTheRedirect() throws Exception {
        // Arrange
        HttpServletResponse response = mock(HttpServletResponse.class);

        // Act
        boolean redirected = process(null, configWith("\"curl/\""), response);

        // Assert
        assertTrue(redirected);
        verify(response).sendRedirect(SILENT_LOGIN_REDIRECT);
    }

    private static Config configWith(String quotedNames) {
        return ConfigFactory.parseString("security.sso.silentLoginSkipUserAgents: [" + quotedNames + "]");
    }

    private static boolean process(String userAgent, Config config, HttpServletResponse response) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("/edu-sharing");
        when(request.getRequestURI()).thenReturn("/edu-sharing/components/start");
        when(request.getServletPath()).thenReturn("/components");
        when(request.getPathInfo()).thenReturn("/start");
        when(request.getHeader("User-Agent")).thenReturn(userAgent);
        when(request.getSession()).thenReturn(session);

        OAuth2ClientProperties properties = mock(OAuth2ClientProperties.class);
        when(properties.getDefaultRegistration()).thenReturn("openIdConnect");
        when(properties.getRegistrationId("openIdConnect")).thenReturn("dev2-mediathek_openIdConnect");
        OAuth2ConfigProvider configProvider = mock(OAuth2ConfigProvider.class);
        when(configProvider.getConfig(any())).thenReturn(properties);
        LightbendConfigLoader configLoader = mock(LightbendConfigLoader.class);
        when(configLoader.getConfig()).thenReturn(config);

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        Environment environment = mock(Environment.class);
        when(applicationContext.getEnvironment()).thenReturn(environment);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        try (MockedStatic<ApplicationContextFactory> applicationContextFactory = Mockito.mockStatic(ApplicationContextFactory.class);
             MockedStatic<ConfigServiceFactory> configServiceFactory = Mockito.mockStatic(ConfigServiceFactory.class)) {
            applicationContextFactory.when(ApplicationContextFactory::getApplicationContext).thenReturn(applicationContext);
            configServiceFactory.when(ConfigServiceFactory::getCurrentConfig).thenReturn(null);

            return new SilentLoginModeRedirect(configProvider, configLoader).process(request, response);
        }
    }
}
