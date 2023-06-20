package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.PersonService;
import org.edu_sharing.alfresco.policy.HomeFolderTool;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;


@Slf4j
@UpdateService
public class Edu_SharingAuthoritiesUpdate {

	private final PersonService personService;
	private final HomeFolderTool homeFolderTool;

	public static final String ID = "Edu_SharingAuthoritiesUpdate";
	
	public static final String description = "Creates edu-sharing folders in exsisting userhomes." ;

	@Autowired
	public Edu_SharingAuthoritiesUpdate(PersonService personService, HomeFolderTool homeFolderTool) {
		this.personService = personService;
		this.homeFolderTool = homeFolderTool;
	}

	@UpdateRoutine(id="Edu_SharingAuthoritiesUpdate",
	description = "Creates edu-sharing folders in exsisting userhomes.",
	order = 1703)
	public void execute(boolean test){
		
		Set<NodeRef> allPeople = personService.getAllPeople();
		int counter = 0;
		for(NodeRef personRef : allPeople){
			if(!test){
				homeFolderTool.constructPersonFolders(personRef);
			}
			counter++;
			if((counter % 100) == 0){
				log.debug("processed "+ counter +" persons");
			}
		}
	}

}
