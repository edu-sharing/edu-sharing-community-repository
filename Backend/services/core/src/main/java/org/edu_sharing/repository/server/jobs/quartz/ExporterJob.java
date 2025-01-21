package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Base64;

import javax.xml.parsers.ParserConfigurationException;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.SearchResultNodeRef;
import org.edu_sharing.repository.server.exporter.OAILOMWithSubobjectsExporter;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.model.NodeRef;
import org.edu_sharing.service.oai.OAIExporterFactory;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceFactory;
import org.edu_sharing.service.search.model.SearchToken;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@JobDescription(description = "OAI Export for nodes as file")
public class ExporterJob extends AbstractJobMapAnnotationParams{



	Log logger = LogFactory.getLog(ExporterJob.class);

	@JobFieldDescription(description = "elastic query to fetch the nodes that shall be processed.")
	private String elasticQuery;

	@JobFieldDescription(description = "directory where oai files should be saved")
	private String outputDir;

	@JobFieldDescription(description = "if subobjects should be used")
	private boolean withSubObjects;


	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		if (elasticQuery != null && outputDir != null) {
			AuthenticationUtil.runAsSystem(() -> {
				try {
					SearchService localService = SearchServiceFactory.getLocalService();

					SearchToken searchToken = new SearchToken();
					searchToken.setFrom(0);
					searchToken.setMaxResult(Integer.MAX_VALUE);
					searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(elasticQuery.getBytes()))).build());

					SearchResultNodeRef search = localService.search(searchToken);
					if(search != null) {
						logger.info("found " + search.getData().size() + " to export with " + elasticQuery);
						for(NodeRef nodeRef : search.getData()){
							String nodeId = nodeRef.getNodeId();
							if(withSubObjects){
								new OAILOMWithSubobjectsExporter(nodeId).export(outputDir);
							}else{
								OAIExporterFactory.getOAILOMExporter().export(outputDir,nodeId);
							}
						}
					}else{
						logger.info("found nothing with: " + elasticQuery);
					}

				} catch (ParserConfigurationException e) {
					logger.error(e.getMessage(), e);
				} catch(Throwable e){
					logger.error(e.getMessage(), e);
				}
				return null;
			});
		}
	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}

}
