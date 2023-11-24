package org.edu_sharing.saml;

import org.opensaml.xml.security.BasicSecurityConfiguration;
import org.opensaml.xml.signature.SignatureConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class SAMLBootstrap extends org.springframework.security.saml.SAMLBootstrap {

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);

        // Disable SHA-1 XML Signatures (https://bugs.openjdk.java.net/browse/JDK-8259709)
        BasicSecurityConfiguration config = (BasicSecurityConfiguration) org.opensaml.xml.Configuration.getGlobalSecurityConfiguration();
        config.registerSignatureAlgorithmURI("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        config.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
    }

}