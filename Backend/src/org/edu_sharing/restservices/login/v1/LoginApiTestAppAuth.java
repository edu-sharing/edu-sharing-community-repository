package org.edu_sharing.restservices.login.v1;

import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.login.v1.model.AuthenticationToken;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Properties;
import java.util.logging.Logger;

public class LoginApiTestAppAuth {

    public static void main(String[] args) {
        java.util.logging.Logger jaxlogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        Client client = ClientBuilder.newClient(new ClientConfig().register(new LoggingFilter(jaxlogger,false)));

        String username = "admin";
        try {
            Properties props = PropertiesHelper.getProperties("org/edu_sharing/restservices/login/v1/LoginApiTestAppAuth.properties.xml",PropertiesHelper.XML);
            String appId = props.getProperty(ApplicationInfo.KEY_APPID);

            Signing signing = new Signing();
            String timestamp = ""+System.currentTimeMillis();
            String signData = username + appId + timestamp;

            byte[] signature = signing.sign(signing.getPemPrivateKey(props.getProperty(ApplicationInfo.KEY_PRIVATE_KEY), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
            signature = new Base64().encode(signature);

            WebTarget webTarget = client.target("http://localhost:8080/edu-sharing/rest/");
            WebTarget currentWebTarget = webTarget.path("authentication/v1/appauth").path(username);
            Response response = currentWebTarget
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .header("X-Edu-App-Id", appId)
                    .header("X-Edu-App-Sig",new String(signature))
                    .header("X-Edu-App-Signed",signData)
                    .header("X-Edu-App-Ts",timestamp)
                    .post(null);

            System.out.println(response.getStatus());
            AuthenticationToken token = response.readEntity(AuthenticationToken.class);
            System.out.println(token.getTicket());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
