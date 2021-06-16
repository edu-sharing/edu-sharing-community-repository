package org.edu_sharing.alfresco.policy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies.OnContentUpdatePolicy;
import org.alfresco.repo.node.NodeServicePolicies.BeforeDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.BeforeUpdateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnDeleteNodePolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.thumbnail.ThumbnailService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.VCardTool;
import org.edu_sharing.repository.server.tools.ActionObserver;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.springframework.security.crypto.codec.Base64;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;


/**
 * 
 * @author rudi
 *	
 *important only for IO's
 *
 * onCreateNode:
 * - content -> IO
 * - folder -> Map
 * - LOM metadata:
 * * - title
 * * - technical location
 * * - contributer
 * 
 * onContentUpdate:
 * 
 * - wenn IO
 * - LOM technical size setzen
 * - nur wenn es neuer content ist Vorschaubild generieren
 * - resourcinfo action aufrufen
 * - Versionshistorie und version erstellen
 * * - nur wenn create_version gesetzt ist (default = true)
 * * - im create servlet wird dies abgestellt, da der upload Vorgang erst nach LOM metadaten Eingabe abgeschlossen ist
 * * - außerdem wird der Wert dieses properties von dem gui element create_version beim update gesteuert
 * 
 */
public class ScopePolicies implements BeforeDeleteNodePolicy, OnCreateNodePolicy,BeforeUpdateNodePolicy{
	
	static Logger logger = Logger.getLogger(ScopePolicies.class);

	
	ActionService actionService;
	
	AuthorityService authorityService;

	NodeService nodeService;
	
	VersionService versionService;
	
	PersonService personService;
	
	PolicyComponent policyComponent;
	
	ContentService contentService;
	
	PermissionService permissionService;
	
	LockService lockService;
	
	ThumbnailService thumbnailService;
	
	BehaviourFilter policyBehaviourFilter;
	
	
	
	public void init() {
		logger.debug("called!");
		//policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "beforeDeleteNode"));
		//policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "beforeDeleteNode"));
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "beforeDeleteNode"));
		policyComponent.bindClassBehaviour(BeforeDeleteNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeDeleteNode"));
		
		//policyComponent.bindClassBehaviour(BeforeUpdateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "beforeUpdateNode"));
		//policyComponent.bindClassBehaviour(BeforeUpdateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "beforeUpdateNode"));
		policyComponent.bindClassBehaviour(BeforeUpdateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "beforeUpdateNode"));
		policyComponent.bindClassBehaviour(BeforeUpdateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "beforeUpdateNode"));
		
		
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_CONTENT, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_THUMBNAIL, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, ContentModel.TYPE_FOLDER, new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_IO), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_MAP), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_NOTIFY), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_SAVED_SEARCH), new JavaBehaviour(this, "onCreateNode"));
		policyComponent.bindClassBehaviour(OnCreateNodePolicy.QNAME, QName.createQName(CCConstants.CCM_TYPE_TOOL_INSTANCE), new JavaBehaviour(this, "onCreateNode"));
		
	}
	public boolean isAdmin(String username) throws Exception {
		try {
			Set<String> testUsetAuthorities = authorityService.getAuthoritiesForUser(username);
			for (String testAuth : testUsetAuthorities) {
				if (testAuth.equals("GROUP_ALFRESCO_ADMINISTRATORS")) {
					return true;
				}
			}
		} catch (org.alfresco.repo.security.permissions.AccessDeniedException e) {
		}
		return false;
	}
	private void throwIfWrongScope(NodeRef nodeRef){
		String username = AuthenticationUtil.getRunAsUser();
		try {
			
			if(isAdmin(username) || username.equals(AuthenticationUtil.SYSTEM_USER_NAME))
				return;
		} catch (Exception e) {}
		
		String currentScope=NodeServiceInterceptor.getEduSharingScope();
			
		String scope = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
		
		if(scope==null){
			if(currentScope!=null)
				throw new ScopeNodeWrongScopeException("trying to modify unscoped node from within a scope");
		}
		else if(!scope.equals(currentScope)){
			throw new ScopeNodeWrongScopeException("trying to modify scoped node from wrong scope");
		}
	}
	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		throwIfWrongScope(nodeRef);
	}
	@Override
	public void beforeUpdateNode(NodeRef nodeRef) {
		QName type = nodeService.getType(nodeRef);
		logger.debug(type + " nodeRef:" + nodeRef);
		throwIfWrongScope(nodeRef);
	}
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		
		logger.debug("childAssocRef:" + childAssocRef +" nodeIdchild:" +  childAssocRef.getChildRef().getId());
		NodeRef eduNodeRef = childAssocRef.getChildRef();
		// Set the current scope of the user as a property to the created node
		String currentScope=NodeServiceInterceptor.getEduSharingScope();
		QName type = nodeService.getType(eduNodeRef);
		logger.debug(type);
		
		if(type.equals(ContentModel.TYPE_THUMBNAIL)){
			currentScope=(String) nodeService.getProperty(childAssocRef.getParentRef(),QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
		}else{
			String parentScope = (String)nodeService.getProperty(childAssocRef.getParentRef(),QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
			if((parentScope != null && !parentScope.equals(currentScope)) 
					|| (parentScope == null && currentScope != null)){
				
				if(!AuthenticationUtil.isRunAsUserTheSystemUser() && !AuthenticationUtil.getRunAsUser().equals(ApplicationInfoList.getHomeRepository().getUsername())){
					throw new ScopeNodeWrongScopeException("parentScope != currentScope");
				}
			}
		}
		
			
		/**
		 * disable policies for this node to prevent that beforeUpdateNode 
		 * checks the scope which will be there after update
		 */
		policyBehaviourFilter.disableBehaviour(eduNodeRef);
		// Add Aspect for thumbnails, content + folder
		if(!nodeService.hasAspect(eduNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE))){
			nodeService.addAspect(eduNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE),null);
		}
		nodeService.setProperty(eduNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), currentScope);
		policyBehaviourFilter.enableBehaviour(eduNodeRef);
		

	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}
	
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}
	
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}
	
	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
	
	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}
	
	public void setThumbnailService(ThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	public void setPolicyBehaviourFilter(BehaviourFilter policyBehaviourFilter) {
		this.policyBehaviourFilter = policyBehaviourFilter;
	}
}
