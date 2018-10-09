package org.edu_sharing.service.notification;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.impl.acegi.FilteringResultSet;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ISO9075;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataReaderV2;
import org.edu_sharing.repository.client.rpc.ACE;
import org.edu_sharing.repository.client.rpc.ACL;
import org.edu_sharing.repository.client.rpc.Authority;
import org.edu_sharing.repository.client.rpc.Group;
import org.edu_sharing.repository.client.rpc.Notify;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.Edu_SharingProperties;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.StringTool;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.edu_sharing.repository.server.tools.mailtemplates.MailTemplate;
import org.edu_sharing.service.Constants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionService;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;
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
