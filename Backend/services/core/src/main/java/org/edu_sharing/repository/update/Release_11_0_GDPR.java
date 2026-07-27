package org.edu_sharing.repository.update;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.alfresco.service.ConnectionDBAlfresco;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;

@Slf4j
@UpdateService
@RequiredArgsConstructor
public class Release_11_0_GDPR {


    private final NodeService nodeService;

    @UpdateRoutine(
            id = "Release_11_0_GDPR",
            description = "Migrate edu_dataprotection_queue to aspects",
            isNonTransactional = true,
            order = 0,
            auto = true)
    public void execute() {

        ConnectionDBAlfresco dbAlf = new ConnectionDBAlfresco();

        try (Connection connection = dbAlf.getConnection()) {

            //language=SQL
            java.sql.DatabaseMetaData dbm = connection.getMetaData();
            java.sql.ResultSet tables = dbm.getTables(null, null, "edu_dataprotection_queue", null);
            if (!tables.next()) {
                log.info("No edu_dataprotection_queue table found, skipping");
                return;
            }

            //language=SQL
            try (PreparedStatement statement = connection.prepareStatement("SELECT node_id FROM edu_dataprotection_queue WHERE node_id NOTNULL")) {
                java.sql.ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String nodeId = resultSet.getString(1);
                    NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeId);
                    nodeService.addAspect(nodeRef, QName.createQName(CCConstants.CCM_ASPECT_GDPR), Collections.emptyMap());
                }
            }

            //language=SQL
            try (PreparedStatement statement = connection.prepareStatement("DROP TABLE edu_dataprotection_queue")) {
                statement.execute();
                connection.commit();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
