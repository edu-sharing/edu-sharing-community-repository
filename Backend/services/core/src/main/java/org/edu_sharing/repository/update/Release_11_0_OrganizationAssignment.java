package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.organization.OrganizationService;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_OrganizationAssignment {


    private final NodeService nodeService;
    private final OwnableService ownableService;
    private final OrganizationService organizationService;
    private final PersonService personService;

    @UpdateRoutine(
            id = "Release_11_0_OrganizationAssignment",
            description = "Migrate all materials by assigning the organization of the current owner",
            isNonTransactional = true,
            order = 0
    )
    public void execute() {
        NodeRunner runner = new NodeRunner();
        runner.setRunAsSystem(true);
        runner.setTypes(List.of(CCConstants.CCM_TYPE_IO));
        runner.setThreaded(false);
        runner.setTransaction(NodeRunner.TransactionMode.Local);
        runner.setKeepModifiedDate(true);
        runner.setRecurseMode(RecurseMode.All);
//        runner.setFilter(nodeService::exists);
        runner.setTask(nodeRef -> {
            log.info("Processing {}", nodeRef);

            String currentUser = ownableService.getOwner(nodeRef);
            if(currentUser == null) {
                currentUser = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);
            }

            if(!personService.personExists(currentUser)) {
                log.warn("Person {} does not exist", currentUser);
                return;
            }

            QName organizationQName = QName.createQName(CCConstants.CCM_PROP_OWNING_ORGANIZATION);
            List<String> knownOrganizations = (List<String>)nodeService.getProperty(nodeRef, organizationQName);
            if(knownOrganizations != null && !knownOrganizations.isEmpty()) {
                log.info("Node {} already has assigned organizations {}", nodeRef, knownOrganizations);
                return;
            }

            Set<String> organisations = organizationService.getAssignedOrganisations(currentUser);
            if (organisations.isEmpty()) {
                log.warn("Person {} has no assigned organisations", currentUser);
                return;
            }

           nodeService.addProperties(nodeRef, Map.of(organizationQName, (Serializable) organisations.stream().toList()));
            log.info("Assigned organization {} to node {}", organisations, nodeRef);
        });


        log.info("Starting organization assignment");
        int processed = runner.run();
        log.info("Organization assignment for all nodes updated. Processed: {}", processed);
    }
}
