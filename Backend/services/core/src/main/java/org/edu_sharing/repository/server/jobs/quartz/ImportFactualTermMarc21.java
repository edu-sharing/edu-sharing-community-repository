package org.edu_sharing.repository.server.jobs.quartz;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.server.importer.PersistenHandlerKeywordsDNBMarc;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

@JobDescription(description = "dnb authorities-sachbegriff, full import of marc 21 xml format.")
public class ImportFactualTermMarc21 extends AbstractJob{

    Logger logger = Logger.getLogger(ImportFactualTermMarc21.class);

    @JobFieldDescription(description = "uri - http or file - to marc 21 file in xml format. i.e.: \"file://localhost/authorities-sachbegriff_dnbmarc_20201013.mrc.xml\"")
    private String fileUri;

    @JobFieldDescription(description = "checks for entries in the database to only rune once. default is \"false\"")
    private boolean checkAlreadyDone = false;


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {


        fileUri = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("fileUri");
        String pCheckDone = (String) jobExecutionContext.getJobDetail().getJobDataMap().get("checkAlreadyDone");
        checkAlreadyDone = Boolean.parseBoolean(pCheckDone);


        URI url = null;
        try {
            url = new URI(fileUri);
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(),e);
            return;
        }

        if(checkAlreadyDone){
            long entries = new PersistenHandlerKeywordsDNBMarc().countEntries();
            if(entries != 0){
                logger.info("abort import. db entries:" + entries);
                return;
            }
        }

        if(fileUri.endsWith(".gz")){
            logger.info(".gz file ending, have to decompresss:" + fileUri);
            try {
                Path tempFile = Files.createTempFile("authorities-gnd-sachbegriff_dnbmarc.mrc", ".xml");

                try(
                        FileOutputStream fosTarget = new FileOutputStream(tempFile.toFile());
                        GZIPInputStream gis = new GZIPInputStream(url.toURL().openStream());
                ){
                    IOUtils.copy(gis,fosTarget);
                }

                url = tempFile.toFile().toURI();

            } catch (IOException e) {
                logger.error(e.getMessage(),e);
                return;
            }

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
