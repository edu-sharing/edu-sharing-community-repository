package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.PersistenHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.net.URI;
import java.net.URISyntaxException;

@JobDescription(description = "dnb authorities-sachbegriff, full import of marc 21 xml format.")
public class ImportFactualTermMarc21 extends AbstractJob{

    Logger logger = Logger.getLogger(ImportFactualTermMarc21.class);

    @JobFieldDescription(description = "uri - http or file - to marc 21 file in xml format. i.e.: \"file://localhost/authorities-sachbegriff_dnbmarc_20201013.mrc.xml\"")
    private String fileUri;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        fileUri = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("fileUri");

        URI url = null;
        try {
            url = new URI(fileUri);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(),e);
            return;
        }



        try {

            new ImportFactualTermsFromFileSax(url,new PersistenHandlerKeywordsDNBMarc(), this);
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    @Override
    public Class[] getJobClasses() {
        return super.getJobClasses();
    }
}
