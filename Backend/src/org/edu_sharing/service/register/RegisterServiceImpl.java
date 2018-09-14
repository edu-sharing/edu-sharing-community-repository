package org.edu_sharing.service.register;

import com.sun.star.lang.IllegalArgumentException;
import io.swagger.models.auth.In;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.RandomStringUtils;
import org.edu_sharing.alfresco.authentication.subsystems.SubsystemChainingAuthenticationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.restservices.register.v1.model.RegisterInformation;
import org.edu_sharing.service.editlock.LockBy;
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

    private final PersonService personService;
    private final MutableAuthenticationService authService;

    public RegisterServiceImpl() {
        ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
        ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext
                .getBean(ServiceRegistry.SERVICE_REGISTRY);

        personService = serviceRegistry.getPersonService();
        authService = serviceRegistry.getAuthenticationService();
    }
    protected void sendRegisterMail(RegisterInformation info, String key) throws Exception {
        String currentLocale = new AuthenticationToolAPI().getCurrentLocale();

        String subject=MailTemplate.getSubject("userRegister", currentLocale);
        String content=MailTemplate.getContent("userRegister", currentLocale,true);
        Map<String,String> replace=new HashMap<>();
        replace.put("link",URLTool.getNgComponentsUrl()+"register/done/"+URLEncoder.encode(key)+"/"+ URLEncoder.encode(info.getEmail()));
        replace.put("key",key);
        replace.put("firstName",info.getFirstName());
        replace.put("lastName",info.getLastName());
        replace.put("email",info.getEmail());
        replace.put("organization",info.getOrganization());
        Mail mail=new Mail();
        ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
        mail.sendMailHtml(
                context,
                info.getEmail(),
                subject,content,replace);

    }
    public boolean userExists(RegisterInformation info){
        return personService.personExists(info.getEmail());
    }
    @Override
    public String activate(String key) throws InvalidKeyException,Throwable {
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

    protected String storeUser(RegisterInformation info){
        return AuthenticationUtil.runAsSystem(()-> {
            Map<QName, Serializable> map = new HashMap<>();
            String authority = info.getEmail();
            map.put(ContentModel.PROP_USERNAME, authority);
            map.put(ContentModel.PROP_FIRSTNAME, info.getFirstName());
            map.put(ContentModel.PROP_LASTNAME, info.getLastName());
            map.put(ContentModel.PROP_ORGANIZATION, info.getOrganization());
            map.put(ContentModel.PROP_EMAIL, info.getEmail());
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
        String value = addToCache(info);
        sendRegisterMail(info,value);

    }

    private String addToCache(RegisterInformation info) {
        while(true){
            String id=RandomStringUtils.random(KEY_LENGTH,true,true);
            if(registerUserCache.contains(id))
                continue;
            registerUserCache.put(id,info);
            return id;
        }
    }
}
