package org.edu_sharing.repository.update;

import lombok.extern.slf4j.Slf4j;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.update.UpdateRoutine;
import org.edu_sharing.repository.server.update.UpdateService;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
@UpdateService
public class Release_8_1_CleanupCollectionSeriesObject {
	
	@UpdateRoutine(
			id= "Release_8_1_CleanupCollectionSeriesObject",
			description = "Removing all child object duplicates in collections, they're now always handled via the original element",
			order = 8100,
			isNonTransactional = true)
	public boolean execute(boolean test) {
		int count = doTransform((ref) -> {
			log.info("Deleting " + ref);
			if(!test) {
				NodeServiceFactory.getLocalService().removeNode(ref.getId(), null, false);
			}
		});
		log.info("Deleted " + count + " objects");
		return true;
	}

	private int doTransform(Consumer<NodeRef> task) {
		NodeRunner runner = new NodeRunner();
		runner.setRunAsSystem(true);
		runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_IO));
		runner.setRecurseMode(RecurseMode.All);
		runner.setThreaded(false);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.LocalRetrying);
		runner.setStartFolder(CollectionServiceFactory.getLocalService().getCollectionHomeParent());
		AtomicInteger count = new AtomicInteger();
		runner.setFilter((ref) -> {
			if(NodeServiceHelper.hasAspect(ref, CCConstants.CCM_ASPECT_IO_CHILDOBJECT) &&
					NodeServiceHelper.hasAspect(
							NodeServiceHelper.getPrimaryParent(ref),
							CCConstants.CCM_ASPECT_COLLECTION_IO_REFERENCE
					)) {
				count.incrementAndGet();
				return true;
			}
			return false;
		});
		runner.setTask(task);
		runner.run();
		return count.get();
	}
}
