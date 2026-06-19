package org.edu_sharing.repository.server.tools.security;

import io.opentelemetry.api.internal.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

import java.util.List;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class SignatureVerifier {

	Logger logger = Logger.getLogger(SignatureVerifier.class);

    com.typesafe.config.Config config = LightbendConfigLoader.get();

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
	
	
	public Result verify(String appId, String sig, String signed, String timeStamp, String algorithm){
		
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

                String algDefaultVerify = this.config.getString("security.sso.authByApp.alg.defaultVerify");

                if(algorithm == null){
                    if(!StringUtils.isNullOrEmpty(appInfo.getSignatureAlgorithm())){
                        algorithm = appInfo.getSignatureAlgorithm();
                    }else{
                        algorithm = algDefaultVerify;
                    }
                }

                List<String> supported = this.config.getStringList("security.sso.authByApp.alg.supported");
                if(!supported.contains(algorithm)
                        && (appInfo.getSignatureAlgorithm() != null && !appInfo.getSignatureAlgorithm().equals(algorithm))
                        && !algDefaultVerify.equals(algorithm)){
                    return new Result(HttpServletResponse.SC_BAD_REQUEST,"ALGORITHM NOT SUPPORTED",appInfo);
                }





				verified = signing.verify(signing.getPemPublicKey(appInfo.getPublicKey(), "RSA"),decoded, signed, algorithm);
				
				
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

		if(getHeaderOrParam("X-Edu-App-Id", httpReq) == null){
			return result = new Result(HttpServletResponse.SC_BAD_REQUEST,"MISSING X-Edu-App-Id",null);
		}


		String appId=getHeaderOrParam("X-Edu-App-Id",httpReq);
		String sig=getHeaderOrParam("X-Edu-App-Sig",httpReq);
		String signed=getHeaderOrParam("X-Edu-App-Signed",httpReq);
        String signedAlg=getHeaderOrParam("X-Edu-App-SignedAlg",httpReq);
		String ts=getHeaderOrParam("X-Edu-App-Ts",httpReq);
		ApplicationInfo app = ApplicationInfoList.getRepositoryInfoById(appId);


		if(app==null){
			String message = "X-Edu-App-Id header was sent but the app/tool "+appId+" was not found in the list of registered apps";
			logger.warn(message);
			result = new Result(HttpServletResponse.SC_BAD_REQUEST,message,app);
		}else{
			result = this.verify(appId, sig, signed, ts, signedAlg);
			if(result.getStatuscode() == HttpServletResponse.SC_OK){
				logger.debug("Application request verified returning "+ appId);
			}
			else{
				logger.warn("X-Edu-App-Id header was sent but signature check failed for app "+appId+":"+result.getMessage());
			}
		}

		return result;
	}

	/**
	 * tries to get header value if null it uses fallback over request param
	 *
	 * @param key
	 * @param httpReq
	 * @return
	 */
	public static String getHeaderOrParam(String key, HttpServletRequest httpReq){
		String value = httpReq.getHeader(key);
		if(value == null){
			value = httpReq.getParameter(key);
		} else {
			// header spec does not allow special utf chars
			if(key.equals("X-Edu-User-Id")) {
				value = URLDecoder.decode(value, StandardCharsets.UTF_8);
			}
		}
		return value;
	}
}
