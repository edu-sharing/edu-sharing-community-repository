/**
 *
 */
package org.edu_sharing.repository.update;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * renames the value the property assignedlicense of AssignedLicense Objects with  
 * the value "ES - personal use" to an new one
 *
 * also deletes AssignedLicense Objects that got assignedlicense = "by" or "by-sa"...
 * and writes the cclicense to the property CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY
 * of th IO to a new one
 *
 */
@Slf4j
@UpdateService
public class Licenses1 {

    private final static StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");

    private final SearchService searchService;
    private final NodeService nodeService;
    private final PermissionService permService;

    @Autowired
    public Licenses1(SearchService searchService, NodeService nodeService, PermissionService permService ) {
        this.searchService = searchService;
        this.nodeService = nodeService;
        this.permService = permService;
    }

    @UpdateRoutine(
            id = "Licenses1",
            description = "renames the value the property assignedlicense of AssignedLicense Objects with the value \"ES - personal use\" to an new one also deletes AssignedLicense Objects that got assignedlicense = \"by\" or \"by-sa\"... and writes the cclicense to the property CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY of th IO to a new one",
            order = 1000
    )
    public void execute(boolean test) {
        int escounter = 0;
        int cccounter = 0;
        int notchangedcounter = 0;
        try {
            String searchString = "TYPE:\"" + CCConstants.CCM_TYPE_ASSIGNED_LICENSE + "\"";
            ResultSet resultSet = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, searchString);

            log.info("found " + resultSet.length() + " " + CCConstants.CCM_TYPE_ASSIGNED_LICENSE + "'s");
            for (NodeRef nodeRef : resultSet.getNodeRefs()) {
                String nodeType = nodeService.getType(nodeRef).toString();

                log.info("");
                log.info("*****************************************************************");
                log.info("node: " + nodeRef.getId() + " type:" + nodeType);
                log.info("*****************************************************************");

                if (!nodeType.equals(CCConstants.CCM_TYPE_ASSIGNED_LICENSE)) {
                    log.error("Update failed! " + nodeType + " is no " + CCConstants.CCM_TYPE_ASSIGNED_LICENSE
                            + " stopping update");
                    return;
                } else {

                    List list = (List) nodeService.getProperty(nodeRef, QName
                            .createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE));

                    String assignedLicense = (String) list.get(0);
                    if (assignedLicense == null) {
                        log.info("assignedLicense is null");
                        continue;
                    }

                    log.info("assigned license:" + assignedLicense);
                    if (assignedLicense.equals("ES - personal use")) {
                        escounter++;
                        log.info("it's an ES license");
                        log.info("Node: " + nodeRef.getId() + " renaming value of property "
                                + CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE + " ES - personal use to"
                                + CCConstants.COMMON_LICENSE_EDU_P_NR);
                        if (!test)
                            nodeService.setProperty(nodeRef, QName
                                            .createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE),
                                    CCConstants.COMMON_LICENSE_EDU_P_NR);

                    } else if (assignedLicense.equals("ES - publish")) {
                        escounter++;
                        log.info("it's an ES license");
                        log.info("Node: " + nodeRef.getId() + " renaming value of property "
                                + CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE + " ES - publish to"
                                + CCConstants.COMMON_LICENSE_EDU_NC);
                        if (!test)
                            nodeService.setProperty(nodeRef, QName
                                            .createQName(CCConstants.CCM_PROP_ASSIGNED_LICENSE_ASSIGNEDLICENSE),
                                    CCConstants.COMMON_LICENSE_EDU_NC);

                    } else if (assignedLicense.contains("by")) {
                        cccounter++;

                        log.info("it's an CC license");
                        ChildAssociationRef parentAssocRef = nodeService.getPrimaryParent(nodeRef);
                        NodeRef parentRef = parentAssocRef.getParentRef();
                        String parentNodeType = nodeService.getType(parentRef).toString();
                        if (!parentNodeType.equals(CCConstants.CCM_TYPE_IO)) {
                            log.error("Update failed! " + parentNodeType + " is no " + CCConstants.CCM_TYPE_IO);
                            return;
                        } else {

                            String value = null;
                            if (assignedLicense.equals("by")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY;
                            }
                            if (assignedLicense.equals("by-sa")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY_SA;
                            }
                            if (assignedLicense.equals("by-nd")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY_ND;
                            }
                            if (assignedLicense.equals("by-nc")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY_NC;
                            }
                            if (assignedLicense.equals("by-nc-sa")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY_NC_SA;
                            }
                            if (assignedLicense.equals("by-nc-nd")) {
                                value = CCConstants.COMMON_LICENSE_CC_BY_NC_ND;
                            }

                            log.info("IO:" + parentRef.getId() + " setting "
                                    + CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY + " val:" + value);
                            if (!test)
                                nodeService.setProperty(parentRef, QName
                                        .createQName(CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY), value);

                            // set Permissions
                            String[] permissionsToSet = {PermissionService.READ, CCConstants.PERMISSION_CC_PUBLISH};
                            log.info("IO:" + parentRef.getId() + " setting permissions for "
                                    + PermissionService.ALL_AUTHORITIES);
                            for (String permission : permissionsToSet) {
                                log.info("  (setting permission: " + permission + ")");
                                if (!test)
                                    permService.setPermission(parentRef, PermissionService.ALL_AUTHORITIES, permission, true);
                            }

                            // delete old
                            log.info("removing AssignedLicense:" + nodeRef.getId() + " from IO:" + parentRef.getId() + "");
                            if (!test) nodeService.removeChild(parentRef, nodeRef);
                        }

                    } else {
                        log.info("no assigned license we should change:" + assignedLicense);
                        notchangedcounter++;
                    }

                }

            }

            if (!test) {
                log.info("Update End");
                log.info("ES:" + escounter + " CC:" + cccounter + " dontneedToChange:" + notchangedcounter);
            } else {
                log.info("Update Test End. No Data changed");
                log.info("ES:" + escounter + " CC:" + cccounter + " dontneedToChange:" + notchangedcounter);
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage(), e);
        }
    }

}
