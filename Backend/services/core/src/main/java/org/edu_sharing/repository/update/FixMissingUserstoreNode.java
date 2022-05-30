package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.UserTransaction;

import net.sf.acegisecurity.providers.dao.UsernameNotFoundException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.MutableAuthenticationDao;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.tools.KeyTool;
import org.springframework.context.ApplicationContext;

public class FixMissingUserstoreNode extends UpdateAbstract {

	public static String ID="FixMissingUserstoreNode";
	
	public static String description = "finds users that got no entry in userStore";
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	
	public FixMissingUserstoreNode() {
		this(null);
	}
	
	public FixMissingUserstoreNode(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(FixMissingUserstoreNode.class);
	}
	
	
	@Override
	public void execute() {
		doIt(false);
	}
	
	@Override
	public void test() {
		doIt(true);
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	
	private void doIt(boolean test){
		MutableAuthenticationDao authenticationDao = (MutableAuthenticationDao)applicationContext.getBean("authenticationDao");
		PersonService personService = serviceRegistry.getPersonService();
		NodeService nodeService = serviceRegistry.getNodeService();
		
		NodeRef peopleContainer = personService.getPeopleContainer();
		
		logInfo("peopleContainer:"+peopleContainer);
		
		List<ChildAssociationRef> children = nodeService.getChildAssocs(peopleContainer);
		
		ArrayList<String> missingUsers = new ArrayList<String>();
		
		for(ChildAssociationRef childref : children){
			
			if(!ContentModel.TYPE_PERSON.equals(nodeService.getType(childref.getChildRef()))){
				this.logError(childref.getChildRef()+"is no person", null);
				continue;
			}
			
			String userName = (String)nodeService.getProperty(childref.getChildRef(), ContentModel.PROP_USERNAME);
			
			if("guest".equals(userName)){
				this.logInfo("ignoring guest");
				continue;
			}
			
			if("System".equals(userName)){
				this.logInfo("ignoring System");
				continue;
			}
			
			if(userName == null || userName.trim().equals("")){
				this.logError("no username for "+childref.getChildRef(), null);
				continue;
			}
			
			try {
				authenticationDao.loadUserByUsername(userName);
			} catch (UsernameNotFoundException e) {
				this.logInfo("username "+userName+" exsists as person but not as user in userstore");
				missingUsers.add(userName);
			}
		}
		
		if(!test){
			UserTransaction userTransaction = serviceRegistry.getTransactionService().getNonPropagatingUserTransaction();
			
			try {
				
				userTransaction.begin();
			
				for(String missingUser : missingUsers){
					authenticationDao.createUser(missingUser, new KeyTool().getRandomPassword().toCharArray());
				}
				
				userTransaction.commit();
				
			} catch(Exception e) {
				this.logError(e.getMessage(), e);
				try {
					this.logError("trying rollback",null);
					userTransaction.rollback();
				} catch(Exception rollBackException){
					logError(rollBackException.getMessage(),rollBackException);
				}
			}
		}
	
	}
	
	@Override
	public void run() {
		this.logInfo("not implemented");
	}
	
}
