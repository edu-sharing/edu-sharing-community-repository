package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;

import org.springframework.context.ApplicationContext;

public class Release_4_1_FixClassificationKeywordPrefix extends UpdateAbstract{

public static final String ID = "Release_4_1_FixClassificationKeywordPrefix";
	
	public static final String description = "Fix for io's that got an cclom:classification_keyword property which is not defined as valid io prop." ;
	
	public Release_4_1_FixClassificationKeywordPrefix(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(Release_4_1_FixClassificationKeywordPrefix.class);
	}
	
	@Override
	public void execute() {
		this.executeWithProtocolEntry();
	}
	
	@Override
	public void test() {
		this.logInfo("this updater has no test method");
	}
	
	@Override
	public void run() {
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
				ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
				ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
				SearchService ss = serviceRegistry.getSearchService();
				NodeService ns = serviceRegistry.getNodeService();
				SearchParameters sp = new SearchParameters();
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.setMaxItems(-1);
				sp.setQuery("TYPE:\"ccm:io\" AND @cclom\\:classification_keyword:\"*\"");
				
				ResultSet rs = ss.query(sp);
				for(NodeRef nodeRef : rs.getNodeRefs()) {
					List<String> lomClassificationKeyword = (List<String>)ns.getProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
					logInfo("switching cclom:classification_keyword for " + nodeRef + " " + ns.getProperty(nodeRef, QName.createQName(CCConstants.CM_NAME)));
					if(lomClassificationKeyword != null && lomClassificationKeyword.size() > 0) {
						ns.setProperty(nodeRef, QName.createQName(CCConstants.CCM_PROP_IO_REPL_CLASSIFICATION_KEYWORD), (ArrayList<String>)lomClassificationKeyword);
						ns.removeProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_CLASSIFICATION_KEYWORD));
					}
				}
				return null;
			}
		};
		
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return ID;
	}
	
	
}
