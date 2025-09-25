package org.edu_sharing.repository.server.jobs.quartz;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.cache.RepositoryCache;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "create initial version for ccm:io nodes if they don't exist")
public class FixInitialVersion extends AbstractJobMapAnnotationParams{



	@Autowired private VersionService versionService;
	@Autowired private org.edu_sharing.service.search.SearchService searchService;
	@Autowired private NodeService alfrescoDefaultDbNodeService;
	@Autowired private BehaviourFilter policyBehaviourFilter;
	@Autowired private RepositoryCache repositoryCache;
	@Autowired private RetryingTransactionHelper retryingTransactionHelper;

	@JobFieldDescription(description = "if false job just logs which nodes are found and would be updated")
	boolean persistentMode = false;

	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		AuthenticationUtil.RunAsWork<Void> runAs = () -> {
            process();
            return null;
        };
		AuthenticationUtil.runAsSystem(runAs);
		
		
	}
	
	private void process() {

		SearchToken searchToken = new SearchToken();
		searchToken.setFrom(0);
		searchToken.setMaxResult(Integer.MAX_VALUE);
		searchToken.setElasticQuery(QueryBuilders.bool()
				.mustNot(m -> m.exists(e -> e.field("properties.cclom:version")))
				.must(m -> m.term(t -> t.field("type").value("ccm:io")))
				.build());

		SearchResultNodeRef search = searchService.search(searchToken);
		log.info("found: " + search.getNodeCount());
		search.getData().forEach(n -> {
			NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, n.getNodeId());
			VersionHistory vh = versionService.getVersionHistory(nodeRef);
			if(vh == null) {
				log.info("creating initial version for:" + nodeRef +"  " + alfrescoDefaultDbNodeService.getProperty(nodeRef, ContentModel.PROP_NAME));

				Map<String, Serializable> transFormedProps = transformQNameKeyToString(alfrescoDefaultDbNodeService.getProperties(nodeRef));
				transFormedProps.put(VersionModel.PROP_VERSION_TYPE, VersionType.MAJOR);

				retryingTransactionHelper.doInTransaction((RetryingTransactionCallback<Void>) () -> {
                    if(persistentMode) {
                        policyBehaviourFilter.disableBehaviour(nodeRef);
						Version version = versionService.createVersion(nodeRef, transFormedProps);
						this.alfrescoDefaultDbNodeService.setProperty(nodeRef, QName.createQName(CCConstants.LOM_PROP_LIFECYCLE_VERSION), version.getVersionLabel());
						repositoryCache.remove(nodeRef.getId());
						policyBehaviourFilter.enableBehaviour(nodeRef);
                        return null;
                    }
                    return null;
                });
			}
		});
	}
	
	Map<String,Serializable> transformQNameKeyToString(Map<QName, Serializable> props){
		Map<String,Serializable> result = new HashMap<>();
		for(Map.Entry<QName,Serializable> entry : props.entrySet()){
			result.put(entry.getKey().toString(), entry.getValue());
		}
		return result;
	}
	
	
	@Override
	public Class<?>[] getJobClasses() {
		super.addJobClass(FixInitialVersion.class);
		return allJobs;
	}
}
