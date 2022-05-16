package org.edu_sharing.repository.server.importer;

import org.edu_sharing.repository.server.jobs.quartz.AbstractJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

public class FactualTermDisplayUpdaterJob extends AbstractJob {



    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            PersistenHandlerKeywordsDNBMarc ph = new PersistenHandlerKeywordsDNBMarc();
            FactualTermDisplayUpdater updater = new FactualTermDisplayUpdater();
            List<String> changedIdents = ph.getChangedIdents();
            for(String key : changedIdents) {
                updater.updateDisplayStrings(key);
                ph.resetModified(key);
            }
        } catch (Exception e) {
           logger.error(e.getMessage(),e);
        }

    }
}
