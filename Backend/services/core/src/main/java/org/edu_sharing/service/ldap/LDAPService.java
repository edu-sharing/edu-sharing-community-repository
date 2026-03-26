package org.edu_sharing.service.ldap;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.spring.conditions.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.security.InvalidParameterException;
import java.util.Base64;
import java.util.Hashtable;

@ConditionalOnProperty(name = "repository.register.class",havingValue = "RegisterServiceLDAPImpl")
@RequiredArgsConstructor
@Service
public class LDAPService {

    private final LDAPConfig config;

    public void setPassword(String email, String newPassword) throws NamingException {
        LdapContext ctx = getLdapContext();
        String userDN = config.getBaseDN();
        String dn = config.getUserRdn()+"=" + email + "," + userDN;
        ModificationItem[] mods = new ModificationItem[1];
        Attribute mod = new BasicAttribute(
                "userPassword",
                convertPassword(newPassword)
        );
        mods[0] = new ModificationItem(
                DirContext.REPLACE_ATTRIBUTE,
                mod
        );
        ctx.modifyAttributes(dn, mods);
    }

    public boolean checkCredentials(String email, String password) throws NamingException {
        String baseDN = config.getBaseDN();
        String userDN = config.getUserRdn() + "=" + email + "," + baseDN;

        // 1️⃣ Try to bind with the old password to validate
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getServer());
        env.put(Context.SECURITY_PRINCIPAL, userDN);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");

        try {
            new InitialLdapContext(env, null); // binds with old password
        } catch (javax.naming.AuthenticationException | NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public boolean userExists(String email) throws NamingException {
        LdapContext ctx = getLdapContext();
        String baseDN = config.getBaseDN();

        String filter = "(" + config.getUserRdn() + "=" + email + ")";

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        controls.setCountLimit(1); // Performance: wir brauchen nur 1 Treffer

        NamingEnumeration<SearchResult> results =
                ctx.search(baseDN, filter, controls);

        return results.hasMore();
    }


    private String convertPassword(String password) throws InvalidParameterException {
        if(password==null)
            return null;
        String algorithm = config.getPasswordAlgorithm();
        if("md5".equalsIgnoreCase(algorithm)){
            return "{MD5}"+ Base64.getEncoder().encodeToString(DigestUtils.md5(password));
        }
        if("none".equalsIgnoreCase(algorithm)){
            return password;
        }
        throw new InvalidParameterException("Unknown ldap password algorithm: "+algorithm+". Set a valid option for ldap.passwordAlgorithm");
    }


    private LdapContext getLdapContext() throws NamingException {

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.SECURITY_AUTHENTICATION, config.getAuthentication());
        if(config.getUsername() != null) {
            env.put(Context.SECURITY_PRINCIPAL, config.getUsername() );
        }
        if(config.getPassword() != null) {
            env.put(Context.SECURITY_CREDENTIALS, config.getPassword());
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getServer());

        return new InitialLdapContext(env,null);
    }

}
