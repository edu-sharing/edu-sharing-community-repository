package org.edu_sharing.service.rendering;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.lightbend.LightbendConfigCache;
import org.edu_sharing.alfresco.repository.server.authentication.Context;
import org.edu_sharing.repository.client.rpc.User;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.repository.server.tools.security.Encryption;
import org.edu_sharing.repository.server.tools.security.Signing;
import org.edu_sharing.restservices.NodeDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.restservices.about.v1.model.AboutPlugins;
import org.edu_sharing.restservices.about.v1.model.PluginInfo;
import org.edu_sharing.restservices.shared.SignedNode;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.config.ConfigServiceFactory;
import org.edu_sharing.spring.ApplicationContextFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RenderingTool {

	public static String DISPLAY_PRERENDER = "prerender";
	public static String DISPLAY_DYNAMIC = "dynamic";
	public static String DISPLAY_EMBED = "embed";
	// only content (e.g. video player), no license data
	public static String DISPLAY_CONTENT = "content";
	public static String DISPLAY_INLINE = "inline";
	
	public static String COM_INTERNAL = "internal";
	
	static Logger logger = Logger.getLogger(RenderingTool.class);

	// thread tasks for processing requests of pre-rendering objects
	static ExecutorService prepareExecutor = Executors.newFixedThreadPool(1);
	/**
	 * this only works for alfresco repositories
	 * 
	 * @param repInfo
	 * @return
	 * @throws GeneralSecurityException
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingService = homeRepo.getContentUrl();

		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();

		if(parameters!=null){
			for(Entry<String, String> param : parameters.entrySet()){
				renderingService = UrlTool.setParam(renderingService, param.getKey(),param.getValue());
			}
		}
		String appId = repInfo.getAppId();
		if(nodeId == null) {
			appId = "";
			nodeId = UUID.randomUUID().toString();
			renderingService = UrlTool.setParam(renderingService, "sig_token", nodeId);
		}
		renderingService = UrlTool.setParam(renderingService, "ts",""+timestamp);
		try{
			renderingService = UrlTool.setParam(renderingService, "language",new AuthenticationToolAPI().getCurrentLanguage());
		}catch(Throwable t){}

		renderingService = UrlTool.setParam(renderingService, "sig", getSignatureSigned(appId,nodeId,timestamp));
		return renderingService;

	}
	
	/**
	 * Just an override with few parameters!
	 */
	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId) throws GeneralSecurityException{
		return getRenderServiceUrl(repInfo,nodeId,null,null);
	}

	public String getRenderServiceUrl(ApplicationInfo repInfo,String nodeId,Map<String,String> parameters,String displayType) throws GeneralSecurityException {
		
		String baseUrl = getRenderServiceUrl(repInfo,nodeId,parameters);
		return UrlTool.setParam(baseUrl,"display",displayType);
	}
	public static String getSignatureSigned(String repId, String nodeId, long timestamp) throws GeneralSecurityException {
		String data = getSignatureContent(repId, nodeId, timestamp);
		Signing sig = new Signing();
		//take the homeRepository keys for signature
		String privateKey = ApplicationInfoList.getHomeRepository().getPrivateKey();


		if(privateKey == null){
			logger.error("no privateKey available");
			throw new GeneralSecurityException("no privateKey available");
		}
		byte[] signature = sig.sign(sig.getPemPrivateKey(privateKey, CCConstants.SECURITY_KEY_ALGORITHM), data.getBytes(StandardCharsets.UTF_8), CCConstants.SECURITY_SIGN_ALGORITHM);
		return URLEncoder.encode(java.util.Base64.getEncoder().encodeToString(signature));

	}

	public static String getSignatureContent(String repId, String nodeId, Object timestamp) {
		return repId+nodeId+timestamp;
	}

	public static String getRenderServiceUrl(ApplicationInfo repInfo, String nodeId,String version,boolean displayMetadata, boolean backendCall) throws GeneralSecurityException{

		ApplicationInfo homeRepo = ApplicationInfoList.getHomeRepository();
		
		String renderingProxy = (backendCall) ? homeRepo.getWebServerUrl() + "/" + homeRepo.getWebappname() +"/renderingproxy" 
											  : homeRepo.getClientBaseUrl() +"/renderingproxy";
		//renderServiceUrl = UrlTool.setParam(renderServiceUrl, "proxyRepId", ApplicationInfoList.getHomeRepository().getAppId());
		
		long timestamp = System.currentTimeMillis();

		renderingProxy = UrlTool.setParam(renderingProxy, "obj_id", nodeId);
		renderingProxy = UrlTool.setParam(renderingProxy, "rep_id",repInfo.getAppId());
		if(version!=null)
			renderingProxy = UrlTool.setParam(renderingProxy, "version",version);
		renderingProxy = UrlTool.setParam(renderingProxy, "metadata",""+displayMetadata);
		renderingProxy = UrlTool.setParam(renderingProxy, "ts",""+timestamp);
		

		renderingProxy = UrlTool.setParam(renderingProxy, "sig", getSignatureSigned(repInfo.getAppId(),nodeId,timestamp));
		
		if(repInfo.ishomeNode()){
			renderingProxy = UrlTool.setParam(renderingProxy, "app_id",repInfo.getAppId());
		}else{
			renderingProxy = UrlTool.setParam(renderingProxy, "proxyRepId",homeRepo.getAppId());
		}
		
		renderingProxy = URLTool.addOAuthAccessToken(renderingProxy);
			
		return renderingProxy;
		
	}
	
	private String getUsernameEncrypted(String username) {
		ApplicationInfo appInfoRender = ApplicationInfoList.getHomeRepository();
		String usernameEncrypted = null;
		try {
			Encryption encryptionTool = new Encryption("RSA");
			byte[] userEncryptedBytes = encryptionTool.encrypt(username.getBytes(), encryptionTool.getPemPublicKey(appInfoRender.getPublicKey()));
			usernameEncrypted = java.util.Base64.getEncoder().encodeToString(userEncryptedBytes);
			usernameEncrypted = URLEncoder.encode(usernameEncrypted, "UTF-8");
			return usernameEncrypted;
		}catch(Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	public static void buildRenderingCache(String nodeId) {
        AboutPlugins plugins = ApplicationContextFactory.getApplicationContext().getBean(AboutPlugins.class);
        Optional<PluginInfo> rs2 = plugins.getPlugins().stream().filter(p -> RenderingPluginInfo.RENDERING_SERVICE_2.equals(p.getId())).findFirst();
        if(rs2.isPresent()){
            ApplicationInfo renderingService2 = ApplicationInfoList.getRenderingService2();
            String baseUrl = renderingService2.getBaseUrl();
            RestClient restClient = RestClient.builder()
                    .baseUrl(baseUrl)
                    .build();

            String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
            if(fullyAuthenticatedUser != null){
                try {
                    User user = AuthorityServiceFactory.getLocalService().getUser(fullyAuthenticatedUser);
                    RequestUserData userData = RequestUserData.builder()
                            .authorityName(fullyAuthenticatedUser)
                            .firstName(user.getGivenName())
                            .surName(user.getSurname())
                            .userEMail(user.getEmail())
                            .build();
                    NodeDao node = NodeDao.getNode(RepositoryDao.getHomeRepository(), nodeId);
                    Base64.Encoder encoder = Base64.getEncoder();

                    SignedNode signedNode = node.getSignedNode();
                    String encodedSignedNode = encoder.encodeToString(signedNode.getNode().getBytes());
                    String encodedSignature = encoder.encodeToString(signedNode.getSignature());
                    RenderDataRequest request = RenderDataRequest.builder()
                            .repoId(ApplicationInfoList.getHomeRepository().getAppId())
                            .nodeId(nodeId)
                            .userData(userData)
                            .securedNode(encodedSignedNode)
                            .signature(encodedSignature)
                            .build();
                    String response = restClient.post()
                            .uri("/rendering/public/renderdata")
                            .header("Authorization", "Bearer " + node.getJWT())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(request)
                            .retrieve()
                            .body(String.class);
                    logger.info("prerendering response:" + response);
                } catch (Throwable e) {
                    logger.warn("prerendering failed:" + e.getMessage(), e);
                }
            }

        }else{
            final Context context = Context.getCurrentContextForCustomThreads();
            prepareExecutor.execute(()->{
                AuthenticationUtil.runAsSystem(()-> {
                    try {
                        Context.setInstance(context);
                        // Deprecated, use the Lightbend config!
                        if(!ConfigServiceFactory.getCurrentConfig().getValue("rendering.prerender",true)) {
                            return null;
                        }
                        if(!LightbendConfigCache.getBoolean("rendering.prerender")) {
                            return null;
                        }
                        // @TODO: May we need to build up caches just for particular file types?
                        RenderingService service = RenderingServiceFactory.getLocalService();
                        return service.getDetails(ApplicationInfoList.getHomeRepository().getAppId(), nodeId, null, DISPLAY_PRERENDER, null);
                    } catch (Exception e) {
                        logger.warn("Error building rendering cache for node " + nodeId + ": " + e.getMessage(), e);
                        return e;
                    } finally {
                        Context.release();
                    }
                });
            });
        }
	}


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderDataRequest {

        @NotNull
        private String nodeId;

        @NotNull
        private String repoId;

        @NotNull
        private String securedNode;

        @NotNull
        private String signature;

        @NotNull
        private RequestUserData userData;

        @Builder.Default
        private String eventType = "PRERENDER";
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestUserData {

        private String authorityName;

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private String firstName = "";

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private String surName = "";

        @JsonSetter(nulls = Nulls.AS_EMPTY)
        @Builder.Default
        private String userEMail = "";
    }


}
