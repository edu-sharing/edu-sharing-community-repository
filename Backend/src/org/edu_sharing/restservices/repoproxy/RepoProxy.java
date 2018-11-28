package org.edu_sharing.restservices.repoproxy;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.UrlTool;
import org.edu_sharing.repository.server.AuthenticationToolAPI;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteAppResult;
import org.edu_sharing.repository.server.tools.AuthenticatorRemoteRepository;
import org.edu_sharing.restservices.mds.v1.model.SuggestionParam;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.webservices.alfresco.extension.NativeAlfrescoWrapper;
import org.edu_sharing.webservices.util.AuthenticationDetails;
import org.edu_sharing.webservices.util.AuthenticationUtils;
import org.edu_sharing.webservices.util.EduWebServiceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.MdsV1Api;
import io.swagger.client.api.NodeV1Api;
import io.swagger.client.api.RenderingV1Api;
import io.swagger.client.api.SearchV1Api;
import io.swagger.client.model.MdsEntriesV2;
import io.swagger.client.model.MdsEntry;
import io.swagger.client.model.MdsV2;
import io.swagger.client.model.NodeEntries;
import io.swagger.util.Json;

public class RepoProxy {

	static Logger logger = Logger.getLogger(RepoProxy.class);

	ObjectMapper mapper = Json.mapper();

	/**
	 * ObjectMapper mapper = Json.mapper(); io.swagger.client.model.SearchParameters
	 * sp = mapper.convertValue(parameters,
	 * io.swagger.client.model.SearchParameters.class); System.out.println("Test:" +
	 * sp.getCriterias().iterator().next().getProperty());
	 */

	public Response searchV2(String repository, String mdsId, String query, SearchService.ContentType contentType,
			Integer maxItems, Integer skipCount, List<String> sortProperties, List<Boolean> sortAscending,
			SearchParameters parameters, List<String> propertyFilter, HttpServletRequest req) {
		logger.info("starting");
		io.swagger.client.model.SearchParameters sp = mapper.convertValue(parameters,
				io.swagger.client.model.SearchParameters.class);

		try {
			io.swagger.client.model.SearchResult sr = new SearchV1Api(getApiClient(repository,true)).searchV2(repository,
					mdsId, query, sp, contentType.toString(), maxItems, skipCount, sortProperties, sortAscending,
					propertyFilter);

			/**
			 * add ticket to preview
			 */
			ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(repository);
			String ticket = getTicket(repoInfo,false);
			for (io.swagger.client.model.Node node : sr.getNodes()) {
				String url = node.getPreview().getUrl();
				url = UrlTool.setParam(url, "ticket", ticket);
				node.getPreview().setUrl(url);
				node.getRef().setIsHomeRepo(false);
				node.getParent().setIsHomeRepo(false);
			}

			logger.info("returning");
			return Response.status(Response.Status.OK).entity(sr).build();

		} catch (ApiException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	public Response getDetailsSnippetWithParameters(String repository, String node, String nodeVersion,
			Map<String, String> parameters, HttpServletRequest req) {
		logger.info("starting");
		String result = "";
		try {
			result = new RenderingV1Api(getApiClient(repository,false)).getDetailsSnippetWithParameters(repository, node,
					nodeVersion, parameters);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("returning");
		return Response.status(Response.Status.OK).entity(result).build();
	}

	public Response getChildren(String repository, String node, Integer maxItems, Integer skipCount,
			List<String> filter, List<String> sortProperties, List<Boolean> sortAscending, String assocName,
			List<String> propertyFilter, HttpServletRequest req) {
		logger.info("starting");
		try {
			NodeEntries nodeEntries = new NodeV1Api(getApiClient(repository,false)).getChildren(repository, node, maxItems, skipCount, filter, sortProperties, sortAscending, assocName, propertyFilter);
			
			logger.info("returning");
			return Response.status(Response.Status.OK).entity(nodeEntries).build();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Response getMetadataSetsV2(String repository, HttpServletRequest req) {
		try {
			logger.info("starting");
			MdsEntriesV2 result = new MdsV1Api(getApiClient(repository, false)).getMetadataSetsV2(repository);
			logger.info("returning");
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Response getMetadataSetV2(String repository, String mdsId, HttpServletRequest req) {
		logger.info("starting");
		try {
			MdsV2 result = new MdsV1Api(getApiClient(repository,false)).getMetadataSetV2(repository, mdsId);
			logger.info("returning");
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public Response getValuesV2(
        	String repository,
        	String mdsId,
        	SuggestionParam suggestionParam,
    		HttpServletRequest req) {
		io.swagger.client.model.SuggestionParam sp = mapper.convertValue(suggestionParam,
				io.swagger.client.model.SuggestionParam.class);
		try {
			MdsEntry result = new MdsV1Api(getApiClient(repository,false)).getValuesV2(repository, mdsId, sp);
			return Response.status(Response.Status.OK).entity(result).build();
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	public static boolean myTurn(String repoId) {
		logger.info("starting");
		String homeRepo = ApplicationInfoList.getHomeRepository().getAppId();
		if (homeRepo.equals(repoId)) {
			return false;
		}
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(repoId);

		if (repoInfo != null && repoInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)) {
			return true;
		}
		logger.info("returning");
		return false;
	}

	private ApiClient getApiClient(String repoId, boolean validate) {
		logger.info("starting");
		ApplicationInfo repoInfo = ApplicationInfoList.getRepositoryInfoById(repoId);
		String ticket = getTicket(repoInfo, validate);

		ApiClient ac = new ApiClient();
		// String authHeaderValue =
		// Base64.getEncoder().encodeToString(("admin:admin").getBytes());
		// ac.addDefaultHeader("Authorization", "Basic " + authHeaderValue);
		ac.addDefaultHeader("Authorization", CCConstants.AUTH_HEADER_EDU_TICKET + ticket);
		ac.addDefaultHeader("Accept", "application/json");
		ac.setBasePath(repoInfo.getClientBaseUrl() + "/rest");
		logger.info("returning");
		return ac;
	}

	private String getTicket(ApplicationInfo repoInfo, boolean validate) {
		return remoteAuth(repoInfo,validate).get(CCConstants.AUTH_TICKET);
	}

	public HashMap<String, String> remoteAuth(ApplicationInfo repoInfo, boolean validate) {

		logger.info("starting");
		String SESSION_ATT = CCConstants.AUTH_REPOSITORY + repoInfo.getAppId();

		HttpSession session = Context.getCurrentInstance().getRequest().getSession();
		HashMap<String, String> remoteAuthInfo = (HashMap<String, String>) session.getAttribute(SESSION_ATT);

		if (remoteAuthInfo != null) {
			if(validate) {
				AuthenticationUtils.setAuthenticationDetails(
						new AuthenticationDetails(remoteAuthInfo.get(CCConstants.AUTH_USERNAME),
								remoteAuthInfo.get(CCConstants.AUTH_TICKET), null));
				NativeAlfrescoWrapper stub = EduWebServiceFactory.getNativeAlfrescoWrapper(repoInfo.getWebServiceHotUrl());
				try {
					stub.validateTicket(remoteAuthInfo.get(CCConstants.AUTH_TICKET));
					logger.info("returning validated ticket from session");
					return remoteAuthInfo;
				} catch (RemoteException e1) {
					logger.info(e1.getMessage());
					remoteAuthInfo = null;
				}
			}else {
				return remoteAuthInfo;
			}
		}

		if (repoInfo != null && repoInfo.getRepositoryType().equals(ApplicationInfo.REPOSITORY_TYPE_ALFRESCO)) {
			AuthenticatorRemoteRepository arr = new AuthenticatorRemoteRepository();
			HashMap<String, String> authInfo = new AuthenticationToolAPI().getAuthentication(session);
			if (repoInfo.getAuthenticationwebservice() != null && !repoInfo.getAuthenticationwebservice().equals("")) {
				try {
					AuthenticatorRemoteAppResult arar = arr.getAuthInfoForApp(authInfo, repoInfo);
					remoteAuthInfo = arar.getAuthenticationInfo();

					session.setAttribute(SESSION_ATT, remoteAuthInfo);

					String ticket = arar.getAuthenticationInfo().get(CCConstants.AUTH_TICKET);
					String username = arar.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
					AuthenticationUtils.setAuthenticationDetails(new AuthenticationDetails(username, ticket, null));

				} catch (Throwable e) {
					logger.error("It seems that repository id:" + repoInfo.getAppId() + "+ is not reachable:"
							+ e.getMessage());
				}
			}
		}
		logger.info("returning");
		return remoteAuthInfo;
	}

}
