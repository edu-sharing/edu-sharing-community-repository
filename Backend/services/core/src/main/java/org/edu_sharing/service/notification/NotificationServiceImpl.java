package org.edu_sharing.service.notification;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.springframework.context.ApplicationContext;

import com.sun.star.lang.IllegalArgumentException;

public class NotificationServiceImpl implements NotificationService {

	private ApplicationInfo appInfo;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	AuthorityService authorityService = serviceRegistry.getAuthorityService();
	MCAlfrescoAPIClient repoClient = new MCAlfrescoAPIClient(); 
	Logger logger = Logger.getLogger(NotificationServiceImpl.class);
	private PermissionService permissionService;

	private org.edu_sharing.service.nodeservice.NodeService nodeService;

	private String currentLocale;
	
	public NotificationServiceImpl(String appId){
		appInfo = ApplicationInfoList.getHomeRepository();
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
		nodeService=NodeServiceFactory.getNodeService(appId);
		currentLocale = new AuthenticationToolAPI().getCurrentLocale();
	}

	@Override
	public void notifyNodeIssue(String nodeId, String reason, String userEmail, String userComment) throws Throwable {
		HashMap<String, Object> properties = nodeService.getProperties(StoreRef.PROTOCOL_WORKSPACE, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId);
		String subject=MailTemplate.getSubject("nodeIssue", currentLocale);
		String content=MailTemplate.getContent("nodeIssue", currentLocale,true);
		Map<String,String> replace=new HashMap<>();
		replace.put("reporterEmail", userEmail.trim());
		replace.put("userComment", userComment);
		replace.put("reason", reason);
		replace.put("name", (String)properties.get(CCConstants.CM_NAME));
		replace.put("id", nodeId);
		replace.put("link", URLTool.getNgRenderNodeUrl(nodeId,null,true));
		Mail mail=new Mail();
		String receiver=(String)mail.getProperties().get("mail.report.receiver");
		if(receiver==null)
			throw new IllegalArgumentException("no mail.report.receiver registered in ccmail.properties");
		ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
		mail.sendMailHtml(
				context,
				receiver,
				subject,content,replace);
	}
}
