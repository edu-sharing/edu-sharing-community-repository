package org.edu_sharing.repository.update;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.Edu_SharingProperties;
import org.springframework.context.ApplicationContext;

public class Release_4_2_PersonStatusUpdater extends UpdateAbstract{

	public static final String ID = "Release_4_2_PersonStatusUpdater";
	
	public static final String description = "when person_active_status is set in edu-sharing.properties set this value for existing person objects." ;
	
	String personActiveStatus = null;
	
	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);
	PersonService personService = serviceRegistry.getPersonService();
	
	public Release_4_2_PersonStatusUpdater(PrintWriter out) {
		this.out = out;
		logger = Logger.getLogger(Release_4_2_PersonStatusUpdater.class);
		personActiveStatus = Edu_SharingProperties.instance.getPersonActiveStatus();
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

	@Override
	public void run() {
		RunAsWork<Void> runAs = new RunAsWork<Void>() {
			@Override
			public Void doWork() throws Exception {
			   
				List<PersonInfo> people = getAllPeople();
				for(PersonInfo personInfo : people) {
		    		logInfo("updateing person: " + personInfo.getUserName());
		    		serviceRegistry.getNodeService().setProperty(personInfo.getNodeRef(), QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS), personActiveStatus);
		    	}
				
				return null;
		       
			}
		};
		AuthenticationUtil.runAsSystem(runAs);
	}
	
	private  List<PersonInfo> getAllPeople() {
		return personService.getPeople(null, null, null, new PagingRequest(Integer.MAX_VALUE, null)).getPage();
	}
	
	@Override
	public void test() {
		logInfo("not implemented");
		
	}
	
	@Override
	public void execute() {
		
		if(personActiveStatus != null 
				&& !personActiveStatus.trim().equals("") ) {
			this.executeWithProtocolEntry();
		}
		
	}
	
	
	
}
