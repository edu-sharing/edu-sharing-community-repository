package org.edu_sharing.saml;

import org.opensaml.common.SAMLException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.validation.ValidationException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;

/**
 * fix serializing issue of attributes list with new redisson version
 *
 * <bean id="webSSOprofileConsumer" class="org.edu_sharing.saml.ESWebSSOProfileConsumerImpl">
 *         <property name="maxAuthenticationAge" value="43200"/>
 *  </bean>
 *
 */
public class ESWebSSOProfileConsumerImpl extends WebSSOProfileConsumerImpl {
    @Override
    public SAMLCredential processAuthenticationResponse(SAMLMessageContext context) throws SAMLException, SecurityException, ValidationException, DecryptionException {
        SAMLCredential c = super.processAuthenticationResponse(context);
        return new ESSAMLCredential(c.getNameID(),c.getAuthenticationAssertion(),c.getRemoteEntityID(),c.getRelayState(),c.getAttributes(), c.getLocalEntityID(),c.getAdditionalData());
    }
}
