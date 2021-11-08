package org.edu_sharing.alfresco.transformer;

import net.sf.jooreports.converter.DocumentFormat;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.repo.content.transform.OOoContentTransformerHelper;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.edu_sharing.jodconverter.client.ApiClient;
import org.edu_sharing.jodconverter.client.ApiException;
import org.edu_sharing.jodconverter.client.api.ConverterControllerApi;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class JodconverterTransformerWorker extends OOoContentTransformerHelper implements ContentTransformerWorker, InitializingBean{

    private static Log logger = LogFactory.getLog(JodconverterTransformerWorker.class);

    Properties properties;

    String baseUrl;

    public JodconverterTransformerWorker(){
        logger.info("edu-sharing "+this.getClass().getSimpleName()+" loaded");
    }

    @Override
    protected void convert(File tempFromFile, DocumentFormat sourceFormat, File tempToFile, DocumentFormat targetFormat) {
        try {
            ApiClient apiClient = new ApiClient().setBasePath(baseUrl);
            ConverterControllerApi converterControllerApi = new ConverterControllerApi(apiClient);

            logger.info("sourceFormat:"+sourceFormat +" targetFormat:"+targetFormat +" from:"+tempFromFile +" to:"+tempToFile);
            byte[] result = converterControllerApi.convertToUsingPathUsingPOST(tempFromFile, targetFormat.getFileExtension(), null);
            FileOutputStream fos = new FileOutputStream(tempToFile);
            fos.write(result);
            fos.close();
        } catch (ApiException|IOException e) {
            logger.error("Problems reaching Jodconverter "+baseUrl +": "+e.getMessage(),e);
        }catch (Throwable e){
            logger.error(e.getMessage(),e);
        }

    }

    @Override
    public boolean isAvailable() {
        /**
         * @TODO check for Jodconverter
         */
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        baseUrl = properties.getProperty("edu-sharing.jodconverter.url");
        if(baseUrl == null){
            baseUrl = "http://localhost:8080/";
        }
    }

    @Override
    public void saveContentInFile(String sourceMimetype, ContentReader reader, File file) throws ContentIOException {
        super.saveContentInFile(sourceMimetype, reader, file);
    }

    @Override
    public Log getLogger() {
        return logger;
    }

    @Override
    protected String getTempFilePrefix()
    {
        return "OpenOfficeContentTransformer";
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
