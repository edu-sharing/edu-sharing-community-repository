package org.edu_sharing.service.nodeservice;

import java.util.HashMap;
import org.edu_sharing.service.search.SearchServiceDDBImpl;
import org.edu_sharing.service.search.SearchServiceKhanImpl;

public class NodeServiceKhanImpl extends NodeServiceAdapter{




	public NodeServiceKhanImpl(String appId) {
		super(appId);
	}

	@Override
	public HashMap<String, Object> getProperties(String storeProtocol, String storeId, String nodeId) throws Throwable {

		SearchServiceKhanImpl searchservice = new SearchServiceKhanImpl(this.appId);
		return null;//searchservice.getProperties(nodeId);

	}

}