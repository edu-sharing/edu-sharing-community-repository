package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.springframework.context.ApplicationContext;

public class Release_1_7_UnmountGroupFolders extends UpdateAbstract {

	public static final String ID = "Release_1_7_UnmountGroupFolders";
	
	public static final String description = "unmounts edu group folders out of userhomes";
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	NodeService nodeService = null;
	NodeService ns = null;
	public Release_1_7_UnmountGroupFolders(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(Release_1_7_UnmountGroupFolders.class);
	}
	
	@Override
	public void execute() {
		doIt(false);
	}
	
	@Override
	public void test() {
		doIt(true);
	}
	
	private void doIt(boolean test){
		
		logger.debug("starting with testmode="+test);
		//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
		//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
		//create here cause of authentication of Update servlet is passed instead of in constructor
		nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
		ns = serviceRegistry.getNodeService();
		
		try{
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				umountEduGroupFoldersInUserHomes(test);
				if(!test){
					protocol.writeSysUpdateEntry(this.getId());
				}
			}else{
				logInfo("update" +this.getId()+ " already done at "+updateInfo.get(CCConstants.CCM_PROP_SYSUPDATE_DATE));
			}
			
		}catch(Throwable e){
			logError(e.getMessage(), e);
		}
		logger.debug("finished with testmode="+test);
	}
	
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	public void unmountEduGroupFoldersInUserHomes(NodeRef groupNodeRef,boolean test){
		if(nodeService.hasAspect(groupNodeRef, QName.createQName(CCConstants.CCM_ASPECT_EDUGROUP))){
			NodeRef eduGroupHomeDirRef = (NodeRef)nodeService.getProperty(groupNodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
			
			String groupname =(String) nodeService.getProperty(groupNodeRef, ContentModel.PROP_NAME);
			
			if(eduGroupHomeDirRef == null || !nodeService.exists(eduGroupHomeDirRef)){
				logger.warn("eduGroupHomeDirRef of group "+groupname+" does not exist");
				return;
			}
			
			if(eduGroupHomeDirRef != null){
				
				int counter = 0;
				List<ChildAssociationRef> childAssocRefs = nodeService.getChildAssocs(groupNodeRef, new HashSet(Arrays.asList(new QName[]{ContentModel.TYPE_PERSON})));
				logger.debug("unmounting groupfolder for group "+groupname+" for "+childAssocRefs.size()+" persons");
				for(ChildAssociationRef personChildRef : childAssocRefs){
					NodeRef personHomeFolderNodeRef = (NodeRef)nodeService.getProperty(personChildRef.getChildRef() , ContentModel.PROP_HOMEFOLDER);
					List<ChildAssociationRef>  childAssocMapTypes = nodeService.getChildAssocsByPropertyValue(personHomeFolderNodeRef, QName.createQName(CCConstants.CCM_PROP_MAP_TYPE),CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP);
					if(childAssocMapTypes != null && childAssocMapTypes.size() == 1){
						ChildAssociationRef childAssocRefGroupFolder = childAssocMapTypes.get(0);
						
						List<ChildAssociationRef> groupFolderChildren = nodeService.getChildAssocs(childAssocRefGroupFolder.getChildRef());
						for(ChildAssociationRef groupFolderChild : groupFolderChildren){
							if(groupFolderChild.getChildRef().equals(eduGroupHomeDirRef)){
								if(!test){
									ns.removeChild(childAssocRefGroupFolder.getChildRef(), eduGroupHomeDirRef);
								}
								counter++;
								
								if((counter % 10) == 0){
									logger.debug("unmounted group folder of group "+groupname+" for "+counter +" persons");
								}
							}
						}
						
					}else{
						logger.debug("can not unmount group Folder for person:"+personChildRef.getChildRef()+" cause of missing or invalid "+CCConstants.CCM_VALUE_MAP_TYPE_EDUGROUP+" Folder");
					}
				}
				logger.debug("unmounted eduGroupHomeDirRef:"+eduGroupHomeDirRef+" in "+counter+" user homes");
			}
		}
	}
	
	public void umountEduGroupFoldersInUserHomes(boolean test){
		NodeRef rootNode = nodeService.getRootNode(new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore"));
		List<ChildAssociationRef> rootChildAssocs = nodeService.getChildAssocs(rootNode);
		for(ChildAssociationRef childAssocRef : rootChildAssocs){
			
			if(childAssocRef.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}system"))){
				
				List<ChildAssociationRef> systemChildren =  nodeService.getChildAssocs(childAssocRef.getChildRef());
				
				for(ChildAssociationRef sysChild : systemChildren){
					if(sysChild.getQName().equals(QName.createQName("{"+NamespaceService.SYSTEM_MODEL_1_0_URI+"}authorities"))){
						List<ChildAssociationRef> authorities = nodeService.getChildAssocs(sysChild.getChildRef());
						
						for(ChildAssociationRef authorityChild :authorities ){
							logger.debug("found authority"+authorityChild.getQName());
							this.unmountEduGroupFoldersInUserHomes(authorityChild.getChildRef(),test);
						}
						
					}
				}
				
			}
		}
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}
	
}
