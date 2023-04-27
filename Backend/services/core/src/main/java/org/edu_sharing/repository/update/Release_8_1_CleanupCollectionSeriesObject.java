package org.edu_sharing.repository.update;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.service.collection.CollectionServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Release_8_1_CleanupCollectionSeriesObject extends UpdateAbstract {
	NodeService nodeService = serviceRegistry.getNodeService();

	@Override
	public String getId() {
		return "Release_8_1_CleanupCollectionSeriesObject";
	}

	@Override
	public String getDescription() {
		return "Removing all child object duplicates in collections, they're now always handled via the original element";
	}


	public Release_8_1_CleanupCollectionSeriesObject(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(Release_8_1_CleanupCollectionSeriesObject.class);
	}
	@Override
	public void execute() {
		executeWithProtocolEntryNoGlobalTx();
	}
	@Override
	public boolean runAndReport() {
		int count = doTransform((ref) -> {
			logInfo("Deleting " + ref);
			NodeServiceFactory.getLocalService().removeNode(ref.getId(), null, false);
		});
		logInfo("Deleted " + count + " objects");
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

	@Override
	public void test() {
		int count = doTransform((ref) -> {
			logInfo("Would delete " + ref);
		});
		logInfo("Would delete " + count + " objects");
	}

}
