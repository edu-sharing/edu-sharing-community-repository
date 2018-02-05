package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.security.authentication.AuthenticationException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.stream.v1.model.StreamEntry;
import org.edu_sharing.restservices.stream.v1.model.StreamEntryInput;
import org.edu_sharing.restservices.stream.v1.model.StreamList;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.stream.StreamService;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.edu_sharing.service.usage.Usage2Service;

public class StreamDao {
	public static StreamEntryInput addEntry(RepositoryDao repoDao,StreamEntryInput newEntry) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			ContentEntry entry=convertStreamEntry(newEntry);
			for(String node : newEntry.getNodes()) {
				NodeDao nodeDao=NodeDao.getNode(repoDao, node);
				// TODO: We need to set an usage to these node so they can be accessed from the stream user
				//new Usage2Service().getUsage(lmsId, courseId, parentNodeId, resourceId)
			}
			String id = service.addEntry(entry);	
			newEntry.setId(id);
			return newEntry;
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
	private static STATUS getStatusForEntry(String entryId) throws Exception{
		StreamService service=StreamServiceFactory.getStreamService();
		return service.getStatus(entryId, getCurrentAuthorities());
	}

	public static void updateStatus(RepositoryDao repoDao,String entryId,String authority,String status) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()  && !service.getEntry(entryId).author.equals(AuthenticationUtil.getFullyAuthenticatedUser())) {
				if(!AuthenticationUtil.getFullyAuthenticatedUser().equals(authority) || getStatusForEntry(entryId)==null)
					throw new AccessDeniedException("No permissions to change status of authority "+authority+" for stream entry "+entryId);
			}
			// simply check if the given authority is valid (does not work for special groups)
			//PersonDao.getPerson(repoDao, authority);
			service.updateStatus(entryId, authority, ContentEntry.Audience.STATUS.valueOf(status));
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
	private static ContentEntry convertStreamEntry(StreamEntryInput newEntry) {
		ContentEntry entry=new ContentEntry();
		entry.id=newEntry.getId();
		entry.title=newEntry.getTitle();
		entry.description=newEntry.getDescription();
		entry.author=AuthenticationUtil.getFullyAuthenticatedUser();
		entry.created=System.currentTimeMillis();
		entry.modified=System.currentTimeMillis();
		entry.nodeId=newEntry.getNodes();
		entry.properties=newEntry.getProperties();
		entry.priority=newEntry.getPriority();
		return entry;
	}

	public static StreamList search(RepositoryDao repoDao,String status,Map<String,String> property, String query, int offset, int maxItems) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			StreamList result=new StreamList();
			
			StreamSearchRequest request = new StreamSearchRequest(getCurrentAuthorities(),getStatus(status));
			request.offset=offset;
			request.size=maxItems;
			if(property!=null && !property.isEmpty())
				request.properties=property;
			if(query!=null && !query.isEmpty())
				request.search=query;
			StreamSearchResult response = service.search(request);
			List<StreamEntry> list=new ArrayList<>();
			for(ContentEntry contentEntry : response.results) {
				StreamEntry entry=new StreamEntry();
				List<Node> nodes=new ArrayList<>();
				for(String nodeId : contentEntry.nodeId) {
					nodes.add(NodeDao.getNode(repoDao, nodeId).asNode());
				}
				entry.setNodes(nodes);
				entry.setId(contentEntry.id);
				entry.setAuthor(new PersonDao(repoDao, contentEntry.author).asPersonSimple());
				entry.setProperties(contentEntry.properties);
				entry.setCreated((long) contentEntry.created);
				entry.setModified((long) contentEntry.modified);
				list.add(entry);
			}
			result.setStream(list);
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
	private static List<String> getCurrentAuthorities() throws Exception {
		AuthorityService authorityService=AuthorityServiceFactory.getLocalService();
		ArrayList<String> authorities = new ArrayList<String>();
		authorities.add(AuthenticationUtil.getFullyAuthenticatedUser());
		AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {

			@Override
			public Void doWork() throws Exception {
				authorities.addAll(authorityService.getMemberships(AuthenticationUtil.getFullyAuthenticatedUser()));
				return null;
			}
		});
		authorities.add(CCConstants.AUTHORITY_GROUP_EVERYONE);
		return authorities;
	}
	private static STATUS getStatus(String status) {
		if(status==null || status.isEmpty())
			return null;
		return ContentEntry.Audience.STATUS.valueOf(status);
	}
	public static Map<String, Number> getCategories(RepositoryDao repoDao) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			return service.getTopCategories();
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
}
