package org.edu_sharing.saml;

import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.x509.PKIXTrustEvaluator;
import org.opensaml.xml.security.x509.PKIXValidationInformation;
import org.opensaml.xml.security.x509.PKIXValidationOptions;
import org.opensaml.xml.security.x509.X509Credential;

public class ESPKIXTrustEvaluator implements PKIXTrustEvaluator {
    private final PKIXValidationOptions options;

    public ESPKIXTrustEvaluator() {
        options = new PKIXValidationOptions();
    }

    @Override
    public boolean validate(PKIXValidationInformation validationInfo, X509Credential untrustedCredential) throws SecurityException {
        return true;
    }

    @Override
    public PKIXValidationOptions getPKIXValidationOptions() {
        return options;
    }
}
