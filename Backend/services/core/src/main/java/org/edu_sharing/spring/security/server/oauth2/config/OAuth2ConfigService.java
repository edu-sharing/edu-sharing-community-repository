package org.edu_sharing.spring.security.server.oauth2.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import lombok.extern.slf4j.Slf4j;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.security.KeyStoreService;
import org.edu_sharing.repository.update.KeyGenerator;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Slf4j
@Component
public class OAuth2ConfigService {

    public static final String OAUTH2_RSA =  "oauth2-rsa";

    public static final String CONFIG_PATH = "security.authentication.oauth2";

    KeyStoreService keyStoreService = new KeyStoreService();

    public OAuth2Config getDefaultConfig() {
        Config config = LightbendConfigLoader.get().getConfig(CONFIG_PATH);
        return ConfigBeanFactory.create(config, OAuth2Config.class);
    }


    public JWKSource<SecurityContext> getJwkSource() throws Exception {
        ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
        KeyStore keyStore = keyStoreService.getKeyStore(CCConstants.EDU_PASSWORD_KEYSTORE_NAME, homeRepo.getKeyStorePassword());
        RSAKey rsaKey;
        if(keyStore.containsAlias(OAUTH2_RSA)){
            log.info("loading jwk keyset");
            Key key = keyStore.getKey(OAUTH2_RSA, homeRepo.getKeyStorePassword().toCharArray());
            if (!(key instanceof RSAPrivateKey)) {
                throw new IllegalStateException("Stored key is not an RSA private key");
            }
            java.security.cert.Certificate cert = keyStore.getCertificate(OAUTH2_RSA);
            RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

            rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey((RSAPrivateKey)key)
                    .keyID(OAUTH2_RSA)
                    .build();
        }else {
            log.info("generating jwk keyset");
            // Generate once
            rsaKey = generateRsa();

            // Store in keystore
            KeyPair kp = new KeyPair(rsaKey.toRSAPublicKey(), rsaKey.toRSAPrivateKey());

            java.security.cert.Certificate cert = new KeyGenerator().generateCertificate(homeRepo,kp.getPrivate(), kp.getPublic());
            keyStore.setKeyEntry(OAUTH2_RSA, kp.getPrivate(), homeRepo.getKeyStorePassword().toCharArray(), new java.security.cert.Certificate[]{cert});
            keyStoreService.storeKeyStore(keyStore,CCConstants.EDU_PASSWORD_KEYSTORE_NAME, homeRepo.getKeyStorePassword());
        }

        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    private static RSAKey generateRsa() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey(keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

}
