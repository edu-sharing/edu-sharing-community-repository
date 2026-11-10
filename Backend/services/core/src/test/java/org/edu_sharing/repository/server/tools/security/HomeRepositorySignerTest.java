package org.edu_sharing.repository.server.tools.security;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.spring.scope.refresh.RefreshScope;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.edu_sharing.repository.server.tools.security.SigningTestVector.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class HomeRepositorySignerTest {

    @Test
    void signsWithTheKeyOfTheHomeApplicationInThePemFormatOfTheRepository() throws Exception {
        HomeRepositorySigner signer = signer("SHA512withRSA");
        String signature = signer.sign("hello");
        assertTrue(verify("hello", signature, "SHA512withRSA"));
        assertEquals(APP_ID, signer.appId());
        assertEquals("SHA512withRSA", signer.algorithm());
        // another algorithm on request, e.g. the one a receiver names
        assertTrue(verify("hello", signer.sign("hello", "SHA256withRSA"), "SHA256withRSA"));
        // deterministic: the same bytes every time (and the key is parsed once)
        assertEquals(signature, signer.sign("hello"));
    }

    @Test
    void namesItsKeyForAJwsAndGivesRawSignatures() throws Exception {
        HomeRepositorySigner signer = signer("SHA512withRSA");
        assertEquals(KID, signer.jwkThumbprint(), "RFC 7638 thumbprint of the public key that belongs to the private one");
        assertEquals("RS512", signer.jwsAlgorithm());
        assertEquals("RS256", signer("SHA256withRSA").jwsAlgorithm());
        assertEquals("RS384", signer("SHA384withRSA").jwsAlgorithm());
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signer("SHA1withRSA").jwsAlgorithm());
        byte[] raw = signer.signBytes("hello".getBytes(java.nio.charset.StandardCharsets.UTF_8), "SHA512withRSA");
        assertEquals(signer.sign("hello"), java.util.Base64.getEncoder().encodeToString(raw));
    }

    @Test
    void withoutAHomeApplicationAnAppIdOrAKeyItFailsWithItsOwnException() {
        ApplicationInfo noKey = Mockito.mock(ApplicationInfo.class);
        when(noKey.getAppId()).thenReturn(APP_ID);
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signerOf(noKey, "SHA512withRSA").sign("x"));
        ApplicationInfo noApp = Mockito.mock(ApplicationInfo.class);
        when(noApp.getPrivateKey()).thenReturn(privateKeyPem());
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signerOf(noApp, "SHA512withRSA").appId());
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signerOf(null, "SHA512withRSA").appId());
        ApplicationInfo rubbish = Mockito.mock(ApplicationInfo.class);
        when(rubbish.getPrivateKey()).thenReturn("not a key");
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signerOf(rubbish, "SHA512withRSA").sign("x"));
        assertThrows(HomeRepositorySigner.SigningFailedException.class, () -> signer("NoSuchAlgorithm").sign("x"));
    }

    @Test
    void aBlankAlgorithmIsTheDefault() {
        assertEquals("SHA512withRSA", new HomeRepositorySigner("").algorithm());
        assertEquals("SHA512withRSA", new HomeRepositorySigner("  ").algorithm());
        assertEquals("SHA512withRSA", new HomeRepositorySigner(null).algorithm());
        assertEquals("SHA256withRSA", new HomeRepositorySigner(" SHA256withRSA ").algorithm());
    }

    // ---- the Spring side: @Value in a @RefreshScope bean, with the CGLIB proxy ---------------------------------------------

    /** What another bean sees: a singleton that gets the signer injected, as the proxy service of the B-API does. */
    public static class Consumer {
        final HomeRepositorySigner signer;

        public Consumer(HomeRepositorySigner signer) {
            this.signer = signer;
        }
    }

    @Test
    void theAlgorithmIsReadFromTheConfigurationThroughValueAndChangesAtRuntimeWithTheScopedProxy() {
        Map<String, Object> config = new HashMap<>();
        config.put("security.sso.authByApp.alg.defaultSign", "SHA256withRSA");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", config));
            context.register(RefreshScope.class, HomeRepositorySigner.class, Consumer.class);
            context.refresh();

            HomeRepositorySigner proxy = context.getBean(Consumer.class).signer;
            assertTrue(AopUtils.isCglibProxy(proxy), "the scoped proxy is made with CGLIB, the class is open for it");
            assertEquals("SHA256withRSA", proxy.algorithm());

            config.put("security.sso.authByApp.alg.defaultSign", "SHA512withRSA");
            assertEquals("SHA256withRSA", proxy.algorithm(), "the bean is not changed until the scope is refreshed");
            context.getBean(RefreshScope.class).refreshAll(true);
            assertEquals("SHA512withRSA", proxy.algorithm(), "a refresh gives the new value without a restart");

            config.remove("security.sso.authByApp.alg.defaultSign");
            context.getBean(RefreshScope.class).refreshAll(true);
            assertEquals("SHA512withRSA", proxy.algorithm(), "no setting is the default");
        }
    }
}
