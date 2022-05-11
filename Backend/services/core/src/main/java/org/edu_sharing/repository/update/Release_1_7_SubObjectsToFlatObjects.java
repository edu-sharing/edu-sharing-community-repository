package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.UserTransaction;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.version.common.VersionUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.TaxonTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.springframework.context.ApplicationContext;

public class Release_1_7_SubObjectsToFlatObjects implements Update {

	public static final String ID = "Release_1_7_SubObjectsToFlatObjects";

	public static final String description = "subobjects data like contributers will be transfered in a flat IO property";

	private static Log logger = LogFactory.getLog(Release_1_7_SubObjectsToFlatObjects.class);

	PrintWriter out = null;
	MCAlfrescoBaseClient mcAlfrescoBaseClient = null;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	NodeService nodeService = null;

	StoreRef spacesStoreStoreRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	StoreRef archiveStoreRef = new StoreRef("archive://SpacesStore");

	Collection<QName> allPropertiesInAlf = serviceRegistry.getDictionaryService().getAllProperties(null);

	int counter = 0;
	int archivCounter = 0;
	
	public Release_1_7_SubObjectsToFlatObjects() {
		this(null);
	}
	public Release_1_7_SubObjectsToFlatObjects(PrintWriter out) {
		this.out = out;
		try {
			mcAlfrescoBaseClient = new MCAlfrescoAPIClient();

		} catch (Throwable e) {
			logError(e.getMessage(), e);
			
		}
	}

	@Override
	public void execute() {
		run(false);

	}

	@Override
	public void test() {
		run(true);
	}

	public void run(boolean test) {
		
		logDebug("STARTING UPDATE ID Release_1_7_SubObjectsToFlatObjects");
		
		//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
		//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
		//this can lead to a problem, that every edugroupfolder is processed for all members of the edugroup again
		nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
		
		counter = 0;
		archivCounter = 0;

		boolean isMLAware = LightbendConfigLoader.get().getBoolean("repository.multilang");
		if(!isMLAware){
			String errorMsg = "please set multilang to true in config to run this updater. This updater is important to otherwise you could loose data";
			logDebug(errorMsg);
			//logError(errorMsg, new Exception(errorMsg));
			return;
		}
		
		UserTransaction userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
		
		try{
			if(!test) userTransaction.begin();
			
			
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
				
				//SpacesStore and Versionstore
				log("Start processing SpacesStore and VersionStore");
				runFromLevel(test, companyHomeId);
				
				//archiveStore
				log("Start processing ArchiveStore");
				NodeRef archiveRootNode = nodeService.getRootNode(archiveStoreRef);
				transferArchiveFromLevel(test,archiveRootNode);
				 
				if (!test) {
					protocol.writeSysUpdateEntry(this.getId());
				}
				
			}else{
				logDebug("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
			
			if (!test) userTransaction.commit();
		
		}catch(Throwable e){
			
			logError(e.getMessage(),e);
			try {
				log("trying rollback");
				userTransaction.rollback();
			} catch (Exception rollBackException) {
				logError(rollBackException.getMessage(),rollBackException);
			}
		}
		
		logDebug("FINISHED UPDATE ID Release_1_7_SubObjectsToFlatObjects");
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return ID;
	}

	private void lomMetadataTransfering(boolean test, NodeRef ioNodeRef) {
		
		//diable all policies for the update process
		//i.e. when userdefined preview the node wille be modified and we don't want the modeified date to be changed
		//and also the edu-sharing node policies should not be excecuted
		BehaviourFilter behaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");	
		try {
			if(!test){
				behaviourFilter.disableBehaviour(ioNodeRef);
			}
			lomSubTypesTransfering(test,ioNodeRef);
					
			//do this out of the transaction cause inside the renditionservice is activated when calling userTransaction.commit();
			//dont know why
			userdefinedPreview(test,ioNodeRef);
			
			//set the new 1.7 default property CCConstants.CCM_PROP_IO_CREATE_VERSION
			if(!test){
				nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_CREATE_VERSION), true);
			}
			
		} finally {
			if (!test) {
				behaviourFilter.enableBehaviour(ioNodeRef);
			}
		}
	
	}
	
	/**
	 * remove the assigned license data from archive node
	 * 
	 * only noderrefs of archive are handled
	 * @param test
	 * @param ioNodeRef
	 */
	private void assignedLicenses(boolean test, NodeRef ioNodeRef){
		
		if(!ioNodeRef.getStoreRef().equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE)){
			return;
		}
		
		Set<QName> qnames = new HashSet<QName>();
		qnames.add(QName.createQName(CCConstants.CCM_TYPE_ASSIGNED_LICENSE));
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(ioNodeRef, qnames);
		for(ChildAssociationRef childAssoc : childAssocs){
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_ASSIGNED_LICENSE))){
				if(!test){
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
			}
		}
	}
	
	private void userdefinedPreview(boolean test, NodeRef ioNodeRef){
		
		
		Set<QName> qnames = new HashSet<QName>();
		qnames.add(QName.createQName(CCConstants.CM_TYPE_THUMBNAIL));
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(ioNodeRef, qnames);
		for(ChildAssociationRef childAssoc : childAssocs){
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.CM_TYPE_THUMBNAIL))){
			
				String nodeName = (String)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.CM_NAME));
				if(nodeName.equals(CCConstants.CM_VALUE_THUMBNAIL_NAME_CCUSERDEFINED)){
					if(!test){
						
						ContentReader contentReader = serviceRegistry.getContentService().getReader(childAssoc.getChildRef(), QName.createQName(CCConstants.CM_PROP_CONTENT));
						ContentWriter contentWriter = serviceRegistry.getContentService().getWriter(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_USERDEFINED_PREVIEW), true);
						
						contentWriter.setEncoding(contentReader.getEncoding());
						contentWriter.setMimetype(contentReader.getMimetype());
						
						contentWriter.putContent(contentReader.getContentInputStream());
						
						nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
					}
				}else if(nodeName.equals(CCConstants.CM_VALUE_THUMBNAIL_NAME_imgpreview_png) && ioNodeRef.getStoreRef().equals(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE)){
					if(!test){
						nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
					}
				}
			}
		}
		
	}
	
	private void lomSubTypesTransfering(boolean test, NodeRef ioNodeRef){
	
		
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(ioNodeRef);
	
		Map<QName, Serializable> ioPropsToAdd = new HashMap<QName, Serializable>();
		
		for(ChildAssociationRef childAssoc : childAssocs){
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_CLASSIFICATION))){
				
				
				Serializable classPurpose = nodeService.getProperty(childAssoc.getChildRef(),QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_PURPOSE));
				if(classPurpose != null){
					ioPropsToAdd.put(QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_PURPOSE), classPurpose);
				}
				//taxon path till version 1.6(including) was single valued. so we can build one TAXONPATH_XML
				List<ChildAssociationRef> childrentaxonPath = nodeService.getChildAssocs(childAssoc.getChildRef());
				for(ChildAssociationRef taxonpathChildRef : childrentaxonPath){
					if(nodeService.getType(taxonpathChildRef.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_TAXON_PATH))){
						MLText source = (MLText)nodeService.getProperty(taxonpathChildRef.getChildRef(), QName.createQName(CCConstants.LOM_PROP_TAXONPATH_SOURCE));
						if(source != null){
							List<String> ids = (List<String>)nodeService.getProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_TAXON_ID));
							List<MLText> entries = (List<MLText>)nodeService.getProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY));
							String id = (ids != null && ids.size() > 0) ? ids.get(0) : null;
							String entry = (entries != null && entries.size() > 0) ? ((MLText)entries.get(0)).getDefaultValue() : null;
							if(id != null){
								if(ids.size() > 1) log("warning: this io " +ioNodeRef+ " got's more than one taxonid value (no default edu-sharing install). the first one will be taken for CCM_PROP_IO_REPL_TAXONPATH_XML.");
								if(!test){
									nodeService.setProperty(ioNodeRef,QName.createQName(CCConstants.CCM_PROP_IO_REPL_TAXONPATH_XML), new TaxonTool().getTaxonXML(source.getDefaultValue(), id, entry));
								}
							}
						}
					}
				}
				
				if(!test){
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
				
			}else if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_CONTRIBUTE))){
				
				MLText entity = (MLText) nodeService.getProperty(childAssoc.getChildRef(),
						QName.createQName(CCConstants.LOM_PROP_CONTRIBUTE_ENTITY));
				String role = (String) nodeService.getProperty(childAssoc.getChildRef(),
						QName.createQName(CCConstants.LOM_PROP_CONTRIBUTE_ROLE));

				String contributerProperty = null;
				if (childAssoc.getTypeQName().equals(QName.createQName(CCConstants.LOM_ASSOC_LIFECYCLE_CONTRIBUTE))) {
					// CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER
					String propPrefix = "{http://www.campuscontent.de/model/1.0}lifecyclecontributer_";
					contributerProperty = propPrefix + role;

				} else if (childAssoc.getTypeQName().equals(QName.createQName(CCConstants.LOM_ASSOC_META_METADATA_CONTRIBUTE))) {
					String propPrefix = "{http://www.campuscontent.de/model/1.0}metadatacontributer_";
					contributerProperty = propPrefix + role;
				}

				if (contributerProperty != null) {
					QName qnameProperty = QName.createQName(contributerProperty);
					if(allPropertiesInAlf.contains(qnameProperty)){
						ArrayList<String> entities = (ArrayList<String>)ioPropsToAdd.get(qnameProperty);
						if(entities == null) entities = new ArrayList<String>();	
						entities.add(entity.getDefaultValue());
						ioPropsToAdd.put(qnameProperty, entities);
					}else{
						logWarn("unknown alfresco property:"+contributerProperty+" will do nothing!");
					}
				}
			
				//remove the contributer
				if(!test){
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
				
			}else if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_EDUCATIONAL))){
				
				if(!test){
					
					String interactivityType = (String)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.LOM_PROP_EDUCATIONAL_INTERACTIVITYTYPE));
					
					if(interactivityType != null && !interactivityType.trim().equals("")){
						nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTERACTIVITYTYPE), interactivityType);
					}
					
					ArrayList<String> typicalAgeRange = (ArrayList<String>)nodeService.getProperty(childAssoc.getChildRef(), QName.createQName(CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE));
					if(typicalAgeRange != null && typicalAgeRange.size() > 0){
						nodeService.setProperty(ioNodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE), typicalAgeRange);
					}
					
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
				
			}else if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_IDENTIFIER))){
				
				if(!test){
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
				
			}else if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(CCConstants.LOM_TYPE_RELATION))){
				if(!test){
					nodeService.removeChild(ioNodeRef, childAssoc.getChildRef());
				}
			}
						
		}
		
		if(ioPropsToAdd.size() > 0){
			
			for(Map.Entry<QName,Serializable> entry : ioPropsToAdd.entrySet()){
				if(!test){
					nodeService.setProperty(ioNodeRef, entry.getKey(),entry.getValue());
				}
			}
		}
	}
	
	private void runFromLevel(boolean test, String parentId) {

		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(new NodeRef(spacesStoreStoreRef, parentId));
		for (ChildAssociationRef objectAssocRef : childAssocs) {
			
			//only follow primary path to prevent finding an linked object twice
			if(!objectAssocRef.isPrimary()){
				continue;
			}
			
			if (nodeService.getType(objectAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
				if((counter % 100)== 0){
					log("processed nodes:"+counter);
				}
				
				transferIO(test, objectAssocRef.getChildRef());
				counter++;
				
			} else if (nodeService.getType(objectAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))
					|| nodeService.getType(objectAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CM_TYPE_FOLDER))) {
				runFromLevel(test, objectAssocRef.getChildRef().getId());
			}
		}
	}
	
	/**
	 * must be called after the spaces store and version store transfering. cause in this process
	 * the removed io subobjects appear in archive store. this must be cleared
	 * @param test
	 * @param nodeRef
	 */
	private void transferArchiveFromLevel(boolean test, NodeRef nodeRef){
		
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(nodeRef);
		for (ChildAssociationRef objectAssocRef : childAssocs) {
			
			QName type = nodeService.getType(objectAssocRef.getChildRef());
			if (type.equals(QName.createQName(CCConstants.CCM_TYPE_IO))) {
				lomMetadataTransfering(test, objectAssocRef.getChildRef());
				assignedLicenses(test, objectAssocRef.getChildRef());
				archivCounter++;
				if((archivCounter % 100) == 0){
					log("processed archived io nodes:"+archivCounter);
				}
			} else if (nodeService.getType(objectAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CCM_TYPE_MAP))
					|| nodeService.getType(objectAssocRef.getChildRef()).equals(QName.createQName(CCConstants.CM_TYPE_FOLDER))) {
				
				transferArchiveFromLevel(test, objectAssocRef.getChildRef());
			}else if(type.equals(QName.createQName(CCConstants.LOM_TYPE_CLASSIFICATION)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_CONTRIBUTE)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_EDUCATIONAL)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_IDENTIFIER)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_RELATION)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_TAXON)) 
					|| type.equals(QName.createQName(CCConstants.LOM_TYPE_TAXON_PATH)) ){
				
				if(!test){
					nodeService.deleteNode(objectAssocRef.getChildRef());
				}
			}else if(type.equals(QName.createQName(CCConstants.CM_TYPE_THUMBNAIL))){
				
				//remove all thumbnails, when an io will be restored than the automatic thumbnail must be generated again
				if(!test){
					nodeService.deleteNode(objectAssocRef.getChildRef());
				}
				
			}
		}
	}

	private void transferIO(boolean test, NodeRef ioNodeRef) {
		String path = nodeService.getPath(ioNodeRef).toDisplayPath(nodeService, serviceRegistry.getPermissionService());
		String name = (String) nodeService.getProperty(ioNodeRef, QName.createQName(CCConstants.CM_NAME));
		
		//first: change the versions, cause when deleting contributers from the main object(workspace store) the version contributers will be deleted
		VersionService versionService = serviceRegistry.getVersionService();
		VersionHistory versionHistory = versionService.getVersionHistory(ioNodeRef);
		if(versionHistory != null){
			Collection<Version> versions = versionHistory.getAllVersions();
			for(Version version : versions){
				NodeRef versionNodeRef = VersionUtil.convertNodeRef(version.getFrozenStateNodeRef());
				lomMetadataTransfering(test,versionNodeRef);
			}
		}
		
		//second: change the main object
		lomMetadataTransfering(test,ioNodeRef);
		
		//remove versionhistory of assigned licenses, its a special handling cause we need the subobjects but not the versionhistory
		removeVersionHistoryOfChildren(test,ioNodeRef,CCConstants.CCM_TYPE_ASSIGNED_LICENSE);
		//remove the generated thumbnail versionhistory (the userdefined was removed by lomMetadataTransfering(...)
		removeVersionHistoryOfChildren(test,ioNodeRef,CCConstants.CM_TYPE_THUMBNAIL);
	}
	
	private void removeVersionHistoryOfChildren(boolean test, NodeRef ioNodeRef, String childType){
		Set<QName> qnames = new HashSet<QName>();
		qnames.add(QName.createQName(childType));
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(ioNodeRef, qnames);
		for(ChildAssociationRef childAssoc : childAssocs){
			if(nodeService.getType(childAssoc.getChildRef()).equals(QName.createQName(childType))){
				if(!test){
					serviceRegistry.getVersionService().deleteVersionHistory(childAssoc.getChildRef());
				}
			}
		}
	}
	
	private void log(String message){
		logger.info(message);
		if(out != null){
			out.println(message);
		}
	}
	
	private void logDebug(String message){
		logger.debug(message);
		if(out != null){
			out.println(message);
		}
	}
	
	private void logWarn(String message){
		logger.warn(message);
		if(out != null){
			out.println(message);
		}
	}
	
	private void logError(String message, Throwable e){
		logger.error(message, e);
		if(out != null){
			out.println(message);
		}
	}
}
