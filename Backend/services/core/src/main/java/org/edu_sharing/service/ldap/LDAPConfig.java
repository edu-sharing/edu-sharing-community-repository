package org.edu_sharing.service.ldap;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "repository.register.ldap")
public class LDAPConfig {
    String authentication;
    String server;
    String username;
    String password;
    String baseDN;
    String passwordAlgorithm;
    String userRdn;
}
