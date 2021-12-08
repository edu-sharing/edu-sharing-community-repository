package org.edu_sharing.service.lti13;

import com.google.common.hash.Hashing;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.lti13.model.LoginInitiationDTO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LTIService {
    public ApplicationInfo getApplicationInfo(String iss, String clientId, String ltiDeploymentId) throws LTIException {

        /**
         * can we reuse our ApplicationInfo ?, even we got only one appId?
         * we could take as appId:
         * - client_id + deployment_id OR
         * - iss + client_id  OR
         * - iss + deployment_id OR
         * - iss
         */
        ApplicationInfo applicationInfo = null;
        String applicationId = null;
        if(clientId != null && ltiDeploymentId != null){
            applicationId = clientId + ltiDeploymentId;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
               throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_CLIENT_ID+":"+clientId +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }else if(clientId != null){
            applicationId = clientId + iss;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_CLIENT_ID+":"+clientId +" "+LTIConstants.LTI_PARAM_ISS+":"+iss +" found.");
            }
        }else if(ltiDeploymentId != null){
            applicationId = iss + ltiDeploymentId;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_ISS+":"+iss +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }else{
            applicationId = iss;
            applicationInfo = ApplicationInfoList.getApplicationInfos().get(applicationId);
            if(applicationInfo == null){
                throw new LTIException(LTIConstants.ERROR_DEPLOYMENT_NOT_FOUND + LTIConstants.LTI_PARAM_ISS+":"+iss +" "+LTIConstants.LTI_PARAM_DEPLOYMENT_ID+":"+ltiDeploymentId +" found.");
            }
        }
        return applicationInfo;

    }
}
