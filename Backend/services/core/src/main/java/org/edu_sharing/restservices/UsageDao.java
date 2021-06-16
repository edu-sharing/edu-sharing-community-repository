package org.edu_sharing.restservices;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.shared.Filter;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.edu_sharing.restservices.usage.v1.model.Usages.NodeUsage;
import org.edu_sharing.restservices.usage.v1.model.Usages.Usage;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceFactory;
import org.edu_sharing.service.usage.Usage2Service;

import io.swagger.annotations.ApiParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;

public class UsageDao {
	Logger logger = Logger.getLogger(UsageDao.class);

	private final PermissionService permissionService;
	RepositoryDao repoDao;

	MCAlfrescoBaseClient baseClient;

	public UsageDao(RepositoryDao repoDao) {

		this.repoDao = repoDao;
		this.baseClient = repoDao.getBaseClient();
		this.permissionService = PermissionServiceFactory.getPermissionService(repoDao.getId());
	}

	public List<Usage> getUsages(String appId) throws DAOException {

		try {
			List<Usage> result = new ArrayList<Usage>();

			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsages(appId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}

			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	private <T extends Usage> T convertUsage(org.edu_sharing.service.usage.Usage usage, Class<T> cls) throws Exception {
		T usageResult = cls.newInstance();
		usageResult.setAppId(usage.getLmsId());

		try {
			usageResult.setAppType(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getType());
			usageResult.setAppSubtype(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getSubtype());
		} catch (Throwable t) {

		}

		// filter out usage info cause of security reasons
		String currentUser = baseClient.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
		if (baseClient.isAdmin(currentUser) || currentUser.equals(usage.getAppUser())) {
			usageResult.setAppUser(usage.getAppUser());
			usageResult.setAppUserMail(usage.getAppUserMail());
		}

		usageResult.setType(usage.getType().name());
		usageResult.setCourseId(usage.getCourseId());
		usageResult.setDistinctPersons(usage.getDistinctPersons());
		usageResult.setGuid(usage.getGuid());
		usageResult.setNodeId(usage.getNodeId());
		usageResult.setParentNodeId(usage.getParentNodeId());
		usageResult.setResourceId(usage.getResourceId());
		usageResult.setUsageCounter(usage.getUsageCounter());
		usageResult.setUsageVersion(usage.getUsageVersion());
		usageResult.setCreated(usage.getCreated());
		usageResult.setModified(usage.getModified());
		String xmlParams = usage.getUsageXmlParams();
		
		if(xmlParams != null) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Usage.Parameters.class);
	
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				jaxbUnmarshaller.setEventHandler(new ValidationEventHandler() {
					@Override
					public boolean handleEvent(ValidationEvent event) {
						// ignore all errors, try to parse what is possible
						return true;
					}
				});
				usageResult.setUsageXmlParams(
						(Usage.Parameters) jaxbUnmarshaller.unmarshal(new StringReader(usage.getUsageXmlParams())));
			} catch (Throwable t) {
				logger.warn("Error converting usage xml " + usage.getUsageXmlParams(), t);
			}
			
			
			usageResult.setUsageXmlParamsRaw(org.json.XML.toJSONObject(xmlParams).toString());
		}
		return usageResult;
	}

	public List<Usage> getUsagesByCourse(String appId, String courseId) throws DAOException {
		try {
			List<Usage> result = new ArrayList<Usage>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsagesByCourse(appId, courseId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}
			return result;
		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}

	}

	public void deleteUsage(String nodeId, String usageId) throws DAOException {
		try {
			boolean permission = permissionService.hasPermission(StoreRef.PROTOCOL_WORKSPACE,
					StoreRef.STORE_REF_WORKSPACE_SPACESSTORE.getIdentifier(), nodeId,
					CCConstants.PERMISSION_CHANGEPERMISSIONS);
			if (!permission) {
				throw new SecurityException("Can not modify usages on node " + nodeId);
			}
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null,
					nodeId)) {
				if (usage.getNodeId().equals(usageId)) {
					if (new Usage2Service().deleteUsage(null, null, usage.getLmsId(), usage.getCourseId(), nodeId,
							usage.getResourceId()))
						return;
					else
						throw new Exception("Error deleting usage " + usage.getNodeId());
				}
			}
			throw new IllegalArgumentException(usageId + " is not an usage of " + nodeId);
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public List<Usage> getUsagesByNode(String nodeId) throws DAOException {
		try {

			List<Usage> result = new ArrayList<Usage>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null,
					nodeId)) {
				Usage usageResult = convertUsage(usage, Usage.class);
				result.add(usageResult);
			}
			// remove usages from internal collections, since they're returned in getUsagesByNodeCollection
			/*
			Set<Usages.CollectionUsage> collections = getUsagesByNodeCollection(nodeId);
			result = result.stream().filter((r) ->
				collections.stream().filter((c) -> c.getNodeId().equals(r.getNodeId())).count()==0
			).collect(Collectors.toList());
			*/
			return result;

		} catch (Throwable e) {
			throw DAOException.mapping(e);
		}
	}

	public Set<Usages.CollectionUsage> getUsagesByNodeCollection(String nodeId) throws DAOException {
		try {
			Set<Usages.CollectionUsage> collections = new HashSet<>();
			for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null,
					nodeId)) {
				if (usage.getCourseId() == null)
					continue;
				try {
					Usages.CollectionUsage collectionUsage = convertUsage(usage, Usages.CollectionUsage.class);
					collectionUsage
							.setCollection(CollectionDao.getCollection(repoDao, usage.getCourseId()).asNode());
					collections.add(collectionUsage);
				} catch (Throwable t) {
				}
			}
			return collections;
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
	}

	public List<Usages.NodeUsage> getUsages(String repositoryId, String nodeId, Long from, Long to) throws Exception {

		
		RunAsWork<List<Usages.NodeUsage>> runAs = new RunAsWork<List<Usages.NodeUsage>>() {
			@Override
			public List<Usages.NodeUsage> doWork() throws Exception {
				RepositoryDao rd = RepositoryDao.getHomeRepository();
				
				List<Usages.NodeUsage> result = new ArrayList<Usages.NodeUsage>();
				for (org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsages(repositoryId, nodeId, from,
						to)) {
					try {
						Usages.NodeUsage usageRest = convertUsage(usage, Usages.NodeUsage.class);
						usageRest.setNode(NodeDao.getNode(rd, usageRest.getParentNodeId(),Filter.createShowAllFilter()).asNode());
						result.add(usageRest);
					} catch (Throwable t) {
					}
				}
				return result;
			}
		};
		return AuthenticationUtil.runAsSystem(runAs);
		
	}
}
