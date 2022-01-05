package org.edu_sharing.alfresco.transformer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.OpenOfficeConnectionTester;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class JodconverterConnectionTester extends OpenOfficeConnectionTester implements InitializingBean {

    private static Log logger = LogFactory.getLog(JodconverterConnectionTester.class);

    private boolean strict;
    Properties properties;
    String baseUrl;


    @Override
    protected void onBootstrap(ApplicationEvent event) {
        checkConnection();
    }

    private void checkConnection()
    {

        boolean connected = testAndConnect();
        if (connected)
        {
            logger.info("jodconverter connection is fine");
            return;
        }
        // now we have to either fail or report the connection
        String msg = "jodconverter connection test failed";
        if (strict)
        {
            throw new AlfrescoRuntimeException(msg);
        }
        else
        {
            logger.warn(msg);
        }
    }

    @Override
    protected void onShutdown(ApplicationEvent event) {
        super.onShutdown(event);
    }


    @Override
    public boolean testAndConnect() {
        logger.info("called");
        if(baseUrl == null){
            logger.error("no jodconverter configured");
            return false;
        }

        try {
            URL url = new URL(baseUrl+"/lool/convert-to");
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.connect();
            int code = connection.getResponseCode();
            /**
             * check for 405
             * when /lool/convert-to is called by GET Method an 405 is the result.
             * we don't want to do an test convert for performance reasons
             */
            if(code != 405){
                logger.error("unexpected status code:" +  code + " check service behind "+ baseUrl);
                return false;
            }
            return true;
        } catch (IOException e) {
           logger.error(e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        baseUrl = properties.getProperty("edu-sharing.jodconverter.url");
    }

    public void setStrict(boolean strict)
    {
        this.strict = strict;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
