package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.shared.SearchResult;
import org.edu_sharing.restservices.stream.v1.model.StreamEntry;
import org.edu_sharing.service.stream.StreamService;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;

public class StreamDao {
	public static SearchResult<StreamEntry> search(RepositoryDao repoDao,String status,int offset, int maxItems) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			SearchResult<StreamEntry> result=new SearchResult<>();
			StreamSearchRequest request = new StreamSearchRequest(AuthenticationUtil.getFullyAuthenticatedUser(),ContentEntry.Audience.STATUS.valueOf(status));
			request.offset=offset;
			request.size=maxItems;
			StreamSearchResult response = service.search(request);
			List<StreamEntry> list=new ArrayList<>();
			for(ContentEntry contentEntry : response.results) {
				StreamEntry entry=new StreamEntry();
				entry.setId(contentEntry.id);
				List<Node> nodes=new ArrayList<>();
				for(String nodeId : contentEntry.nodeId) {
					nodes.add(NodeDao.getNode(repoDao, nodeId).asNode());
				}
				entry.setNodes(nodes);
				list.add(entry);
			}
			Pagination pagination = new Pagination();
			pagination.setFrom(offset);
			pagination.setCount(response.results.size());
			pagination.setTotal((int) response.total);
			result.setPagination(pagination);
			return result;

		}catch(Exception e){
			throw DAOException.mapping(e);
		}
	}	
}
