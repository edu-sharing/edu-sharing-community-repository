package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.context.ApplicationContext;

public class Release_3_2_DefaultScope implements Update {
public static String ID = "Release_3_2_DefaultScope";
	
	public static String description = "sets default scope for Maps and IO's";
	
	Logger logger = Logger.getLogger(Release_3_2_DefaultScope.class);
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = (NodeService)applicationContext.getBean("alfrescoDefaultDbNodeService");
	
	BehaviourFilter policyBehaviourFilter = (BehaviourFilter)applicationContext.getBean("policyBehaviourFilter");
	
	PrintWriter out;
	ArrayList<NodeRef> processedEduGroups = new ArrayList<NodeRef>();
	ArrayList<NodeRef> eduGroupFolderNodeIds = new ArrayList<NodeRef>();
	
	int processedNodeCounter = 0;
	
	public Release_3_2_DefaultScope(PrintWriter out) {
		this.out = out;
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
	
	private void run(boolean test){
		try{
		    /**
			 * disable policies for this node to prevent that beforeUpdateNode 
			 * checks the scope which will be there after update
			 */
			Protocol protocol = new Protocol();
			HashMap<String,Object> updateInfo = protocol.getSysUpdateEntry(this.getId());
			if(updateInfo == null){
				setDefautScope(nodeService.getRootNode(MCAlfrescoAPIClient.storeRef), test);
				if(!test){
					protocol.writeSysUpdateEntry(getId());
				}
			}else{
				if(this.out != null) this.out.println("Updater "+this.getId() + " already done");
				log("Updater "+this.getId() + " already done");
			}
			log("finished");
		}catch(Throwable e){
			if(this.out != null) this.out.println(e.getMessage());
			logger.error(e.getMessage(),e);
		}
	}
	
	void setDefautScope(NodeRef parent, boolean test) throws Throwable{
		List<ChildAssociationRef> childAssocRef = nodeService.getChildAssocs(parent);
		for(ChildAssociationRef child : childAssocRef){
			NodeRef noderef = child.getChildRef();
			String name = (String)nodeService.getProperty(child.getChildRef(),ContentModel.PROP_NAME);
			String nodeType = nodeService.getType(child.getChildRef()).toString();
			
			if(nodeService.hasAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE))){
				continue;
			}
			
			//serviceRegistry.getTransactionService().getRetryingTransactionHelper().doInTransaction(cb)
			
			
			
			if(CCConstants.CCM_TYPE_IO.equals(nodeType) 
					|| CCConstants.CCM_TYPE_TOOLPERMISSION.equals(nodeType)
					|| CCConstants.CCM_TYPE_NOTIFY.equals(nodeType)){
				log("updateing node:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()) +" processedNodeCounter:" + processedNodeCounter);
				if(!test){
					
					Map<QName,Serializable> aspectProps = new HashMap<QName,Serializable>();
					aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);
					
					
					serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
						@Override
						public Void execute() throws Throwable {
							policyBehaviourFilter.disableBehaviour(child.getChildRef());
							nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
							policyBehaviourFilter.enableBehaviour(child.getChildRef());
							return null;
						}
					});
					
				}
			}else if(CCConstants.CCM_TYPE_MAP.equals(nodeType) || CCConstants.CM_TYPE_FOLDER.equals(nodeType) ){
				if(this.out != null) this.out.println("updateing Map:"+ noderef +" in "+ nodeService.getPath(child.getChildRef()));
				
				if(!test){
					Map<QName,Serializable> aspectProps = new HashMap<QName,Serializable>();
					aspectProps.put(QName.createQName(CCConstants.CCM_PROP_EDUSCOPE_NAME), null);
					serviceRegistry.getRetryingTransactionHelper().doInTransaction(new RetryingTransactionCallback<Void>() {
						@Override
						public Void execute() throws Throwable {
							policyBehaviourFilter.disableBehaviour(child.getChildRef());
							nodeService.addAspect(child.getChildRef(), QName.createQName(CCConstants.CCM_ASPECT_EDUSCOPE), aspectProps);
							policyBehaviourFilter.enableBehaviour(child.getChildRef());
							return null;
						}
					});
				}
				
				if(eduGroupFolderNodeIds.contains(noderef)){
					if(processedEduGroups.contains(noderef)){
						if(this.out != null) this.out.println("already processed edugroup: " + name);
						log("already processed edugroup: " + name);
						continue;
					}else{
						log("remembering edugroup: "+name+ " " + noderef);
						processedEduGroups.add(noderef);
					}
				}
				
				setDefautScope(noderef, test);
			}
			processedNodeCounter++;
			
		}
	}
	
	private void log(String message){
		logger.info(message);
		if(this.out != null) this.out.println(message);	
	}
	
}
