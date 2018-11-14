package org.edu_sharing.alfresco.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.workspace_administration.NodeServiceInterceptor;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;

public class OrganisationService {

	AuthorityService eduAuthorityService;

	NodeService nodeService;

	org.alfresco.service.cmr.security.AuthorityService authorityService;

	Repository repositoryHelper;

	PermissionService permissionService;

	public static String ORGANIZATION_GROUP_FOLDER = "EDU_SHARED";

	public static final String CCM_PROP_EDUGROUP_EDU_HOMEDIR = "{http://www.campuscontent.de/model/1.0}edu_homedir";
	public static final String CCM_PROP_EDUGROUP_EDU_UNIQUENAME = "{http://www.campuscontent.de/model/1.0}edu_uniquename";
	public static final QName QNAME_EDUGROUP = QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP);

	Logger logger = Logger.getLogger(OrganisationService.class);

	public String createOrganization(String orgName, String groupDisplayName) throws Exception {
		return createOrganization(orgName, groupDisplayName, null, null);
	}

	public String createOrganization(String orgName, String groupDisplayName,String metadataset, String scope) throws Exception {
		orgName+=(scope==null || scope.isEmpty() ? "" : "_"+scope);
        groupDisplayName+=(scope==null || scope.isEmpty() ? "" : "_"+scope);
        String groupName = eduAuthorityService.createOrUpdateGroup(AuthorityService.ORG_GROUP_PREFIX + orgName, groupDisplayName, null, true);
		
		String authorityAdmins = eduAuthorityService.createOrUpdateGroup(AuthorityService.ADMINISTRATORS_GROUP, groupDisplayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX, groupName, true);

		addAspect(PermissionService.GROUP_PREFIX + authorityAdmins, CCConstants.CCM_ASPECT_GROUPEXTENSION);

		nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + authorityAdmins), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE),
				AuthorityService.ADMINISTRATORS_GROUP_TYPE);

		NodeRef companyHome = repositoryHelper.getCompanyHome();

		NodeRef shared = findNodeByName(companyHome, ORGANIZATION_GROUP_FOLDER);

		if (shared == null) {
			shared = createNode(companyHome, CCConstants.CCM_TYPE_MAP, ORGANIZATION_GROUP_FOLDER);
			permissionService.setInheritParentPermissions(shared, false);
		}

		String orgFolderName = (groupDisplayName != null && !groupDisplayName.trim().isEmpty()) ? groupDisplayName : orgName;
		NodeRef orgFolder = createNode(shared, CCConstants.CCM_TYPE_MAP, orgFolderName);
		if(metadataset != null) {
			nodeService.setProperty(orgFolder, QName.createQName(CCConstants.CM_PROP_METADATASET_EDU_METADATASET), metadataset.trim());
		}

		bindEduGroupFolder(groupName, orgFolder);

		permissionService.setPermission(orgFolder, PermissionService.GROUP_PREFIX + groupName, PermissionService.CONSUMER, true);
		permissionService.setPermission(orgFolder, PermissionService.GROUP_PREFIX + authorityAdmins, PermissionService.COORDINATOR, true);

		if(scope!=null && !scope.isEmpty()){
			nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName), QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
					CCConstants.CCM_VALUE_SCOPE_SAFE);
			nodeService.setProperty(authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + authorityAdmins), QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
					CCConstants.CCM_VALUE_SCOPE_SAFE);
			nodeService.setProperty(orgFolder, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME),
					CCConstants.CCM_VALUE_SCOPE_SAFE);
		}

		return groupName;
	}
	
	/**
	 * 
	 * @param orgName without GROUP_ AND ORG_ PREFIX
	 */
	public Map<QName, Serializable> getOrganisation(String orgName) {
		for(NodeRef nodeRef : EduGroupCache.getKeys()) {
			Map<QName, Serializable> props = EduGroupCache.get(nodeRef);
			String tmpOrgName = getCleanName((String)props.get(ContentModel.PROP_AUTHORITY_NAME));
			if(orgName.equals(tmpOrgName)) {
				return props;
			}
		}
		
		return null;
	}

	public String getCleanName(String fullOrgName) {
		String tmpOrgName = new String(fullOrgName);
		tmpOrgName = tmpOrgName.replace(AuthorityType.GROUP.getPrefixString(),"");
		tmpOrgName = tmpOrgName.replace(AuthorityService.ORG_GROUP_PREFIX, "");
		return tmpOrgName;
	}
	
	private void addAspect(String authority, String aspect) {
		nodeService.addAspect(authorityService.getAuthorityNodeRef(authority), QName.createQName(aspect), new HashMap<>());
	}

	public void bindEduGroupFolder(String groupName, NodeRef folder) throws Exception {

		NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(PermissionService.GROUP_PREFIX + groupName);

		if (authorityNodeRef == null) {
			throw new Exception("Group does not exist");
		}

		if (!nodeService.exists(folder)) {
			throw new Exception("Folder does not exist");
		}

		Map<QName, Serializable> params = new HashMap<QName, Serializable>();
		params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_HOMEDIR), folder);
		params.put(QName.createQName(CCM_PROP_EDUGROUP_EDU_UNIQUENAME), groupName);

		nodeService.addAspect(authorityNodeRef, QNAME_EDUGROUP, params);
	}

	private NodeRef createNode(NodeRef parent, String type, String name) {
		Map<QName, Serializable> propsOrgFolder = new HashMap<QName, Serializable>();
		propsOrgFolder.put(ContentModel.PROP_NAME, name);
		String assocName = "{" + CCConstants.NAMESPACE_CCM + "}" + name;
		return nodeService.createNode(parent, ContentModel.ASSOC_CONTAINS, QName.createQName(assocName), QName.createQName(type), propsOrgFolder).getChildRef();
	}

	private NodeRef findNodeByName(NodeRef parent, String name) {
		/**
		 * getChildAssocsByPropertyValue does not allow search of system
		 * maintained properties List<ChildAssociationRef> children =
		 * nodeService.getChildAssocsByPropertyValue( new
		 * NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,parentId),
		 * QName.createQName(CCConstants.CM_NAME), name);
		 */
		List<ChildAssociationRef> children = nodeService.getChildAssocs(parent);
		for (ChildAssociationRef child : children) {
			String childName = (String) nodeService.getProperty(child.getChildRef(), QName.createQName(CCConstants.CM_NAME));
			if (childName.equals(name))
				return child.getChildRef();
		}
		return null;
	}
	

	/**
	 *
	 * @param organisationName
	 * @return Organisation admin Group
	 */
	public String getOrganisationAdminGroup(String organisationName) {
		String authorityName = getAuthorityName(organisationName);

		NodeRef eduGroupNodeRef =authorityService.getAuthorityNodeRef(authorityName);
		List<ChildAssociationRef> childGroups = nodeService.getChildAssocs(eduGroupNodeRef);
		for(ChildAssociationRef childGroup : childGroups){
			String grouptype = (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
			if(CCConstants.ADMINISTRATORS_GROUP_TYPE.equals(grouptype)){
				return (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
			}
		}

		return null;
	}

	/**
	 * runs over organisation homefolder recursively and
	 * adds ORG_ADMIN Group as Coordinator if not already set
	 * @param organisationName
	 */
	public void setOrgAdminPermissions(String organisationName) {
		logger.debug("inviting orgadmin group as coordinator for org:" + organisationName);
		String authorityName = getAuthorityName(organisationName);
		NodeRef orgNodeRef = authorityService.getAuthorityNodeRef(authorityName);
		NodeRef eduGroupHomeDir = (NodeRef)nodeService.getProperty(orgNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
		if(eduGroupHomeDir == null) {
			logger.debug(organisationName + " is no organisation");
			return;
		}

		setOrgAdminPermissions(eduGroupHomeDir, getOrganisationAdminGroup(organisationName));

	}

	private void setOrgAdminPermissions(NodeRef parent, String adminAuthority) {
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef childRef : childAssocs) {

			Set<AccessPermission> allSetPerms = permissionService.getAllSetPermissions(childRef.getChildRef());

			boolean isAlreadySet = false;
			for(AccessPermission perm : allSetPerms) {
				if(perm.getAuthority().equals(adminAuthority)
						&& perm.getPermission().equals(PermissionService.COORDINATOR)) {
					isAlreadySet = true;
				}
			}
			if(!isAlreadySet) {
				logger.debug("will set org admingroup as Coordnator for:" + childRef.getChildRef() );
				permissionService.setPermission(childRef.getChildRef(), adminAuthority, PermissionService.COORDINATOR, true);
			}

			if(nodeService.getType(childRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))) {
				for(ChildAssociationRef mapChildRef : nodeService.getChildAssocs(childRef.getChildRef())){
					setOrgAdminPermissions(mapChildRef.getChildRef(), adminAuthority);
				}
			}
		}
	}

	/**
	 * GROUP_ and ORG_ Prefixes are added if they ar not present
	 * @param organisationName
	 * @return
	 */
	public String getAuthorityName(String organisationName) {
		organisationName = organisationName.replaceFirst(PermissionService.GROUP_PREFIX, "");
		organisationName = organisationName.replaceFirst(AuthorityService.ORG_GROUP_PREFIX, "");

		return  PermissionService.GROUP_PREFIX + AuthorityService.ORG_GROUP_PREFIX + organisationName;
	}

	public List<String> getMyOrganisations(boolean scoped){
		Set<String> authorities = authorityService.getContainingAuthorities(AuthorityType.GROUP, AuthenticationUtil.getFullyAuthenticatedUser(), true);
		List<String> organisations = new ArrayList<String>();
		for (String authority : authorities) {
			NodeRef nodeRefAuthority = authorityService.getAuthorityNodeRef(authority);
			if (nodeService.hasAspect(nodeRefAuthority, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
				
				String eduGroupScope = (String)nodeService.getProperty(nodeRefAuthority, QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME));
				
				boolean add = false;
				if(authorities.contains(CCConstants.AUTHORITY_GROUP_ALFRESCO_ADMINISTRATORS) 
						|| authorities.contains(authority)) {
					add = true;
				}
				
				if(scoped) {
					String currentScope = NodeServiceInterceptor.getEduSharingScope();
					if(eduGroupScope == null && currentScope != null) {
						add=false;
					}
					if(eduGroupScope != null && !eduGroupScope.equals(currentScope)) {
						add=false;
					}
						
				}
				
				if (add) {
					organisations.add(authority);
				}
			}	
		}
		return organisations;
	}

	public void setEduAuthorityService(AuthorityService eduAuthorityService) {
		this.eduAuthorityService = eduAuthorityService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setAuthorityService(org.alfresco.service.cmr.security.AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setRepositoryHelper(Repository repositoryHelper) {
		this.repositoryHelper = repositoryHelper;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
}
