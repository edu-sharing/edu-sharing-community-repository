/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.importer.OAIPMHLOMImporter;
import org.edu_sharing.repository.server.tools.cache.RepositoryCacheTool;

@Slf4j
public class RefreshCacheExecuter {

	/**
	 * Rebuilds the repository cache. The caller is responsible for running this in an
	 * authenticated context (the cache building itself runs as system anyway), so no
	 * session is created here
	 */
	public void excecute(String rootFolderId, boolean sticky) throws Throwable {

        log.info("rootFolderId:{}", rootFolderId);
		if (StringUtils.isBlank(rootFolderId)) {
			MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
			String companyHomeId = apiClient.getCompanyHomeNodeId();
			Map<String, Object> importFolderProps = apiClient.getChild(companyHomeId, CCConstants.CCM_TYPE_MAP, CCConstants.CM_NAME,
					OAIPMHLOMImporter.FOLDER_NAME_IMPORTED_OBJECTS);
			if (importFolderProps != null) {
				rootFolderId = (String) importFolderProps.get(CCConstants.SYS_PROP_NODE_UID);
			}
		}

		if (StringUtils.isBlank(rootFolderId)) {
			log.info("no root folder to refresh, skipping");
			return;
		}

		RepositoryCacheTool cache = new RepositoryCacheTool();
		if (sticky) {
			cache.buildStickyCache(rootFolderId);
		} else {
			cache.buildNewCache(rootFolderId);
		}
	}
}
