package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.restservices.collection.v1.model.Collection;
import org.edu_sharing.restservices.usage.v1.model.Usages;
import org.edu_sharing.restservices.usage.v1.model.Usages.Usage;
import org.edu_sharing.service.usage.Usage2Service;

public class UsageDao {


	RepositoryDao repoDao;
	
	MCAlfrescoBaseClient baseClient;
	
	public UsageDao(RepositoryDao repoDao) {
		
		this.repoDao = repoDao;
		this.baseClient = repoDao.getBaseClient();
	}
	
	public List<Usage> getUsages(String appId) throws DAOException{
		
		try{
			List<Usage> result = new ArrayList<Usage>();
			
			for(org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsages(appId)){
				Usage usageResult = convertUsage(usage);
				result.add(usageResult);
			}
			
			return result;
		}catch(Throwable e){
			throw DAOException.mapping(e);
		}
	}

	private Usage convertUsage(org.edu_sharing.service.usage.Usage usage) throws Exception {
		Usage usageResult = new Usage();
		usageResult.setAppId(usage.getLmsId());

		try {
			usageResult.setAppType(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getType());
			usageResult.setAppSubtype(ApplicationInfoList.getRepositoryInfoById(usage.getLmsId()).getSubtype());
		}catch(Throwable t){

		}

		//filter out usage info cause of security reasons
		String currentUser = baseClient.getAuthenticationInfo().get(CCConstants.AUTH_USERNAME);
		if(baseClient.isAdmin(currentUser) || currentUser.equals(usage.getAppUser())){
			usageResult.setAppUser(usage.getAppUser());
			usageResult.setAppUserMail(usage.getAppUserMail());
		}

		usageResult.setCourseId(usage.getCourseId());
		usageResult.setDistinctPersons(usage.getDistinctPersons());
		usageResult.setGuid(usage.getGuid());
		usageResult.setNodeId(usage.getNodeId());
		usageResult.setParentNodeId(usage.getParentNodeId());
		usageResult.setResourceId(usage.getResourceId());
		usageResult.setUsageCounter(usage.getUsageCounter());
		usageResult.setUsageVersion(usage.getUsageVersion());
		usageResult.setUsageXmlParams(usage.getUsageXmlParams());
		return usageResult;
	}

	public List<Usage> getUsagesByCourse(String appId, String courseId) throws DAOException{
		try{
			List<Usage> result = new ArrayList<Usage>();
			for(org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsagesByCourse(appId, courseId)){
				Usage usageResult = convertUsage(usage);
				result.add(usageResult);
			}
		return result;
		}catch(Throwable e){
			throw DAOException.mapping(e);
		}
		
		
	}

	public List<Usage> getUsagesByNode(String nodeId) throws DAOException{
		try{
			
			List<Usage> result = new ArrayList<Usage>();
			for(org.edu_sharing.service.usage.Usage usage : new Usage2Service().getUsageByParentNodeId(null, null, nodeId)){
				Usage usageResult = convertUsage(usage);
				result.add(usageResult);
			}
			return result;
			
		}catch(Throwable e){
			throw DAOException.mapping(e);
		}
	}

	public List<Collection> getUsagesByNodeCollection(String nodeId) throws DAOException {
		List<Collection> collections = new ArrayList<>();
		for(Usage usage : getUsagesByNode(nodeId)) {
			if(usage.getCourseId()==null)
				continue;
			try {
				collections.add(CollectionDao.getCollection(repoDao, usage.getCourseId()).asCollection());
			}catch(Throwable t) {}
		}
		return collections;
	}
}
