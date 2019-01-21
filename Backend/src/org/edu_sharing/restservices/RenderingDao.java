package org.edu_sharing.restservices;

import java.util.Map;

import org.apache.log4j.Logger;
import org.edu_sharing.service.rendering.RenderingTool;

public class RenderingDao {

	RepositoryDao repoDao;
	
	public RenderingDao(RepositoryDao repoDao) {
		this.repoDao = repoDao;
	}
	
	public String getDetails(String nodeId,String nodeVersion,Map<String,String> parameters) throws DAOException{
		try{
			return repoDao.getRenderingServiceClient().getDetails(nodeId,nodeVersion,RenderingTool.DISPLAY_DYNAMIC,parameters);
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}
}
