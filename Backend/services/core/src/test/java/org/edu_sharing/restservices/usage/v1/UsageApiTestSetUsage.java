package org.edu_sharing.restservices.usage.v1;

import org.apache.commons.codec.binary.Base64;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.PropertiesHelper;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.login.v1.model.AuthenticationToken;
import org.edu_sharing.restservices.login.v1.model.Login;
import org.edu_sharing.restservices.shared.UserProfileAppAuth;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Properties;
import java.util.logging.Logger;
import org.glassfish.jersey.logging.LoggingFeature;

public class UsageApiTestSetUsage {

    public static void main(String[] args) {
        java.util.logging.Logger jaxlogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        Client client = ClientBuilder.newClient(new ClientConfig().register(new LoggingFeature(jaxlogger)));

        String username = "admin";
        try {

            /***
             * appAuth
             */
            Properties props = PropertiesHelper.getProperties("org/edu_sharing/restservices/login/v1/LoginApiTestAppAuth.properties.xml",PropertiesHelper.XML);
            String appId = props.getProperty(ApplicationInfo.KEY_APPID);

            Signing signing = new Signing();
            String timestamp = ""+System.currentTimeMillis();
            String signData = username + appId + timestamp;

            byte[] signature = signing.sign(signing.getPemPrivateKey(props.getProperty(ApplicationInfo.KEY_PRIVATE_KEY), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
            signature = new Base64().encode(signature);

            WebTarget webTarget = client.target("http://localhost:8080/edu-sharing/rest/");
            WebTarget currentWebTarget = webTarget.path("authentication/v1/appauth").path(username);

            UserProfileAppAuth up = new UserProfileAppAuth();
            up.setFirstName("Dan");
            up.setLastName("rud");
            up.setEmail("test@d.de");

            //additionalAttributes bean must be set in edu-sharing-sso-context.xml
            up.getExtendedAttributes().put("eduPersonScopedAffiliation",new String[]{"student"});

            Entity<UserProfileAppAuth> e = Entity.entity(up, MediaType.APPLICATION_JSON);

            System.out.println(e.toString());

            String signatureString = new String(signature);
            Response response = currentWebTarget
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Edu-App-Id", appId)
                    .header("X-Edu-App-Sig",signatureString)
                    .header("X-Edu-App-Signed",signData)
                    .header("X-Edu-App-Ts",timestamp)
                    .post(e);

            System.out.println(response.getStatus()+" "+response.getStatusInfo());
            if(response.getStatus() != 200) {
                System.out.println("ERROR:" + response.getStatus() +" "+ response.readEntity( String.class ));
                return;
            }

            AuthenticationToken token = response.readEntity(AuthenticationToken.class);
            System.out.println(token.getUserId() + " " + token.getTicket());


            currentWebTarget = webTarget.path("authentication/v1/validateSession");
            response = currentWebTarget
                    .request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization",CCConstants.AUTH_HEADER_EDU_TICKET + token.getTicket()).get();

            Login l = response.readEntity(Login.class);

            System.out.println("valid ticket:" + l.isValidLogin());

            /**
             * set Usage
             *
             */
            //new signature cause of timstamp
            timestamp = ""+System.currentTimeMillis();
            signData = username + appId + timestamp;

            signature = signing.sign(signing.getPemPrivateKey(props.getProperty(ApplicationInfo.KEY_PRIVATE_KEY), CCConstants.SECURITY_KEY_ALGORITHM), signData, CCConstants.SECURITY_SIGN_ALGORITHM);
            signature = new Base64().encode(signature);
            signatureString = new String(signature);

            Usages.Usage u = new Usages.Usage();
            u.setAppId(appId);
            u.setAppUser(token.getUserId());
            u.setCourseId("123");
            u.setAppType(ApplicationInfo.TYPE_LMS);
            u.setAppSubtype("moodle");
            u.setParentNodeId("2a464068-390b-460a-8c4a-39531a44463d");
            u.setResourceId("1");

            Entity< Usages.Usage> entityUsage = Entity.entity(u, MediaType.APPLICATION_JSON);

            currentWebTarget = webTarget.path("usage/v1/usages/repository/-home-");
            response = currentWebTarget.request(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Edu-App-Id", appId)
                    .header("X-Edu-App-Sig",signatureString)
                    .header("X-Edu-App-Signed",signData)
                    .header("X-Edu-App-Ts",timestamp)
                    .header("Authorization",CCConstants.AUTH_HEADER_EDU_TICKET + token.getTicket())
                    .post(entityUsage);

            System.out.println("usage respons status:"+response.getStatus()+" "+response.getStatusInfo());
            Usages.Usage result = response.readEntity(Usages.Usage.class);
            System.out.println("created usage. usage has alf nodeId: "+result.getNodeId());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
