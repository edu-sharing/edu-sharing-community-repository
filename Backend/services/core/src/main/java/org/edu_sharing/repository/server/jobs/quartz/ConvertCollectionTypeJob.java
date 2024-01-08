/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryParser.QueryParser;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataSet;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.permission.PermissionServiceHelper;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JobDescription(description = "Migrate the collection type of a whole collection tree")
public class ConvertCollectionTypeJob extends AbstractJobMapAnnotationParams{
	public enum Mode {
		@JobFieldDescription(description = "convert regular collections to editorial ones")
		DefaultToEditorial,
		@JobFieldDescription(description = "convert editorial collections to regular ones", sampleValue = "true")
		EditorialToDefault
	}

	protected static Logger logger = Logger.getLogger(ConvertCollectionTypeJob.class);
	@JobFieldDescription(description = "The collection id to start from (all subcollections from here will be converted)")
	private String collectionId;
	@JobFieldDescription(description = "The mode to use")
	private Mode mode;
	@JobFieldDescription(description = "Make the collections public? (recommended if converting to editorial)")
	private Boolean makePublic;

	ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();
	ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

	@Override
	public void executeInternal(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.runAsSystem(() -> {
			if (collectionId == null || mode == null) {
				throw new IllegalArgumentException("collectionId + mode are mandatory");
			}
			if (!NodeServiceHelper.hasAspect(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId), CCConstants.CCM_ASPECT_COLLECTION)) {
				throw new IllegalArgumentException("collectionId is not of type " + CCConstants.CCM_ASPECT_COLLECTION);
			}
			NodeRunner runner = new NodeRunner();
			runner.setRunAsSystem(true);
			runner.setTransaction(NodeRunner.TransactionMode.Local);
			runner.setKeepModifiedDate(true);
			runner.setThreaded(false);
			runner.setTypes(Collections.singletonList(CCConstants.CCM_TYPE_MAP));
			runner.setTask(this::convert);
			runner.setStartFolder(collectionId);
			runner.run();
			convert(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, collectionId));
			return null;
		});
	}

	private void convert(NodeRef nodeRef) {
		if(!NodeServiceHelper.hasAspect(nodeRef, CCConstants.CCM_ASPECT_COLLECTION)) {
			logger.error("Element " + nodeRef + " is not a collection!");
			return;
		}
		if(mode.equals(Mode.DefaultToEditorial)) {
			NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_MAP_COLLECTIONTYPE, CCConstants.COLLECTIONTYPE_EDITORIAL);
		} else if(mode.equals(Mode.EditorialToDefault)) {
			NodeServiceHelper.setProperty(nodeRef, CCConstants.CCM_PROP_MAP_COLLECTIONTYPE, CCConstants.COLLECTIONTYPE_DEFAULT);
		}
		if(makePublic != null && makePublic) {
			serviceRegistry.getPermissionService().setPermission(nodeRef, CCConstants.AUTHORITY_GROUP_EVERYONE, CCConstants.PERMISSION_CONSUMER, true);
		}
		logger.info("Processed collection " + nodeRef);
	}
}
