package org.edu_sharing.repository.server.tools.security;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.service.stream.StreamService;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.edu_sharing.service.permission.PermissionService;

public class SignatureVerifier {

	Logger logger = Logger.getLogger(SignatureVerifier.class);
	
	public class Result{
		int statuscode;
		String message;
		ApplicationInfo appInfo;
		
		public Result(int statuscode, String message, ApplicationInfo appInfo) {
			this.statuscode = statuscode;
			this.message = message;
			this.appInfo = appInfo;
		}
		
		public String getMessage() {
			return message;
		}
		
		public int getStatuscode() {
			return statuscode;
		}

		public ApplicationInfo getAppInfo() { return appInfo; }
	}
	
	
	public Result verify(String appId, String sig, String signed, String timeStamp){
		
		logger.debug("appId:"+appId+" sig:"+sig+" signed:"+signed+" timeStamp:"+timeStamp);
			
			ApplicationInfo appInfo = ApplicationInfoList.getRepositoryInfoById(appId);
			if(appInfo == null){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"appid "+appId+" is not registered",appInfo);
			}
			
			if(appInfo.getPublicKey() == null || appInfo.getPublicKey().trim().equals("")){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING PUBLIC KEY for appId:"+appId,appInfo);
			}
			
			if(sig == null){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING Signature",appInfo);
			}
			
			
			if(timeStamp == null || timeStamp.trim().equals("")){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING timestamp",appInfo);
			}
		
			
			long messageSendTs = new Long(timeStamp);
			long messageArrivedTs = System.currentTimeMillis();
						
			long messageSendOffset = appInfo.getMessageSendOffsetMs();

			if((messageSendTs - messageSendOffset) > messageArrivedTs){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MESSAGE SEND TIMESTAMP newer than MESSAGE ARRIVED TIMESTAMP",appInfo);
			}
			
			long messageOffset = appInfo.getMessageOffsetMs();
			
			if((messageArrivedTs - messageSendTs) > messageOffset ){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MESSAGE SEND TIMESTAMP TO OLD",appInfo);
			}
						
			
			if(signed == null){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING signed data",appInfo);
			}
			
			if(!signed.contains(timeStamp)){
				return new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING timestamp in signed data",appInfo);
			}
			
			boolean verified = false;
			try{
				Signing signing = new Signing();
				
				byte[] decoded = new Base64().decode(sig.getBytes());
				
				verified = signing.verify(signing.getPemPublicKey(appInfo.getPublicKey(), "RSA"),decoded, signed, "SHA1withRSA");
				
				
			}catch(Exception e){
				e.printStackTrace();
				return new Result(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage(),appInfo);
			}
			
			
			if(!verified){
				return new Result(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Signature could not be verified!",appInfo);
			}
			
			
			return new Result(HttpServletResponse.SC_OK, "OK",appInfo);
		
	}

	/**
	 *
	 * @param httpReq
	 * @return ApplicationInfo of verified app
	 */
	public Result verifyAppSignature(HttpServletRequest httpReq) {
		ApplicationInfo appResult = null;
		SignatureVerifier.Result result = null;

		if(httpReq.getHeader("X-Edu-App-Id") == null){
			return result = new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING X-Edu-App-Id",null);
		}


		String appId=httpReq.getHeader("X-Edu-App-Id");
		String sig=httpReq.getHeader("X-Edu-App-Sig");
		String signed=httpReq.getHeader("X-Edu-App-Signed");
		String ts=httpReq.getHeader("X-Edu-App-Ts");
		ApplicationInfo app = ApplicationInfoList.getRepositoryInfoById(appId);


		if(app==null){
			String message = "X-Edu-App-Id header was sent but the app/tool "+appId+" was not found in the list of registered apps";
			logger.warn(message);
			result = new Result(HttpServletResponse.SC_BAD_REQUEST,message,app);
		}else{
			result = this.verify(appId, sig, signed, ts);
			if(result.getStatuscode() == HttpServletResponse.SC_OK){
				logger.debug("Application request verified returning "+ appId);
			}
			else{
				logger.warn("X-Edu-App-Id header was sent but signature check failed for app "+appId+":"+result.getMessage());
			}
		}

		return result;
	}
}
