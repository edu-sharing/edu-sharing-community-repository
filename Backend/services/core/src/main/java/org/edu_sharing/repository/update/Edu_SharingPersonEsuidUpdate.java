package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;

@Slf4j
@UpdateService
public class Edu_SharingPersonEsuidUpdate {

	private final NodeService nodeService;
	private final PersonService personService;


	@Autowired
	public Edu_SharingPersonEsuidUpdate(NodeService nodeService, PersonService personService) {
		this.nodeService = nodeService;
		this.personService = personService;
	}

	@UpdateRoutine(
			id = "Edu_SharingPersonEsuidUpdate",
			description = "Creates esuids for all persons.",
			order = 1803
	)
	public void execute(boolean test) {
		Set<NodeRef> allPeople = personService.getAllPeople();
		int counter = 0;
		for(NodeRef personRef : allPeople){
			
			if(!test){
				
				//nodeService.setProperty(personRef, ContentModel.PROP_FIRSTNAME, nodeService.getProperty(personRef, ContentModel.PROP_FIRSTNAME));
				
				UUID uuid = UUID.randomUUID();
				QName esUidQName =  QName.createQName(CCConstants.PROP_USER_ESUID);
				if(nodeService.getProperty(personRef, esUidQName) == null){
					nodeService.setProperty(personRef, esUidQName, uuid.toString());
				}
				
			}
			counter++;
			if((counter % 100) == 0){
				log.info("processed "+ counter +" persons");
			}
		}
	}
}
