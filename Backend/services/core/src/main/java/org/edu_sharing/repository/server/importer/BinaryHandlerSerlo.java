package org.edu_sharing.repository.server.importer;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.transaction.TransactionService;
import org.edu_sharing.alfresco.policy.NodeCustomizationPolicies;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.w3c.dom.Node;

public class BinaryHandlerSerlo implements BinaryHandler{

	@Override
	public void safe(String alfrescoNodeId, RecordHandlerInterfaceBase recordHandler, Node nodeRecord) {
		ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
		
		TransactionService transactionService = serviceRegistry.getTransactionService();
		
		RetryingTransactionHelper rth = transactionService.getRetryingTransactionHelper();
		
		rth.doInTransaction(new RetryingTransactionCallback<Void>() {
			@Override
			public Void execute() throws Throwable {
				NodeCustomizationPolicies codeCustom = (NodeCustomizationPolicies)AlfAppContextGate.getApplicationContext().getBean("nodeCustomizationPolicies");
				
				String techLocation = (String) recordHandler.getProperties().get(CCConstants.LOM_PROP_TECHNICAL_LOCATION);
				if(techLocation != null) {
					techLocation += "?contentOnly";
					codeCustom.generateWebsitePreview(new NodeRef(MCAlfrescoAPIClient.storeRef, alfrescoNodeId), techLocation);
				}
				
				return null;
			}
		});
		
		
	}
}
