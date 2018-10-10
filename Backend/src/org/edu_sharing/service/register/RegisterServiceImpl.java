package org.edu_sharing.service.register;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.NoSuchPersonException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.RandomStringUtils;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import java.io.Serializable;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

public class RegisterServiceImpl implements RegisterService {
    private static int KEY_LENGTH = 16;
    public final static SimpleCache<String,RegisterInformation> registerUserCache = (SimpleCache)  AlfAppContextGate.getApplicationContext().getBean("eduSharingRegisterUserCache");
    public final static SimpleCache<String,String> recoverPasswordCache = (SimpleCache)  AlfAppContextGate.getApplicationContext().getBean("eduSharingRecoverPasswordCache");

    private final PersonService personService;
    private final NodeService nodeService;
    private final MutableAuthenticationService authService;

    public RegisterServiceImpl() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext
                .getBean(ServiceRegistry.SERVICE_REGISTRY);

        personService = serviceRegistry.getPersonService();
        nodeService = serviceRegistry.getNodeService();
        authService = serviceRegistry.getAuthenticationService();
    }
    protected void sendRegisterMail(RegisterInformation info, String key) throws Exception {
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        String subject=MailTemplate.getSubject("userRegister", currentLocale);
        String content=MailTemplate.getContent("userRegister", currentLocale,true);
        Map<String,String> replace=new HashMap<>();
        replace.put("link",URLTool.getNgComponentsUrl()+"register/done/"+URLEncoder.encode(key)+"/"+ URLEncoder.encode(info.getEmail()));
        replace.put("key",key);
        addMailRegisterInfo(info, replace);
        Mail mail=new Mail();
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        mail.sendMailHtml(
                context,
                info.getEmail(),
                subject,content,replace);

    }

    private void addMailRegisterInfo(RegisterInformation info, Map<String, String> replace) {
        replace.put("firstName",info.getFirstName());
        replace.put("lastName",info.getLastName());
        replace.put("email",info.getEmail());
        replace.put("organization",info.getOrganization());
    }

    public boolean userExists(String id) throws Exception {
        try {
            return personService.personExists(id);
        }catch(AuthenticationCredentialsNotFoundException e){
            return false;
        }
    }
    public boolean userExists(RegisterInformation info) throws Exception {
        return userExists(info.getEmail());
    }

    @Override
    public boolean recoverPassword(String id) throws Exception {
        return AuthenticationUtil.runAsSystem(()-> {
            try {
                RegisterInformation info = getPersonById(id);
                String key = addToCacheNoDuplicate(info,recoverPasswordCache);

                String currentLocale = new AuthenticationToolAPI().getCurrentLocale();
                String subject = MailTemplate.getSubject("userRecoverPassword", currentLocale);
                String content = MailTemplate.getContent("userRecoverPassword", currentLocale, true);
                Map<String, String> replace = new HashMap<>();
                replace.put("link", URLTool.getNgComponentsUrl() + "register/reset-password/" + URLEncoder.encode(key));
                replace.put("key", key);
                addMailRegisterInfo(info, replace);
                Mail mail = new Mail();
                ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
                mail.sendMailHtml(
                        context,
                        info.getEmail(),
                        subject, content, replace);
                return true;
            } catch (NoSuchPersonException|AuthenticationCredentialsNotFoundException e) {
                return false;
            }
        });
    }

    private RegisterInformation getPersonById(String id) {
        NodeRef ref = personService.getPerson(id,false);
        Map<QName, Serializable> props = nodeService.getProperties(ref);
        RegisterInformation info = new RegisterInformation();
        info.setFirstName((String) props.get(ContentModel.PROP_FIRSTNAME));
        info.setLastName((String) props.get(ContentModel.PROP_LASTNAME));
        info.setEmail((String) props.get(ContentModel.PROP_EMAIL));
        info.setOrganization((String) props.get(ContentModel.PROP_ORGANIZATION));
        if(props.containsKey(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS)))
            info.setAllowNotifications((Boolean) props.get(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS)));
        return info;
    }

    @Override
    public String activate(String key) throws Throwable {
        RegisterInformation info = registerUserCache.get(key);
        if(info==null)
            throw new InvalidKeyException();
        String result = storeUser(info);
        registerUserCache.remove(key);
        // authenticate the newly activated user automatically
        authenticate(info);
        return result;

    }

    private void authenticate(RegisterInformation info) throws Exception {
        AuthenticationTool authTool=new AuthenticationToolAPI();
        HashMap<String, String> validatedAuth = authTool.createNewSession(info.getEmail(), info.getPassword());
        authTool.storeAuthInfoInSession(info.getEmail(), validatedAuth.get(CCConstants.AUTH_TICKET),CCConstants.AUTH_TYPE_DEFAULT, Context.getCurrentInstance().getRequest().getSession(true));
        authService.authenticate(info.getEmail(),info.getPassword().toCharArray());
    }

    protected String storeUser(RegisterInformation info) throws Exception {
        return AuthenticationUtil.runAsSystem(()-> {
            Map<QName, Serializable> map = new HashMap<>();
            String authority = info.getEmail();
            map.put(ContentModel.PROP_USERNAME, authority);
            map.put(ContentModel.PROP_FIRSTNAME, info.getFirstName());
            map.put(ContentModel.PROP_LASTNAME, info.getLastName());
            map.put(ContentModel.PROP_ORGANIZATION, info.getOrganization());
            map.put(ContentModel.PROP_EMAIL, info.getEmail());
            map.put(QName.createQName(CCConstants.CM_PROP_PERSON_ALLOW_NOTIFICATIONS), info.isAllowNotifications());
            try {
                authService.createAuthentication(authority, info.getPassword().toCharArray());
                NodeRef result = personService.createPerson(map);
                return result.getId();
            } catch (Throwable t) {
                // check for duplicates
                throw t;
            }
        });
    }
    @Override
    public void register(RegisterInformation info) throws DuplicateAuthorityException, Throwable{
       if(userExists(info))
           throw new DuplicateAuthorityException();
        String value = addToCacheNoDuplicate(info,registerUserCache);
        sendRegisterMail(info,value);
    }
    @Override
    public boolean resendRegisterMail(String mail) throws Exception {
        String key=getKeyForMail(mail);
        if(key!=null){
            sendRegisterMail(registerUserCache.get(key),key);
            return true;
        }
        return false;
    }
    private String getKeyForMail(String mail){
        for (String cacheKey : registerUserCache.getKeys()) {
            try {
                if (registerUserCache.get(cacheKey).getEmail().equals(mail))
                    return cacheKey;
            }catch(Throwable t){
                // it's possible to get class cast exceptions when hot deploying
            }
        }
        return null;
    }
    private String addToCacheNoDuplicate(RegisterInformation info,SimpleCache cache) {
        String existing=getKeyForMail(info.getEmail());
        if(existing!=null)
            return existing;
        return addToCache(info, cache);
    }

    private String addToCache(Object data, SimpleCache cache) {
        while(true){
            String id=RandomStringUtils.random(KEY_LENGTH,true,true);
            if(cache.contains(id))
                continue;
            cache.put(id,data);
            return id;
        }
    }
}
