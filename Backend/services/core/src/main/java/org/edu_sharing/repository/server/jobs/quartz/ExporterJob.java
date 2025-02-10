package org.edu_sharing.repository.server.jobs.quartz;

import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.xml.XmlWriter;
import lombok.extern.slf4j.Slf4j;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.alfresco.repo.node.MLPropertyInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.exporter.OAILOMExporter;
import org.edu_sharing.repository.server.exporter.OAILOMWithSubobjectsExporter;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.oai.OAIExporterFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;


@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "OAI Export for nodes as file")
public class ExporterJob extends AbstractJobMapAnnotationParams{




    @Autowired
    private EduMetadataFormatRegistry eduMetadataFormatRegistry;

    @Autowired
    private EduSharingItemRepository eduSharingItemRepository;

	@JobFieldDescription(description = "elastic query to fetch the nodes that shall be processed.")
	private String elasticQuery;

	@JobFieldDescription(description = "directory where oai files should be saved")
	private String outputDir;

    @JobFieldDescription(description = "Export format", sampleValue = "lom")
    public String format;


	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if (elasticQuery == null || outputDir == null) {
            return;
        }

        AuthenticationUtil.runAsSystem(() -> {
            try {
                MetadataFormat formatWriter = eduMetadataFormatRegistry.getMetadataFormat(format);
                SearchService localService = SearchServiceFactory.getLocalService();

                SearchToken searchToken = new SearchToken();
                searchToken.setFrom(0);
                searchToken.setMaxResult(Integer.MAX_VALUE);
                searchToken.setElasticQuery(QueryBuilders.wrapper().query(new String(Base64.getEncoder().encode(elasticQuery.getBytes()))).build());

                SearchResultNodeRef search = localService.search(searchToken);
                if(search != null) {
                    log.info("found " + search.getData().size() + " to export with " + elasticQuery);
                    for(NodeRef nodeRef : search.getData()){
                        String nodeId = nodeRef.getNodeId();
                        try(FileOutputStream os = new FileOutputStream(outputDirectory + "/" + nodeId + ".xml")) {
                            Item item = eduSharingItemRepository.getItem(nodeId, formatWriter);
                            try (XmlWriter writer = new XmlWriter(os)) {
                                item.getMetadata().write(writer);
                            }
                        }
                    }
                }else{
                    log.info("found nothing with: " + elasticQuery);
                }
            }catch(Throwable e){
                log.error(e.getMessage(), e);
            }
            return null;
        });

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}

}
