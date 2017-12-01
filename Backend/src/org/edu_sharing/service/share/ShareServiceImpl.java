package org.edu_sharing.service.share;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.validator.EmailValidator;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.policy.ScopeNodeWrongScopeException;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.DateTool;
import org.edu_sharing.repository.server.tools.I18nServer;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.edu_sharing.repository.server.tools.Mail;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.service.Constants;
import org.springframework.context.ApplicationContext;

public class ShareServiceImpl implements ShareService {
	
	public static final QName SHARE_TYPE = QName.createQName(CCConstants.CCM_TYPE_SHARE);
	public static final QName SHARE_ASSOC = QName.createQName(CCConstants.CCM_ASSOC_ASSIGNED_SHARES);
	
	public static final QName SHARES_ASPECT = QName.createQName(CCConstants.CCM_ASPECT_SHARES);
	
	public static final QName SHARE_PROP_EXPIRYDATE = QName.createQName(CCConstants.CCM_PROP_SHARE_EXPIRYDATE);
	public static final QName SHARE_PROP_SHARE_MAIL = QName.createQName(CCConstants.CCM_PROP_SHARE_MAIL);
	public static final QName SHARE_PROP_SHARE_TOKEN = QName.createQName(CCConstants.CCM_PROP_SHARE_TOKEN);
	public static final QName SHARE_PROP_DOWNLOAD_COUNTER = QName.createQName(CCConstants.CCM_PROP_SHARE_DOWNLOAD_COUNTER);
	
	public static String I18n_MailSubject = "dialog_share_mailsubject_file";
	public static String I18n_MailSubjectLink = "dialog_share_mailsubject_link";
	public static String I18n_MailText = "dialog_share_mailtext_file";
	public static String I18n_MailTextLink = "dialog_share_mailtext_link";
	
	ServiceRegistry serviceRegistry= null;
	
	Logger logger = Logger.getLogger(ShareServiceImpl.class);
	
	public ShareServiceImpl() {
		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	}
	
	public Share createShare(String nodeId, long expiryDate) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException, NodeDoesNotExsistException, PermissionFailedException{
			return getShare(nodeId, createShare(nodeId,new String[]{EMAIL_TYPE_LINK},expiryDate,null));
	}
	
	@Override
	public String createShare(String nodeId, String[] emails, long expiryDate, String emailMessageLocale) throws EMailValidationException, EMailSendFailedException, ExpiryDateValidationException,
			NodeDoesNotExsistException, PermissionFailedException {
		
		NodeService nodeService = serviceRegistry.getNodeService();
		AuthenticationService authService = serviceRegistry.getAuthenticationService();
		//email validation
		EmailValidator mailValidator = EmailValidator.getInstance();
		for(String email : emails){
			if(!EMAIL_TYPE_LINK.equals(email) && !mailValidator.isValid(email)){
				throw new EMailValidationException("EMail: " + email + " is invalid!");
			}
		}
		
		//expiry date validation
		if(expiryDate != EXPIRY_DATE_UNLIMITED && new Date(expiryDate).before(new Date(System.currentTimeMillis()))){
			throw new ExpiryDateValidationException("Expiry Date "+ new Date(expiryDate) +  " is to old!");
		}
		
		//parent validation
		NodeRef io = new NodeRef(Constants.storeRef,nodeId);
		if(!nodeService.exists(io)){
			throw new NodeDoesNotExsistException();
		}
		throwIfScopedNode(io);
		
		//license/permission validation
		String globalLicense = null;
		ArrayList<String> globalLicenses = (ArrayList<String>)nodeService.getProperty(io, QName.createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY));
		if(globalLicenses != null){
			globalLicense = globalLicenses.get(0);
		}
		
		boolean isLink = false;
		String wwwUrl = (String)nodeService.getProperty(io, QName.createQName(CCConstants.CCM_PROP_IO_WWWURL));
		if(wwwUrl != null){
			isLink = true;
		}
		
		NodeRef personNodeRef = (NodeRef)serviceRegistry.getPersonService().getPerson(authService.getCurrentUserName());
		Map<QName, Serializable> personProps = serviceRegistry.getNodeService().getProperties(personNodeRef);
		String nameInvitor = (String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_FIRSTNAME))+" "+(String)personProps.get(QName.createQName(CCConstants.CM_PROP_PERSON_LASTNAME));
		
		//shares is no mandatory aspect so:
		if(!nodeService.hasAspect(io, SHARES_ASPECT)){
			nodeService.addAspect(io, SHARES_ASPECT, null);
		}
		String token=null;
		for(String email : emails){
			
			Map<QName, Serializable> props = new HashMap<QName, Serializable>();
			
			props.put(SHARE_PROP_EXPIRYDATE, expiryDate);
			props.put(SHARE_PROP_SHARE_MAIL, email);
			
			token = new KeyTool().getKey();
			
			props.put(SHARE_PROP_SHARE_TOKEN, token);
			props.put(SHARE_PROP_DOWNLOAD_COUNTER, 0);
			
			ChildAssociationRef createdChild = nodeService.createNode(io, SHARE_ASSOC, QName.createQName(email), SHARE_TYPE, props);
			
			if(!EMAIL_TYPE_LINK.equals(email)){
				Mail mail = new Mail();
				String subject = (isLink)?  I18nServer.getTranslationDefaultResourcebundle(I18n_MailSubjectLink, emailMessageLocale) : I18nServer.getTranslationDefaultResourcebundle(I18n_MailSubject, emailMessageLocale);
				
				subject = subject.replace("{user}",nameInvitor);
				
				String message = (isLink) ? I18nServer.getTranslationDefaultResourcebundle(I18n_MailTextLink, emailMessageLocale) : I18nServer.getTranslationDefaultResourcebundle(I18n_MailText, emailMessageLocale);
				
				message = message.replace("{link}", URLTool.getShareServletUrl(io, token));
				message = message.replace("{user}", nameInvitor);
				
				String expiryDateString = null;
				if(expiryDate == -1){
					expiryDateString = I18nServer.getTranslationDefaultResourcebundle("dialog_share_expiry_unlimited", emailMessageLocale);
				}else{
					expiryDateString = new DateTool().formatDate(expiryDate,DateFormat.LONG,null);
				}
				
				message = message.replace("{expiry}", expiryDateString);
				try{
					mail.sendMail(email, subject, message);
				}catch(Exception e){
					
					//rollback
					nodeService.deleteNode(createdChild.getChildRef());
					logger.error(e.getMessage(), e);
					throw new EMailSendFailedException(e.getMessage());
				}
			}
		}
		return token;
	}
	
	private void throwIfScopedNode(NodeRef io) {
		if(serviceRegistry.getNodeService().getProperty(io, QName.createQName(CCConstants.CCM_PROP_SCOPE_TYPE))!=null){
			throw new ScopeNodeWrongScopeException("Not allowed to share a scoped node!");
		}		
	}
	@Override
	public void removeShare(String shareNodeId){
		serviceRegistry.getNodeService().deleteNode(new NodeRef(Constants.storeRef,shareNodeId));
	}
	
	@Override
	public void updateShare(String nodeId, String email, long expiryDate) throws EMailValidationException, ExpiryDateValidationException,
			NodeDoesNotExsistException, PermissionFailedException {
	}
	
	@Override
	public void updateShare(Share share) {
		Map<QName,Serializable> props = new HashMap<QName,Serializable>();
		props.put(SHARE_PROP_SHARE_TOKEN, share.getToken());
		props.put(SHARE_PROP_EXPIRYDATE, share.getExpiryDate());
		props.put(SHARE_PROP_SHARE_MAIL, share.getEmail());
		props.put(SHARE_PROP_DOWNLOAD_COUNTER, share.getDownloadCount());
		throwIfScopedNode(new NodeRef(Constants.storeRef,share.getIoNodeId()));
		serviceRegistry.getNodeService().setProperties(new NodeRef(Constants.storeRef, share.getNodeId()), props);
	}
	
	@Override
	public Share[] getShares(String nodeId) {
		
		Set<QName> qnameSet = new HashSet<QName>();
		qnameSet.add(SHARE_TYPE);
		List<ChildAssociationRef> childNodeRefs = serviceRegistry.getNodeService().getChildAssocs(new NodeRef(Constants.storeRef,nodeId),qnameSet);
		
		List<Share> result = new ArrayList<Share>();
		for(ChildAssociationRef childRef : childNodeRefs){
			Map<QName,Serializable> props =  serviceRegistry.getNodeService().getProperties(childRef.getChildRef());
			Share share = new Share();
			share.setIoNodeId(nodeId);
			share.setNodeId(childRef.getChildRef().getId());
			share.setEmail((String)props.get(SHARE_PROP_SHARE_MAIL));
			share.setExpiryDate((Long)props.get(SHARE_PROP_EXPIRYDATE));
			share.setDownloadCount((Integer)props.get(SHARE_PROP_DOWNLOAD_COUNTER));
			
			if(share.getExpiryDate() != null && share.getExpiryDate() != -1){
				share.setExpiryDateFormated(new DateTool().formatDate(share.getExpiryDate(),DateFormat.LONG,null));
			}
			
			share.setToken((String)props.get(SHARE_PROP_SHARE_TOKEN));
			String creator = (String)props.get(QName.createQName(CCConstants.CM_PROP_C_CREATOR));
			share.setInvitedBy(creator);
			NodeRef personNodeRef = serviceRegistry.getPersonService().getPerson(creator);
			String firstName = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_FIRSTNAME);
			String lastName = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_LASTNAME);
			String email = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_EMAIL);
			String invitedByCaption = firstName + " " +lastName+" (" +email+")";
			share.setInvitedByCaption(invitedByCaption);
			
			share.setInvitedAt((Date)props.get(QName.createQName(CCConstants.CM_PROP_C_CREATED)));
			
			if(share.getInvitedAt() != null){
				share.setInvitedAtFormated(new DateTool().formatDate(share.getInvitedAt().getTime(),DateFormat.LONG,null));
			}
			
			result.add(share);
		}
		
		return result.toArray(new Share[result.size()]);
	}
	
	
	public Share getShare(String nodeId, String token){
		List<ChildAssociationRef> childAssocRefs = serviceRegistry.getNodeService().getChildAssocsByPropertyValue(new NodeRef(Constants.storeRef,nodeId), SHARE_PROP_SHARE_TOKEN,token);
		if(childAssocRefs.size() == 1){
			NodeRef shareNodeRef = childAssocRefs.get(0).getChildRef();
			Map<QName,Serializable> props =  serviceRegistry.getNodeService().getProperties(shareNodeRef);
			Share share = new Share();
			share.setIoNodeId(nodeId);
			share.setNodeId(shareNodeRef.getId());
			share.setEmail((String)props.get(SHARE_PROP_SHARE_MAIL));
			share.setExpiryDate((Long)props.get(SHARE_PROP_EXPIRYDATE));
			share.setDownloadCount((Integer)props.get(SHARE_PROP_DOWNLOAD_COUNTER));
			
			if(share.getExpiryDate() != null && share.getExpiryDate() != -1){
				share.setExpiryDateFormated(new DateTool().formatDate(share.getExpiryDate(),DateFormat.LONG,null));
			}
			
			share.setToken((String)props.get(SHARE_PROP_SHARE_TOKEN));
			
			String creator = (String)props.get(QName.createQName(CCConstants.CM_PROP_C_CREATOR));
			share.setInvitedBy(creator);
			NodeRef personNodeRef = serviceRegistry.getPersonService().getPerson(creator);
			String firstName = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_FIRSTNAME);
			String lastName = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_LASTNAME);
			String email = (String)serviceRegistry.getNodeService().getProperty(personNodeRef, ContentModel.PROP_EMAIL);
			String invitedByCaption = firstName + " " +lastName+" (" +email+")";
			share.setInvitedByCaption(invitedByCaption);
			
			share.setInvitedAt((Date)props.get(QName.createQName(CCConstants.CM_PROP_C_CREATED)));		
			
			if(share.getInvitedAt() != null){
				share.setInvitedAtFormated(new DateTool().formatDate(share.getInvitedAt().getTime(),DateFormat.LONG,null));
			}
			
			return share;
			
		}else{
			logger.error("invalid number of share objects for io:"+childAssocRefs.size()+" "+nodeId);
			return null;
		}
	}
}
