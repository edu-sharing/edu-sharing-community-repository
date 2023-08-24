package org.edu_sharing.service.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.EduGroup;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchResult;
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
		replace.put("link.static", URLTool.getNgRenderNodeUrl(nodeId,null,false));
		MailTemplate.applyNodePropertiesToMap("node.", properties, replace);
		try {
			HashMap<String, Object> userProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(AuthenticationUtil.getFullyAuthenticatedUser()));
			MailTemplate.applyNodePropertiesToMap("user.", userProps, replace);
			SearchResult<EduGroup> orgList = SearchServiceFactory.getLocalService().getAllOrganizations(true);
			if(!orgList.getData().isEmpty()) {
				HashMap<String, Object> orgProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(orgList.getData().get(0).getGroupname()));
				MailTemplate.applyNodePropertiesToMap("user.organization.", orgProps, replace);
			}
			List<String> mzList = SearchServiceFactory.getLocalService().getAllMediacenters();
			if(!mzList.isEmpty()) {
				HashMap<String, Object> mzProps = NodeServiceHelper.getProperties(AuthorityServiceFactory.getLocalService().getAuthorityNodeRef(mzList.get(0)));
				MailTemplate.applyNodePropertiesToMap("user.mediacenter.", mzProps, replace);
			}
		} catch (Throwable ignored) {

		}
		Mail mail=new Mail();
		List<String> receivers = null;
		if(mail.getConfig().hasPath("report.receivers")) {
			receivers = mail.getConfig().getStringList("report.receivers");
		} else if (mail.getConfig().getString("report.receiver") != null){
			receivers = Collections.singletonList(mail.getConfig().getString("report.receiver"));
			logger.info("report.receiver is deprecated. Prefer using the report.receivers field instead");
		}
		if(receivers==null || receivers.isEmpty()) {
			throw new IllegalArgumentException("no mail.report.receivers registered in ccmail.properties");
		}
		ServletContext context = Context.getCurrentInstance().getRequest().getSession().getServletContext();
		for (String receiver : receivers) {
			mail.sendMailHtml(
					context,
					null,
					userEmail,
					receiver,
					subject, content, replace);
		}
	}
}
