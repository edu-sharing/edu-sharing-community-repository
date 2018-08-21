package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.spring.ApplicationContextFactory;

public class NodeServiceFactory {

	public static NodeService getNodeService(String appId){
		
		NodeService nodeService = null;
		ApplicationInfo appInfo = (appId == null) ? ApplicationInfoList.getHomeRepository() : ApplicationInfoList.getRepositoryInfoById(appId);
		
		if(appInfo.getNodeService() == null || appInfo.getNodeService().trim().equals("")){
			return getLocalService();

		}else{
			try{
				Class clazz = Class.forName(appInfo.getNodeService());
				Object obj = clazz.getConstructor(new Class[] { String.class}).newInstance(new Object[] { appId });
				nodeService = (NodeService)obj;
			}catch(Exception e){
				e.printStackTrace();
				throw new RuntimeException(e.getMessage());
			}
		}
		
		return nodeService;
	}

	public static NodeService getLocalService() {
		return (NodeService)ApplicationContextFactory.getApplicationContext().getBean("nodeService");
	}
}
