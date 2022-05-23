package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.restservices.stream.v1.model.StreamEntry;
import org.edu_sharing.restservices.stream.v1.model.StreamEntryInput;
import org.edu_sharing.restservices.stream.v1.model.StreamList;
import org.edu_sharing.service.authority.AuthorityServiceFactory;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.stream.StreamService;
import org.edu_sharing.service.stream.StreamServiceFactory;
import org.edu_sharing.service.stream.StreamServiceHelper;
import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ContentEntry.Audience.STATUS;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionException;
import org.edu_sharing.service.toolpermission.ToolPermissionServiceFactory;

public class StreamDao {
	public static StreamEntryInput addEntry(RepositoryDao repoDao,StreamEntryInput newEntry) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			ContentEntry entry=convertStreamEntry(newEntry);
			if(!ToolPermissionServiceFactory.getInstance().hasToolPermission(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_STREAM)){
				throw new ToolPermissionException(CCConstants.CCM_VALUE_TOOLPERMISSION_INVITE_STREAM);
			}
			for(String node : newEntry.getNodes()) {
				NodeDao nodeDao=NodeDao.getNode(repoDao, node);
				if(!nodeDao.access.contains(CCConstants.PERMISSION_CC_PUBLISH)) {
					throw new AccessDeniedException("No "+CCConstants.PERMISSION_CC_PUBLISH+" for node "+node);
				}
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
		return service.getStatus(entryId, StreamServiceHelper.getCurrentAuthorities());
	}
	public static void deleteEntry(RepositoryDao repoDao,String entryId) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			if(!AuthorityServiceFactory.getLocalService().isGlobalAdmin()  && !service.getEntry(entryId).author.equals(AuthenticationUtil.getFullyAuthenticatedUser())) {
				throw new AccessDeniedException("No permissions to delete stream entry "+entryId);
			}
			// simply check if the given authority is valid (does not work for special groups)
			//PersonDao.getPerson(repoDao, authority);
			service.delete(entryId);
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
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

	public static StreamList search(RepositoryDao repoDao, String status, Map<String, String> property, String query, int offset, int maxItems, SortDefinition sortDefinition) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			StreamList result=new StreamList();
			
			StreamSearchRequest request = new StreamSearchRequest(StreamServiceHelper.getCurrentAuthorities(),getStatus(status));
			request.offset=offset;
			request.size=maxItems;
			if(property!=null && !property.isEmpty())
				request.properties=property;
			if(query!=null && !query.isEmpty())
				request.search=query;
			request.sortDefinition=sortDefinition;
			StreamSearchResult response = service.search(request);
			List<StreamEntry> list=new ArrayList<>();
			for(ContentEntry contentEntry : response.results) {
				StreamEntry entry=new StreamEntry();
				List<Node> nodes=new ArrayList<>();
				for(String nodeId : contentEntry.nodeId) {
					// TODO: Is this correct?
					// When an item is visible in the stream, allow fully access to the user who has access to the stream entity
					NodeDao nodeDao=AuthenticationUtil.runAsSystem(()-> {
						try {
							return NodeDao.getNode(repoDao, nodeId);
						}catch(Throwable t) {
							t.printStackTrace();
						}
						return null;
					});
					if(nodeDao==null) {
						continue;
					}
					// load permissions with the current user
					nodeDao.refreshPermissions(null);
					AuthenticationUtil.runAsSystem(()-> {
						nodes.add(nodeDao.asNode());
						return null;
					});
				}
				entry.setNodes(nodes);
				entry.setId(contentEntry.id);
				entry.setAuthor(new PersonDao(repoDao, contentEntry.author).asPersonSimple());
				entry.setProperties(contentEntry.properties);
				entry.setCreated((long) contentEntry.created);
				entry.setModified((long) contentEntry.modified);
				entry.setPriority((int) contentEntry.priority);
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
	private static STATUS getStatus(String status) {
		if(status==null || status.isEmpty())
			return null;
		return ContentEntry.Audience.STATUS.valueOf(status);
	}
	public static boolean canAccessNode(String nodeId) throws DAOException{
		try {
			return StreamServiceHelper.canCurrentAuthorityAccessNode(StreamServiceFactory.getStreamService(),nodeId);
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
	public static Map<String, Number> getTopValues(RepositoryDao repoDao,String property) throws DAOException{
		try {
			StreamService service=StreamServiceFactory.getStreamService();
			return service.getTopValues(property);
		}catch(Exception e) {
			throw DAOException.mapping(e);
		}
	}
}
