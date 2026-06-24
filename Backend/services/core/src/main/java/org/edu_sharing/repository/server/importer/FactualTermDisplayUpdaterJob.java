package org.edu_sharing.repository.server.importer;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.repository.server.jobs.quartz.AbstractJobMapAnnotationParams;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "resolves nodes with changed factualTerms and touch nodes so they get re-indexed by search index")
public class FactualTermDisplayUpdaterJob extends AbstractJobMapAnnotationParams {

    @Autowired
    FactualTermElasticUpdater factualTermElasticUpdater;

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            AuthenticationUtil.runAsSystem(() ->{
                PersistenHandlerKeywordsDNBMarc ph = new PersistenHandlerKeywordsDNBMarc();
                List<String> changedIdents = ph.getChangedIdents();
                for(String key : changedIdents) {
                    factualTermElasticUpdater.touchNodes(key);
                    ph.resetModified(key);
                }
                return null;
            });
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
    }
}
