package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.cache.EduGroupCache;
import org.springframework.context.ApplicationContext;

public class Release_3_2_FillOriginalId implements Update {

	public static String ID = "Release_3_2_FillOriginalId";
	
	public static String description = "remembers original id for colletion ref objects to use solr grouping";
	
	Logger logger = Logger.getLogger(Release_3_2_FillOriginalId.class);
	
	ApplicationInfo appInfo = null;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	PrintWriter out;
	
	ArrayList<NodeRef> processedEduGroups = new ArrayList<NodeRef>();
	
	ArrayList<NodeRef> eduGroupFolderNodeIds = new ArrayList<NodeRef>();

	//very important: use the alfrescoDefaultDbNodeService defined in custom-core-services-context.xml
	//cause of overwriten getChild... methods in org.edu_sharing.alfresco.fixes.DbNodeServiceImpl
	//this can lead to a problem, that every edugroupfolder is processed for all members of the edugroup again
			
	NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");//serviceRegistry.getNodeService();
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
	
	public Release_3_2_FillOriginalId(PrintWriter out) {
		
		try{
			this.appInfo = ApplicationInfoList.getHomeRepository();
			this.out = out;
			
			for(NodeRef nodeRef : EduGroupCache.getKeys()){
				
				NodeRef eduGroupFolderNodeId = (NodeRef)nodeService.getProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_EDUGROUP_EDU_HOMEDIR));
				eduGroupFolderNodeIds.add(eduGroupFolderNodeId);
			}
			
		}catch(Throwable e){
			throw new RuntimeException(e.getMessage());
		}
		
	}
	
	@Override
	public void execute() {
		run(false);
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public void test() {
		run(true);
	}
	
	void run(boolean test){
	
		try{
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				logger.info("starting");
				setOrignalIds(nodeService.getRootNode(MCAlfrescoAPIClient.storeRef), test);
				logger.info("finished");
				
				if(!test){
					protocol.writeSysUpdateEntry(getId());
				}
			}else{
				if(this.out != null) this.out.println("Updater "+this.getId() + " already done");
				logger.debug("Updater "+this.getId() + " already done");
			}
			
		}catch(Throwable e){
			if(this.out != null) this.out.println(e.getMessage());
			logger.error(e.getMessage(),e);
		}
	
	}
	
	void setOrignalIds(NodeRef parent, boolean test) throws Throwable{
		List<ChildAssociationRef> childAssocRef = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef child : childAssocRef){
			NodeRef noderef = child.getChildRef();
			String name = (String)nodeService.getProperty(child.getChildRef(),ContentModel.PROP_NAME);
			String nodeType = nodeService.getType(child.getChildRef()).toString();
			if(CCConstants.CCM_TYPE_IO.equals(nodeType)){
				logger.info("updateing node:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()));
				if(this.out != null) this.out.println("updateing node:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()));
				if(!test){
					
					serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
						@Override
						public Void execute() throws Throwable {
							policyBehaviourFilter.disableBehaviour(child.getChildRef());
							nodeService.setProperty(child.getChildRef(), QName.createQName(CCConstants.CCM_PROP_IO_ORIGINAL), noderef.getId());
							policyBehaviourFilter.enableBehaviour(child.getChildRef());
							return null;
						}
					});
					
					
				}
			}else if(CCConstants.CCM_TYPE_MAP.equals(nodeType) 
					|| CCConstants.CM_TYPE_FOLDER.equals(nodeType)){
				if(eduGroupFolderNodeIds.contains(noderef)){
					if(processedEduGroups.contains(noderef)){
						if(this.out != null) this.out.println("already processed edugroup: " + name);
						logger.info("already processed edugroup: " + name);
						continue;
					}else{
						logger.info("remembering edugroup: "+name+ " " + noderef);
						processedEduGroups.add(noderef);
					}
				}
				
				setOrignalIds(noderef, test);
			}
		}
	}
}