package org.edu_sharing.service.nodeservice;

import java.io.InputStream;
import java.util.HashMap;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.search.SearchServiceDDBImpl;

public class NodeServiceDDBImpl extends NodeServiceAdapter{



	
	public NodeServiceDDBImpl(String appId) {
		super(appId);
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {

		SearchServiceDDBImpl searchservice = new SearchServiceDDBImpl (this.appId);
		return searchservice.getProperties(nodeId);

   }
	
	@Override
	public String getType(String nodeId) {
		// TODO Auto-generated method stub
		return CCConstants.CCM_TYPE_IO;
	}
	
	@Override
	public String[] getAspects(String storeProtocol, String storeId, String nodeId) {
		// TODO Auto-generated method stub
		return new String[] {};
	}

	@Override
	public InputStream getContent(String nodeId) throws Throwable {
		return null;
	}
}