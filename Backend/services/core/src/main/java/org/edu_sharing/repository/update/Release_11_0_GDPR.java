package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.share.GlobalShareService;
import org.edu_sharing.service.share.ShareInfoServiceImpl;
import org.edu_sharing.service.share.ShareType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_GDPR {


    private final NodeService nodeService;

    @UpdateRoutine(
            id = "Release_11_0_GDPR",
            description = "Migrate edu_dataprotection_queue to aspects",
            order = 0,
            auto = true)
    public void execute() {

        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();
        //language=SQL
        try (Connection connection = dbAlf.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("SELECT node_id FROM edu_dataprotection_queue WHERE node_id NOTNULL")) {
                java.sql.ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String nodeId = resultSet.getString(1);
                    NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
                    nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_GDPR), Collections.emptyMap());
                }
            }

            try(PreparedStatement statement = connection.prepareStatement("DROP TABLE edu_dataprotection_queue")) {
                statement.execute();
                connection.commit();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
