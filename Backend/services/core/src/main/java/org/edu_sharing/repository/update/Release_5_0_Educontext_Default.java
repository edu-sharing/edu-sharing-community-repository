package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@UpdateService
public class Release_5_0_Educontext_Default {

    private final NodeService nodeService;

    @Autowired
    public Release_5_0_Educontext_Default(NodeService nodeService) {
        this.nodeService = nodeService;
    }


    @UpdateRoutine(
            id = "Release_5_0_Educontext_Default",
            description = "add the default value \"" + CCConstants.EDUCONTEXT_DEFAULT + "\" into the new field " + CCConstants.CCM_PROP_EDUCONTEXT_NAME,
            order = 5000
    )
    public boolean execute(boolean test) {
        NodeRunner runner = new NodeRunner();
        runner.setRunAsSystem(true);
        runner.setTypes(CCConstants.EDUCONTEXT_TYPES);
        runner.setThreaded(false);
        runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
        runner.setKeepModifiedDate(true);
        runner.setRecurseMode(RecurseMode.All);
        int[] processed = new int[]{0};
        AtomicBoolean result = new AtomicBoolean(true);
        runner.setFilter((ref) -> {
            try {
                if (ref == null) {
                    return false;
                }
                if (!nodeService.exists(ref)) {
                    return false;
                }
                Serializable prop = nodeService.getProperty(ref, QName.createQName(CCConstants.CCM_PROP_EDUCONTEXT_NAME));
                // we want to return true for all nodes which don't have the property set yet
                if (prop == null)
                    return true;
                if (prop instanceof String) {
                    return ((String) prop).isEmpty();
                } else if (prop instanceof List) {
                    return ((List) prop).isEmpty();
                } else
                    return true;
            } catch (Throwable e) {
                log.error("error filtering node:" + ref + " " + e.getMessage(), e);
                result.set(false);
                return false;
            }
        });
        runner.setTask((ref) -> {
            try {
                if (!nodeService.exists(ref)) {
                    return;
                }
                log.info("add educontext to " + ref.getId());
                if (!test) {
                    nodeService.setProperty(ref, QName.createQName(CCConstants.CCM_PROP_EDUCONTEXT_NAME), CCConstants.EDUCONTEXT_DEFAULT);
                }
                processed[0]++;
            } catch (Throwable e) {
                result.set(false);
                log.error("error processing node:" + ref + " " + e.getMessage(), e);
            }
        });
        runner.run();
        log.info("Added educontext default value to a total of " + processed[0] + " nodes. success:" + result.get());
        return result.get();
    }


}
