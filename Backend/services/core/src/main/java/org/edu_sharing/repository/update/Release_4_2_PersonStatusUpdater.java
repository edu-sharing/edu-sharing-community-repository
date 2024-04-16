package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.query.PagingRequest;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.rest.framework.core.exceptions.InvalidArgumentException;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.security.PersonService.PersonInfo;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@UpdateService
public class Release_4_2_PersonStatusUpdater {

    private final String personActiveStatus;
    private final PersonService personService;
    private final NodeService nodeService;
    private final QName prop = QName.createQName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS);

    @Autowired
    public Release_4_2_PersonStatusUpdater(PersonService personService, NodeService nodeService) {
        this.personService = personService;
        this.nodeService = nodeService;

        if (!LightbendConfigLoader.get().getIsNull("repository.personActiveStatus")) {
            personActiveStatus = LightbendConfigLoader.get().getString("repository.personActiveStatus");
        }else {
            personActiveStatus = null;
        }
    }


    @UpdateRoutine(
            id = "Release_4_2_PersonStatusUpdater",
            description = "when personActiveStatus is set in config set this value for existing person objects.",
            order = 200000,
            auto = true
    )
    public void execute() {
        if (StringUtils.isBlank(personActiveStatus)) {
            throw new InvalidArgumentException("personActiveStatus not set in config");
        }

        AuthenticationUtil.runAsSystem(() -> {
            List<PersonInfo> people = getAllPeople();
            for (PersonInfo personInfo : people) {
                if (nodeService.getProperty(personInfo.getNodeRef(), prop) == null) {
                    log.info("updating person: " + personInfo.getUserName());
                    nodeService.setProperty(personInfo.getNodeRef(), prop, personActiveStatus);
                }
            }

            return null;

        });
    }

    private List<PersonInfo> getAllPeople() {
        return personService.getPeople(null, null, null, new PagingRequest(Integer.MAX_VALUE, null)).getPage();
    }
}
