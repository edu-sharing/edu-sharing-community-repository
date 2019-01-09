package org.edu_sharing.repository.update;

import java.util.List;

import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.Constants;

public class Release_5_0_NotifyRefactoring extends UpdateAbstract{

	public static final String ID = "Release_5_0_NotifyRefactoring";
	
	public static final String description = "remove notify objects, use permission_history aspect" ;
	
	int maxItems = 100;
	
	SearchService searchService = serviceRegistry.getSearchService();
	NodeService nodeService = serviceRegistry.getNodeService();
	
	
	@Override
	public void execute() {
		this.executeWithProtocolEntry();
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return description;
	}
	
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return ID;
	}
	
	@Override
	public void run() {
		
		
		
		
		SearchParameters sp = new SearchParameters();
		sp.addStore(Constants.storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setMaxItems(maxItems);
		sp.setQuery("TYPE:\"ccm:io\"");
		sp.setSkipCount(0);
		
		
		doIt(sp,null);
		
	}
	
	private void doIt(SearchParameters sp, ResultSet rs) {
		if(rs != null) {
			sp.setSkipCount(rs.getStart() + maxItems);
		}
		rs = searchService.query(sp);
		for(NodeRef nodeRef : rs.getNodeRefs()) {
			List<ChildAssociationRef> notifyParentAssocs = nodeService.getParentAssocs(nodeRef, QName.createQName(CCConstants.CCM_TYPE_NOTIFY), RegexQNamePattern.MATCH_ALL);
		}
	}
	
	
	@Override
	public void test() {
		logInfo("not implemented");
		
	}
	
}
