package org.edu_sharing.restservices;

import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.nodeservice.NodeService;
import org.edu_sharing.service.nodeservice.NodeServiceFactory;
import org.edu_sharing.service.search.SearchService;

public abstract class AbstractDao {
    protected final RepositoryDao repoDao;
    protected final AuthorityService authorityService;
    protected final NodeService nodeService;
    protected final SearchService searchService;

    AbstractDao(RepositoryDao repoDao){
        this.repoDao=repoDao;
        this.authorityService = repoDao.getAuthorityService();
        this.nodeService = repoDao.getNodeService();
        this.searchService = repoDao.getSearchService();
    }
}
