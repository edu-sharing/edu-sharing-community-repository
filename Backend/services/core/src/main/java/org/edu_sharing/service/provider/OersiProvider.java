package org.edu_sharing.service.provider;

import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceOersiImpl;
import org.edu_sharing.service.permission.PermissionService;
import org.edu_sharing.service.permission.PermissionServiceCCPublish;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.SearchServiceOersiImpl;

public class OersiProvider extends Provider {

  public OersiProvider(String appId) {
    super(appId);
  }

  @Override
  public NodeService getNodeService() {
    return new NodeServiceOersiImpl(appId);
  }

  @Override
  public PermissionService getPermissionService() {
    return new PermissionServiceCCPublish(appId);
  }

  @Override
  public SearchService getSearchService() {
    return new SearchServiceOersiImpl(appId);
  }
}
