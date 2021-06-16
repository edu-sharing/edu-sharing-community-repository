package org.edu_sharing.service.register;

import org.apache.commons.codec.digest.DigestUtils;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.security.InvalidParameterException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Properties;

public class RegisterServiceLDAPImpl extends RegisterServiceImpl {
    @Override
    public boolean userExists(RegisterInformation info) throws Exception {
        if(super.userExists(info))
            return true;
        String userDN = RegisterServiceFactory.getConfig().getProperty("ldap.baseDN");
        LdapContext ctx = getLdapContext();
        boolean exists=false;
        try {
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.OBJECT_SCOPE);
            controls.setCountLimit(1);
            NamingEnumeration<SearchResult> result = ctx.search("uid=" + info.getEmail() + "," + userDN, "(objectclass=inetOrgPerson)", controls);
            exists = result.hasMore();
        }catch(NameNotFoundException e){
            // no search result
        }
        ctx.close();
        return exists;
    }

    @Override
    protected String storeUser(RegisterInformation info) throws Exception {
        LdapContext ctx = getLdapContext();
        String userDN = RegisterServiceFactory.getConfig().getProperty("ldap.baseDN");

        Attributes attrs = getLDAPAttributes(info);

        ctx.bind("uid="+info.getEmail()+","+userDN,null,attrs);
        ctx.close();
        return info.getEmail();
    }

    private Attributes getLDAPAttributes(RegisterInformation info) throws Exception {
        Attributes attrs = new BasicAttributes(true);
        Attribute objclass = new BasicAttribute("objectClass");
        objclass.add("inetOrgPerson");
        objclass.add("person");
        objclass.add("top");
        attrs.put(objclass);
        attrs.put("uid",info.getEmail());
        attrs.put("cn",info.getEmail());
        attrs.put("mail",info.getEmail());
        if(info.getFirstName()!=null && !info.getFirstName().isEmpty())
            attrs.put("givenName",info.getFirstName());
        else
            attrs.put("givenName","");
        if(info.getLastName()!=null && !info.getLastName().isEmpty())
            attrs.put("sn",info.getLastName());
        else
            attrs.put("sn","");
        attrs.put("userPassword",convertPassword(info.getPassword()));
        if(info.getOrganization()!=null && !info.getOrganization().isEmpty())
            attrs.put("o",info.getOrganization());
        return attrs;
    }

    @Override
    protected void setPassword(RegisterInformation info, String newPassword) throws Exception{
        LdapContext ctx = getLdapContext();
        String userDN = RegisterServiceFactory.getConfig().getProperty("ldap.baseDN");
        Attributes attrs = getLDAPAttributes(info);
        attrs.put("userPassword",convertPassword(newPassword));
        ctx.rebind("uid="+info.getEmail()+","+userDN,null,attrs);
    }

    private String convertPassword(String password) throws Exception {
        if(password==null)
            return null;
        String algorithm = RegisterServiceFactory.getConfig().getProperty("ldap.passwordAlgorithm");
        if("md5".equalsIgnoreCase(algorithm)){
            return "{MD5}"+Base64.getEncoder().encodeToString(DigestUtils.md5(password));
        }
        if("none".equalsIgnoreCase(algorithm)){
            return password;
        }
        throw new InvalidParameterException("Unknown ldap password algorithm: "+algorithm+". Set a valid option for ldap.passwordAlgorithm");
    }

    private LdapContext getLdapContext() throws Exception {
        Properties config = RegisterServiceFactory.getConfig();

        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.SECURITY_AUTHENTICATION, config.getProperty("ldap.authentication"));
        if(config.getProperty("ldap.username") != null) {
            env.put(Context.SECURITY_PRINCIPAL, config.getProperty("ldap.username"));
        }
        if(config.getProperty("ldap.password") != null) {
            env.put(Context.SECURITY_CREDENTIALS, config.getProperty("ldap.password"));
        }
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getProperty("ldap.server"));

        return new InitialLdapContext(env,null);
    }
}
