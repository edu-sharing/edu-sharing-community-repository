/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.MLText;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.MCBaseClient;
import org.springframework.context.ApplicationContext;

public class ClassificationKWToGeneralKW implements Update {

	public static final String ID = "CKW_TO_GKW";
	
	public static final String description = "UPDATE AUF 1.4.0. speichert alle lom classification keywords nach general keywords";
	
	static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	NodeService nodeService = serviceRegistry.getNodeService();
	
	private static Log logger = LogFactory.getLog(ClassificationKWToGeneralKW.class);

	Set<String> udpatedIOs = new HashSet<String>(); 
	
	PrintWriter out;
	
	public ClassificationKWToGeneralKW(PrintWriter out) {
		this.out = out;
	}
	
	@Override
	public void execute() {
		run(false);
	}

	public String getId() {
		return ID;
	};
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void test() {
		run(true);
	}

	private void run(boolean test) {
		
		logger.info("starting with test:"+test);
		out.println("starting with test:"+test);
		
		try {

			MCBaseClient mcBaseClient = new MCAlfrescoAPIClient();
			MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient) mcBaseClient;
			String companyHomeId = mcAlfrescoBaseClient.getCompanyHomeNodeId();
			
			runFromLevel(test,companyHomeId);
			
			logger.info("updated ios:"+udpatedIOs.size() +" test:"+test);
			out.println("updated ios:"+udpatedIOs.size() +" test:"+test);
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private void runFromLevel(boolean test, String parentId){
		
		logger.info("starting with parentId:"+parentId);
		out.println("starting with parentId:"+parentId);
		
		List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(new NodeRef(storeRef, parentId));
		
		for (ChildAssociationRef objectAssocRef : childAssocs) {
			
			// process io
			 QName typeQName = nodeService.getType(objectAssocRef.getChildRef());
			 if(typeQName.isMatch(QName.createQName(CCConstants.CCM_TYPE_IO))){
				 
				Set<QName> childSet = new HashSet<QName>();
				childSet.add(QName.createQName(CCConstants.LOM_TYPE_CLASSIFICATION));
				 
				List<ChildAssociationRef> classificationRefs = nodeService.getChildAssocs(objectAssocRef.getChildRef(),childSet);
				for(ChildAssociationRef classRef : classificationRefs){
					List<MLText> classKeywordList = (List<MLText>)nodeService.getProperty(classRef.getChildRef(), QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
					if(classKeywordList != null && classKeywordList.size() > 0){
						List<MLText> generalKeyWordList = (List<MLText>)nodeService.getProperty(objectAssocRef.getChildRef(), QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD));
						writeToGeneralKeyword(objectAssocRef.getChildRef(), generalKeyWordList, classKeywordList,test);
					}
				}
			 }
			
			// process child
			 if(typeQName.isMatch(QName.createQName(CCConstants.CCM_TYPE_MAP)) || typeQName.isMatch(QName.createQName(CCConstants.CM_TYPE_FOLDER) )){
				 runFromLevel(test,objectAssocRef.getChildRef().getId());
			 }
		}
	}
	
	private void writeToGeneralKeyword(NodeRef nodeRef, List<MLText> generalKeyWordList, List<MLText> classificationKeywordList, boolean test){
		ArrayList<MLText> newKeywordsList = new ArrayList<MLText>();
		
		if(generalKeyWordList != null){
			newKeywordsList.addAll(generalKeyWordList);
		}
		
		for(MLText classKW : classificationKeywordList){
			
			logger.info(" classKW.getValues().size(): " + classKW.getValues().size() +" classKW.getDefaultValue():"+classKW.getDefaultValue());
			out.println(" classKW.getValues().size(): " + classKW.getValues().size() +" classKW.getDefaultValue():"+classKW.getDefaultValue());
		
			if(classKW.getValues().size() > 0 && !classKW.getDefaultValue().trim().equals("") && !mlTextListContainsValue(newKeywordsList,classKW)){
				newKeywordsList.add(classKW);
			}
		}
		
		if((generalKeyWordList == null && newKeywordsList.size() > 0 ) || (newKeywordsList.size() > generalKeyWordList.size())){
			
			logger.info("updating node IO: " + nodeRef.getId());
			out.println("updating node IO: " + nodeRef.getId());
			
			if(!test){
				nodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_GENERAL_KEYWORD), newKeywordsList);	
			}
			
			udpatedIOs.add(nodeRef.getId());
		}
		
	}
	
	boolean mlTextListContainsValue(List<MLText> list, MLText value){
		
		for(MLText tmpMlText : list){
			if(tmpMlText.getDefaultValue() != null && tmpMlText.getDefaultValue().trim().equals(value.getDefaultValue().trim())){
				return true;
			}
		}
		
		return false;
	}

}
