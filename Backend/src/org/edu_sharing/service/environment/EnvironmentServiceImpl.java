package org.edu_sharing.service.environment;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.EnvInfo;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;

/**
 * uses per Thread auth
 * 
 * @author rudi
 *
 */
public class EnvironmentServiceImpl implements EnvironmentService{

	Logger logger = Logger.getLogger(EnvironmentServiceImpl.class);
	@Override
	public EnvInfo getEntInfo(String repositoryId) {
		EnvInfo result = new EnvInfo();
		try {

			//use per thread auth
			MCAlfrescoBaseClient mcAlfrescoBaseClient = (MCAlfrescoBaseClient)RepoFactory.getInstance(repositoryId, (HashMap<String,String>)null);
			String rootNodeId = mcAlfrescoBaseClient.getRootNodeId();
			
			
			if (rootNodeId != null && !rootNodeId.trim().equals("")) {
				//link public folder
				mcAlfrescoBaseClient.checkAndLinkPublicFolder(rootNodeId);
				result.setRootNode(rootNodeId);
				HashMap<String,Object> rootNodeProps = mcAlfrescoBaseClient.getProperties(rootNodeId);
				result.setRootNodeProps(rootNodeProps);

			} 
			
			HashMap<String,Object> defaultUploadFolderProps = getDefaultUploadFolder(repositoryId);
			if(defaultUploadFolderProps == null && result.getRootNodeProps() != null){
				defaultUploadFolderProps = result.getRootNodeProps();
			}
			
			result.setDefaultUploadFolder((String)defaultUploadFolderProps.get(CCConstants.SYS_PROP_NODE_UID));
			result.setDefaultUploadFolderProps(defaultUploadFolderProps);

		} catch (Throwable e) {
			logger.error(e.getMessage(),e);
		}
		return result;
	}
	
	
	protected HashMap<String,Object> getDefaultUploadFolder(String repositoryId){
		return null;
	}
}
