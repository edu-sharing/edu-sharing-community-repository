package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.archive.ArchiveService;
import org.edu_sharing.service.archive.ArchiveServiceFactory;
import org.edu_sharing.service.archive.model.RestoreResult;
import org.edu_sharing.service.search.model.SortDefinition;
import org.edu_sharing.service.search.model.SortDefinition.SortDefinitionEntry;

public class ArchiveDao {
	
	public static NodeSearch search(RepositoryDao repoDao, 
			String query, 
			int startIdx,
			int nrOfresults, List<String> sortProperties, List<Boolean> sortAscending) throws DAOException {

		try {
			ArchiveService archivService = ArchiveServiceFactory.getArchiveService(repoDao.getId());
			
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			
			return NodeDao.transform(repoDao, archivService.search(query,startIdx,nrOfresults,sortDefinition));
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}

	public static NodeSearch search(RepositoryDao repoDao, 
			String query, 
			String user,
			int startIdx,
			int nrOfresults, List<String> sortProperties, List<Boolean> sortAscending) throws DAOException {

		try {
						
			String currentUser = AuthenticationUtil.getFullyAuthenticatedUser(); 
			
			if (PersonDao.ME.equals(user)) {
				user = currentUser;
			}
			
			ArchiveService archivService = ArchiveServiceFactory.getArchiveService(repoDao.getId());
			
			SortDefinition sortDefinition = new SortDefinition(sortProperties,sortAscending);
			
			
			return NodeDao.transform(repoDao, archivService.search(query, user, startIdx, nrOfresults,sortDefinition));
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static void purge(RepositoryDao repoDao, List<String> archivedNodeIds) throws DAOException {
		try {
			ArchiveService archivService = ArchiveServiceFactory.getArchiveService(repoDao.getId());
			archivService.purge(archivedNodeIds);
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	public static List<org.edu_sharing.restservices.archive.v1.model.RestoreResult> restore(RepositoryDao repoDao, List<String> archivedNodeIds, String toFolder) throws DAOException {
		try {
			ArchiveService archivService = ArchiveServiceFactory.getArchiveService(repoDao.getId());
			List<RestoreResult> results =  archivService.restore(archivedNodeIds, toFolder);
			
			
			List<org.edu_sharing.restservices.archive.v1.model.RestoreResult> apiResult = new ArrayList<org.edu_sharing.restservices.archive.v1.model.RestoreResult>();
			for(RestoreResult result : results){
				org.edu_sharing.restservices.archive.v1.model.RestoreResult rs = new org.edu_sharing.restservices.archive.v1.model.RestoreResult();
				rs.setArchiveNodeId(result.getArchiveNodeId());
				rs.setRestoreStatus(result.getRestoreStatus());
				rs.setNodeId(result.getNodeId());
				rs.setPath(result.getPath());
				rs.setParent(result.getParent());
				rs.setName(result.getName());
				apiResult.add(rs);
			}
			
			return apiResult;
						
		} catch (Throwable t) {
			
			throw DAOException.mapping(t);
		}
	}
	
	
}

