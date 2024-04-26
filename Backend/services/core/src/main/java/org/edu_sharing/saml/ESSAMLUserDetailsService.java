package org.edu_sharing.saml;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

/**
 *
 *
 *     <bean id="samlAuthenticationProvider" class="org.springframework.security.saml.SAMLAuthenticationProvider">
 *           <property name="userDetails" ref="userDetailsService" />
 *     </bean>
 *
 *     <bean id="userDetailsService" class="org.edu_sharing.saml.ESSAMLUserDetailsService">
 *         <property name="userNameAttribute" value="id"/>
 *     </bean>
 *
 */
public class ESSAMLUserDetailsService implements SAMLUserDetailsService {

    String userNameAttribute;

    @Override
    public Object loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        return credential.getAttributeAsString(userNameAttribute);
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }
}
