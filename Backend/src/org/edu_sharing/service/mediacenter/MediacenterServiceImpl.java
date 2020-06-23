package org.edu_sharing.service.mediacenter;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.AuthorityService;
import org.edu_sharing.alfresco.service.OrganisationService;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.shared.Mediacenter;
import org.edu_sharing.restservices.shared.Mediacenter.MediacenterProfileExtension;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.CMISSearchHelper;
import org.edu_sharing.service.toolpermission.ToolPermissionHelper;
import org.edu_sharing.service.util.CSVTool;
import org.springframework.context.ApplicationContext;

public class MediacenterServiceImpl implements MediacenterService{
	
	Logger logger = Logger.getLogger(MediacenterServiceImpl.class);
	
	ApplicationContext  applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceregistry = (ServiceRegistry)applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	org.alfresco.service.cmr.security.AuthorityService authorityService = serviceregistry.getAuthorityService();
	NodeService nodeService = serviceregistry.getNodeService();
	AuthorityService eduAuthorityService = (AuthorityService)applicationContext.getBean("eduAuthorityService");
	OrganisationService organisationService = (OrganisationService) applicationContext
			.getBean("eduOrganisationService");
	org.edu_sharing.service.authority.AuthorityService eduAuthorityService2 = AuthorityServiceFactory.getLocalService();
	SearchService searchService = serviceregistry.getSearchService();
	PermissionService permissionService = serviceregistry.getPermissionService();
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");


	@Override
	public int importMediacenters(InputStream csv) {
		RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
			@Override
			public Integer doWork() throws Exception {
				List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_ISO);

				int counter = 0;
				for (List<String> record : records) {

					String mzId = record.get(0);
					String mz = record.get(1);
					String plz = record.get(2);
					String ort = record.get(3);

					try {
						
						String authorityName = AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + mzId;
						logger.info("managing:" + authorityName);
						
						if(authorityService.authorityExists("GROUP_" + authorityName)) {
							logger.info("authority already exists:" + authorityName);
							updateMediacenter(authorityName,mz,plz,ort,null,null,null,true);
							continue;
						}
						
						createMediacenter(mzId,mz,plz,ort);
						
						
						counter++;
					} catch (Exception e) {
						logger.error("error in record: " + ((record == null || record.size() < 1)?null:record.get(0)), e);
						throw e;
					}
				}
				return counter;
			}
		};

		return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
	}

	public void updateMediacenter(String authorityName, String displayName, String postalCode, String city,
								  String districtAbbreviation, String mainUrl, String mediacenterCatalogs, boolean active) throws Exception {

		NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef( authorityName);
		String alfAuthorityName = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
		String currentDisplayName = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));

		if(displayName != null && !displayName.equals(currentDisplayName)) {
			authorityService.setAuthorityDisplayName(alfAuthorityName, displayName);
			String mcAdminGroup = getMediacenterAdminGroup(alfAuthorityName);
			if(mcAdminGroup != null) {
				authorityService.setAuthorityDisplayName(mcAdminGroup, displayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
			}

			String mcProxyGroup = getMediacenterProxyGroup(alfAuthorityName);
			if(mcProxyGroup != null) {
				authorityService.setAuthorityDisplayName(mcProxyGroup, displayName + AuthorityService.MEDIA_CENTER_PROXY_DISPLAY_POSTFIX);
			}
		}

		updateProperty(authorityNodeRef,CCConstants.CCM_PROP_ADDRESS_POSTALCODE, postalCode);
		updateProperty(authorityNodeRef,CCConstants.CCM_PROP_ADDRESS_CITY, city);
		updateProperty(authorityNodeRef,CCConstants.CCM_PROP_MEDIACENTER_DISTRICT_ABBREVIATION, districtAbbreviation);
		updateProperty(authorityNodeRef,CCConstants.CCM_PROP_MEDIACENTER_MAIN_URL, mainUrl);
		updateProperty(authorityNodeRef,CCConstants.CCM_PROP_MEDIACENTER_CATALOGS, mediacenterCatalogs);

		if(active){
			this.setActive(true, authorityName );
		}else{
			this.setActive(false, authorityName );
		}
	}

	private void updateProperty(NodeRef authorityNodeRef, String property, String newValue){
		String oldValue = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(property));
		if(newValue != null && !newValue.equals(oldValue)) {
			nodeService.setProperty(authorityNodeRef, QName.createQName(property), newValue);
		}
	}

	public String createMediacenter(String id, String displayName, String postalCode, String city) throws Exception {

		String authorityName = AuthorityService.MEDIA_CENTER_GROUP_TYPE + "_" + id;

		/**
		 * create mediacenter group
		 */
		String alfAuthorityName = authorityService.createAuthority(AuthorityType.GROUP, authorityName);
		authorityService.setAuthorityDisplayName(alfAuthorityName, displayName);

		/**
		 * create mediacenter admin group
		 */
		createMediacenterAdminGroup(alfAuthorityName, displayName);

		/**
		 * create mediacenter proxy group
		 */
		createMediacenterProxyGroup(alfAuthorityName,displayName);

		/**
		 * add mediacenter metadata
		 */
		NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(alfAuthorityName);

		Map<QName, Serializable> groupExtProps = new HashMap<QName, Serializable>();
		groupExtProps.put(QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE), AuthorityService.MEDIA_CENTER_GROUP_TYPE);
		nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_GROUPEXTENSION), groupExtProps);

		Map<QName, Serializable> groupAddressProps = new HashMap<QName, Serializable>();
		if(postalCode != null) groupAddressProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE), postalCode);
		if(city != null) groupAddressProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), city);
		nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_ADDRESS), groupAddressProps);

		Map<QName, Serializable> groupMZProps = new HashMap<QName, Serializable>();
		groupMZProps.put(QName.createQName(CCConstants.CCM_PROP_MEDIACENTER_ID), id);
		if(city != null) groupMZProps.put(QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), city);
		nodeService.addAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER), groupMZProps);

		return alfAuthorityName;
	}




	@Override
	public int importOrganisations(InputStream csv) {
		RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
			@Override
			public Integer doWork() throws Exception {
				List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_UTF8);

				int counter = 0;
				for (List<String> record : records) {
					String schoolId = record.get(0);
					String schoolName = record.get(1);
					String plz = (record.size() > 2) ? record.get(2) : null;
					String city = (record.size() > 3) ? record.get(3) : null;

					try {
						if(schoolId == null || schoolId.trim().length() == 0) {
							logger.info("no schoolid provided:" + record);
							continue;
						}

						if(authorityService.authorityExists("GROUP_ORG_" + schoolId)) {
							logger.info("authority already exists:" + schoolId);
							NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef("GROUP_ORG_" + schoolId);
							String alfAuthorityName = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
							String currentDisplayName = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYDISPLAYNAME));
							String currentCity = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY));
							String currentPLZ = (String)nodeService.getProperty(authorityNodeRef,QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE));
							
							if(schoolName != null && !schoolName.equals(currentDisplayName)) {
								authorityService.setAuthorityDisplayName(alfAuthorityName, schoolName);
								String authorityNameOrgAdmin = organisationService.getOrganisationAdminGroup(alfAuthorityName);
								if(authorityNameOrgAdmin != null) {
									authorityService.setAuthorityDisplayName(authorityNameOrgAdmin, schoolName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX);
								}
							}
							
							if(city != null && !city.equals(currentCity)) {
								nodeService.setProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_CITY), currentCity);
							}
							
							if(plz != null && !plz.equals(currentPLZ)) {
								nodeService.setProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_ADDRESS_POSTALCODE), plz);
							}
							
							continue;
						}

						logger.info("creating: " + schoolId + " " + schoolName);
						String organisationName = organisationService.createOrganization(schoolId, schoolName);

						String authorityName = PermissionService.GROUP_PREFIX + organisationName;

						eduAuthorityService2.addAuthorityAspect(authorityName, CCConstants.CCM_ASPECT_ADDRESS);
						eduAuthorityService2.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_ADDRESS_POSTALCODE,
								plz);
						eduAuthorityService2.setAuthorityProperty(authorityName, CCConstants.CCM_PROP_ADDRESS_CITY, city);


						counter++;
					} catch (Exception e) {
						logger.error("error in record: " + ((record == null || record.size() < 1)?null:record.get(0)), e);
						throw e;
					}

				}

				return counter;
			}
		};

		return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
	}

	@Override
	public int importOrgMcConnections(InputStream csv, boolean removeSchoolsFromMC) {
		RunAsWork<Integer> runAs = new RunAsWork<Integer>() {
			@Override
			public Integer doWork() throws Exception {

				List<List<String>> records = new CSVTool().getRecords(csv, CSVTool.ENC_UTF8);

				/**
				 * remove schools from mediacenter
				 */
				if(removeSchoolsFromMC) {
					
					Map<String,List<String>> newMZsAndSchools = listToUniqueMap(records);
					
					/**
					 * get existing mediacenters
					 */
					{
						SearchParameters sp = new SearchParameters();
						sp.setQuery("ASPECT:\"ccm:mediacenter\"");
						sp.setLanguage(SearchService.LANGUAGE_LUCENE);
						sp.setSkipCount(0);
						sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
						
						ResultSet rs = searchService.query(sp);
						if (rs == null || rs.length() < 1) {
							logger.error("no mediacenters found");
						}else {
							Map<String,List<String>> existingMZsAndSchools = new HashMap<String,List<String>>();
							for(NodeRef mzNodeRef : rs.getNodeRefs()) {
								String authorityName = (String)nodeService.getProperty(mzNodeRef, ContentModel.PROP_AUTHORITY_NAME);
								String mzId = authorityName.replace("GROUP_MEDIA_CENTER_", "");
								try {
									Integer.parseInt(mzId);
									Set<String> mzContains = authorityService.getContainedAuthorities(AuthorityType.GROUP, authorityName, true);
									
									for(String schoolAuthorityName : mzContains) {
										//"GROUP_ORG_" + schoolId
										NodeRef nodeRef = authorityService.getAuthorityNodeRef(schoolAuthorityName);
										
										if(nodeRef == null) {
											logger.info("authority does not exist:" + schoolAuthorityName);
											continue;
										}
										if(!nodeService.hasAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))) {
											logger.debug("authority is no edugroup:" + schoolAuthorityName);
											continue;
										}
										
										String schoolId = schoolAuthorityName.replace("GROUP_ORG_", "");
										
										List<String> schools = existingMZsAndSchools.get(mzId);
										if(schools == null) {
											schools = new ArrayList<String>();
										}
										if(!schools.contains(schoolId)) {
											schools.add(schoolId);
										}
										existingMZsAndSchools.put(mzId, schools);
									}
								}catch(NumberFormatException e) {
									logger.info("authorityName:" + authorityName + " mzId:" + mzId + " is no number" );
								}
							}
							
							for(Map.Entry<String,List<String>> mzAndSchools : existingMZsAndSchools.entrySet()) {
								List<String> newSchools = newMZsAndSchools.get(mzAndSchools.getKey());
								if(newSchools == null) {
									logger.info("existing mz:" + mzAndSchools.getKey() +" has a null school list in new sheet");
									newSchools = new ArrayList<String>();
								}
								
								if(mzAndSchools.getValue() == null) {
									logger.info("existing mz:" + mzAndSchools.getKey() +" has a null school list");
									continue;
								}
								
								
								for(String existingSchoolId : mzAndSchools.getValue()) {
									if(!newSchools.contains(existingSchoolId)) {
										String mzAuthorityName = "GROUP_MEDIA_CENTER_" + mzAndSchools.getKey();
										String schoolAuthorityName = "GROUP_ORG_" + existingSchoolId;
										logger.info("removing school " + schoolAuthorityName + " from " + mzAuthorityName +" cause its not in imported list");
										authorityService.removeAuthority(mzAuthorityName, schoolAuthorityName);
									}
								}
							}
						}
					}
				}
				
				
				int counter = 0;
				for (List<String> record : records) {
					String mzId = record.get(0);
					String schoolId = record.get(1);

					SearchParameters sp = getSearchParameterMZ(mzId);
					ResultSet rs = searchService.query(sp);

					if (rs == null || rs.length() < 1) {
						logger.error("no mediacenter found for " + mzId);
						continue;
					}

					NodeRef nodeRefAuthorityMediacenter = rs.getNodeRef(0);

					String authorityNameSchool = "GROUP_ORG_" + schoolId;

					sp = new SearchParameters();
					sp.setQuery("@cm\\:authorityName:" + authorityNameSchool);
					sp.setMaxItems(1);
					sp.setLanguage(SearchService.LANGUAGE_LUCENE);
					sp.setSkipCount(0);
					sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					rs = searchService.query(sp);
					if (rs == null || rs.length() < 1) {
						logger.error("no school found for " + schoolId + " " + authorityNameSchool);
						continue;
					}

					String authorityNameMZ = (String) nodeService.getProperty(nodeRefAuthorityMediacenter,
							ContentModel.PROP_AUTHORITY_NAME);
					
					
					Set<String> mzContains = authorityService.getContainedAuthorities(AuthorityType.GROUP,authorityNameMZ,true);
					
					if(!mzContains.contains(authorityNameSchool)) {
						logger.info("adding school" + authorityNameSchool + " to MZ " + authorityNameMZ );
						authorityService.addAuthority(authorityNameMZ, authorityNameSchool);
						counter++;
					}else {
						logger.info("mediacenter:" + authorityNameMZ + " already contains " + authorityNameSchool);
					}
					
				}
				return counter;
			}
		};

		return AuthenticationUtil.runAs(runAs, ApplicationInfoList.getHomeRepository().getUsername());
	}
	
	Map<String,List<String>> listToUniqueMap(List<List<String>> records){
		Map<String,List<String>> result = new HashMap<String,List<String>>();

		for (List<String> record : records) {
			if(!result.containsKey(record.get(0))) {
				List<String> list = new ArrayList<String>();
				list.add(record.get(1));
				result.put(record.get(0), list);
			}else {
				result.get(record.get(0)).add(record.get(1));
			}
		}
		
		return result;
	}
	
	private SearchParameters getSearchParameterMZ(String mzId) {
		SearchParameters sp = new SearchParameters();
		sp.setQuery("ASPECT:\"ccm:mediacenter\" AND @ccm\\:mediacenterId:\"" + mzId + "\"");
		sp.setMaxItems(1);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setSkipCount(0);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		return sp;
	}
	
	/**
	 * 
	 *
	 * @return
	 */
	public String getMediacenterAdminGroup(String authorityName) {

		NodeRef eduGroupNodeRef =authorityService.getAuthorityNodeRef(authorityName);
		List<ChildAssociationRef> childGroups = nodeService.getChildAssocs(eduGroupNodeRef);
		for(ChildAssociationRef childGroup : childGroups){
			String grouptype = (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
			if(AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE.equals(grouptype)){
				return (String)nodeService.getProperty(childGroup.getChildRef(), QName.createQName(CCConstants.CM_PROP_AUTHORITY_AUTHORITYNAME));
			}
		}

		return null;
	}

	public void isAllowedToManage(String authorityName){

		if(org.edu_sharing.service.authority.AuthorityServiceFactory.getLocalService().isGlobalAdmin()){
			return;
		}

		ToolPermissionHelper.throwIfToolpermissionMissing(CCConstants.CCM_VALUE_TOOLPERMISSION_MEDIACENTER_MANAGE);
		String mediacenterAdminGroup = getMediacenterAdminGroup(authorityName);
		Set<String> mediacenterAdmins = authorityService.getContainedAuthorities(AuthorityType.USER, mediacenterAdminGroup,false);
		if(!mediacenterAdmins.contains(serviceregistry.getAuthenticationService().getCurrentUserName())){
			throw new RuntimeException("current user is not part of mediacenter admin group");
		}
	}

	/**
	 * create mediacenter proxy group and add mediacenter group to proxy group
	 */
	public void createMediacenterProxyGroup(String alfAuthorityName, String displayName) throws Exception{

		String mediacenterId = getMediacenterId(alfAuthorityName);
		String mediacenterProxyName = AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_" + mediacenterId;
		AuthorityServiceFactory.getLocalService().createGroupWithType(
				mediacenterProxyName,
				displayName + AuthorityService.MEDIA_CENTER_PROXY_DISPLAY_POSTFIX,
				null,
				AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE);
		authorityService.addAuthority("GROUP_" + mediacenterProxyName, alfAuthorityName);
		String mediacenterAdminGroup = getMediacenterAdminGroup(alfAuthorityName);
		authorityService.addAuthority("GROUP_" + mediacenterProxyName, mediacenterAdminGroup);
	}

	public void createMediacenterAdminGroup(String alfAuthorityName, String displayName) throws Exception {
		AuthorityServiceFactory.getLocalService().createGroupWithType(
				AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP,
				displayName + AuthorityService.ADMINISTRATORS_GROUP_DISPLAY_POSTFIX,
				alfAuthorityName.replace("GROUP_",""),
				AuthorityService.MEDIACENTER_ADMINISTRATORS_GROUP_TYPE);
	}
	
	public String getMediacenterProxyGroup(String authorityName) {

		String proxyAuthorityName = PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE
				+ "_"
				+ getMediacenterId(authorityName);

		if(authorityService.authorityExists(proxyAuthorityName)){
			NodeRef nodeRef = authorityService.getAuthorityNodeRef(proxyAuthorityName);
			String groupType = (String)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_GROUPEXTENSION_GROUPTYPE));
			if(AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE.equals(groupType)){
				return proxyAuthorityName;
			}
		}

		return null;
	}

	private String getMediacenterId(String authorityName){
		return authorityName.replace(PermissionService.GROUP_PREFIX + AuthorityService.MEDIA_CENTER_GROUP_TYPE +"_","");
	}


	public boolean isActive(String authorityName){
		String proxyGroup = getMediacenterProxyGroup(authorityName);
		if(proxyGroup == null){
			return false;
		}

		Set<String> containedAuthorities = authorityService.getContainedAuthorities(AuthorityType.GROUP, proxyGroup, false);
		if(containedAuthorities == null){
			return false;
		}

		if(containedAuthorities.contains(authorityName)){
			return true;
		}else{
			return false;
		}

	}

	public void setActive(boolean active, String authorityName){
		if(active){
			if(isActive(authorityName)){
				return;
			}

			String proxyGroup = getMediacenterProxyGroup(authorityName);
			if(proxyGroup == null){
				logger.error("no proxy group found for " + authorityName);
				return;
			}

			authorityService.addAuthority(proxyGroup, authorityName);
		}else{
			String proxyGroup = getMediacenterProxyGroup(authorityName);
			if(proxyGroup != null && authorityService.authorityExists(proxyGroup)) {
				authorityService.removeAuthority(proxyGroup, authorityName);
			}
		}
	}


	private HashMap<String,NodeRef> getImportedNodes(String startFolder){

		HashMap<String,NodeRef> result = new HashMap<String,NodeRef>();
		NodeRunner runner = new NodeRunner();
		runner.setTask((ref)->{
			String replicationSourceId = (String) nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID));
			if(replicationSourceId != null) {
				result.put(replicationSourceId,ref);
			}
		});

		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
		runner.setStartFolder(startFolder);
		runner.setRunAsSystem(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		runner.setThreaded(false);

		int processNodes = runner.run();
		logger.info("processed nodes:" + processNodes +" size:" +result.size());
		return result;
	}

	List<String> getAllMediacenterIds(){
		Set<String> allGroups = authorityService.getAllAuthoritiesInZone(org.alfresco.service.cmr.security.AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);

		List<String> result = new ArrayList<>();

		for(String group : allGroups) {
			NodeRef authorityNodeRef = authorityService.getAuthorityNodeRef(group);
			if(nodeService.hasAspect(authorityNodeRef, QName.createQName(CCConstants.CCM_ASPECT_MEDIACENTER))) {
				String mediacenterId = (String)nodeService.getProperty(authorityNodeRef, QName.createQName(CCConstants.CCM_PROP_MEDIACENTER_ID));
				result.add(mediacenterId);
			}
		}
		return result;
	}


	boolean hasPermission(NodeRef nodeRef, String authority, String permission) {
		AuthenticationUtil.RunAsWork<Boolean> runAs = () -> {
			if(permissionService.hasPermission(nodeRef, permission) == AccessStatus.ALLOWED) {
				return true;
			}else {
				return false;
			}
		};
		return AuthenticationUtil.runAs(runAs, authority);
	}

	public void manageNodeLicenses(){
		logger.info("cache mediacenterids");
		List<String> allMediacenterIds = getAllMediacenterIds();
		logger.info("cache mediacenter nodes");

		Repository repositoryHelper = (Repository) applicationContext.getBean("repositoryHelper");
		HashMap<String, NodeRef> importedNodes = getImportedNodes(repositoryHelper.getCompanyHome().getId());


		HashMap<String,List<String>> sodisMediacenterIdNodes = new HashMap<>();
		for(String mediacenterId : allMediacenterIds) {
			logger.info("cache sodis mediacenter nodes mediacenterId:" + mediacenterId +" already cached:" + sodisMediacenterIdNodes);
			List<String> nodes = MediacenterLicenseProviderFactory.getMediacenterLicenseProvider().getNodes(mediacenterId);
			sodisMediacenterIdNodes.put(mediacenterId, nodes);
		}


		HashMap<String,List<NodeRef>> addToMediacenterList = new HashMap<>();
		HashMap<String,List<NodeRef>> removeFromMediacenterList = new HashMap<>();

		for(String mediacenterId : allMediacenterIds){
			logger.info("collect differences for "+ mediacenterId);
			List<String> sodisLicensedNodes = sodisMediacenterIdNodes.get(mediacenterId);
			if(sodisLicensedNodes == null || sodisLicensedNodes.size() == 0) {
				logger.info("leave out mediacenter " + mediacenterId +" cause no licensed nodes found");
				continue;
			}
			String mediacenterName = "GROUP_" + AuthorityService.MEDIA_CENTER_PROXY_GROUP_TYPE + "_" + mediacenterId;


			for (Map.Entry<String, NodeRef> entry : importedNodes.entrySet()) {

				boolean hasPublishPermission = hasPermission(entry.getValue(), mediacenterName,
						CCConstants.PERMISSION_CC_PUBLISH);
				boolean hasConsumerPermission = hasPermission(entry.getValue(), mediacenterName,
						CCConstants.PERMISSION_CONSUMER);

				if (sodisLicensedNodes.contains(entry.getKey()) && (!hasConsumerPermission || !hasPublishPermission)) {
					List<NodeRef> nodeRefs = addToMediacenterList.get(mediacenterName);
					if (nodeRefs == null) {
						nodeRefs = new ArrayList<NodeRef>();
						addToMediacenterList.put(mediacenterName, nodeRefs);
					}
					nodeRefs.add(entry.getValue());
				} else if (!sodisLicensedNodes.contains(entry.getKey())
						&& (hasConsumerPermission || hasPublishPermission)) {
					List<NodeRef> nodeRefs = removeFromMediacenterList.get(mediacenterName);
					if (nodeRefs == null) {
						nodeRefs = new ArrayList<NodeRef>();
						removeFromMediacenterList.put(mediacenterName, nodeRefs);
					}
					nodeRefs.add(entry.getValue());
				}
			}
		}


		for(Map.Entry<String, List<NodeRef>> entry : addToMediacenterList.entrySet()) {
			String mediacenter = entry.getKey();
			logger.info("process add changes for " + mediacenter);
			for(NodeRef nodeRef : entry.getValue()) {
				policyBehaviourFilter.disableBehaviour(nodeRef);
				permissionService.setPermission(nodeRef, mediacenter, CCConstants.PERMISSION_CONSUMER , true);
				permissionService.setPermission(nodeRef, mediacenter, CCConstants.PERMISSION_CC_PUBLISH , true);
				policyBehaviourFilter.enableBehaviour(nodeRef);

			}
		}

		for(Map.Entry<String, List<NodeRef>> entry : removeFromMediacenterList.entrySet()) {
			String mediacenter = entry.getKey();
			logger.info("process remove changes for " + mediacenter);
			for(NodeRef nodeRef : entry.getValue()) {
				policyBehaviourFilter.disableBehaviour(nodeRef);
				permissionService.deletePermission(nodeRef, mediacenter, CCConstants.PERMISSION_CONSUMER);
				permissionService.deletePermission(nodeRef, mediacenter, CCConstants.PERMISSION_CC_PUBLISH);
				policyBehaviourFilter.enableBehaviour(nodeRef);
			}
		}

	}


}
