package org.edu_sharing.spring.security.oauth2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SilentLoginModeRedirectTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko; compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "Mozilla/5.0 (compatible; AhrefsBot/7.0; +http://ahrefs.com/robot/)",
            "Mozilla/5.0 (compatible; YandexBot/3.0; +http://yandex.com/bots)",
            "Mozilla/5.0 (compatible; Yahoo! Slurp; http://help.yahoo.com/help/us/ysearch/slurp)",
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
            "Mozilla/5.0 (compatible; SomeCrawler/1.0)",
            "bot"
    })
    void crawlersAreDetected(String userAgent) {
        assertTrue(SilentLoginModeRedirect.isCrawler(userAgent), userAgent);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 10; CUBOT_X30) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36",
            ""
    })
    void browsersAreNotDetectedAsCrawler(String userAgent) {
        assertFalse(SilentLoginModeRedirect.isCrawler(userAgent), userAgent);
    }

    @Test
    void missingUserAgentIsNoCrawler() {
        assertFalse(SilentLoginModeRedirect.isCrawler(null));
    }

    /**
     * the servlets are mapped to the exact path, so the container reports the whole path as servlet path
     * and no path info (web.xml: /eduservlet/sitemap, /eduservlet/oembed, /eduservlet/oai/provider)
     */
    @ParameterizedTest
    @ValueSource(strings = {"/eduservlet/sitemap", "/eduservlet/oembed", "/eduservlet/oai/provider", "/robots.txt"})
    void machineEndpointsWithAnExactMapping(String servletPath) {
        assertTrue(SilentLoginModeRedirect.isMachineEndpoint(servletPath, null), servletPath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sitemap", "/oembed", "/oai", "/oai/", "/oai/sets"})
    void machineEndpointsWithAMappingToAPrefix(String pathInfo) {
        assertTrue(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet", pathInfo), pathInfo);
    }

    @Test
    void otherPathsAreNotMachineEndpoints() {
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet/download", null));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet/oaifoo", null));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet", "/download"));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet", "/oaifoo"));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/other", "/sitemap"));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/components", "/start"));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint("/eduservlet", null));
        assertFalse(SilentLoginModeRedirect.isMachineEndpoint(null, null));
    }
}
