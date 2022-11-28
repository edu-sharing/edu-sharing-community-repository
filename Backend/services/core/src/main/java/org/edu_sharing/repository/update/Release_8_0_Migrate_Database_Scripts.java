package org.edu_sharing.repository.update;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.service.toolpermission.ToolPermissionBaseService;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;
import org.edu_sharing.repository.server.tools.UserEnvironmentTool;
import org.edu_sharing.service.nodeservice.NodeServiceHelper;
import org.edu_sharing.service.nodeservice.RecurseMode;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Release_8_0_Migrate_Database_Scripts extends UpdateAbstract {

	public static final String ID = "Release_8_0_Migrate_Database_Scripts";

	public static final String description = "Migrates all database scripts and removes the .properties name";
	public static final String FILE_SUFFIX = ".properties";


	NodeService nodeService = serviceRegistry.getNodeService();
	CopyService copyService = serviceRegistry.getCopyService();

	MCAlfrescoAPIClient apiClient;

	public Release_8_0_Migrate_Database_Scripts(PrintWriter out) {
		this.out = out;
		this.logger = Logger.getLogger(Release_8_0_Migrate_Database_Scripts.class);
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void run() throws Throwable {

	}

	@Override
	public boolean runAndReport() {
		return doTask(false);
	}

	@Override
	public void execute() {
		executeWithProtocolEntryNoGlobalTx();
	}

	@Override
	public void test() {
		doTask(true);
	}

	private boolean doTask(boolean test) {
		try {
			String eduSystemFolderUpdate = new UserEnvironmentTool().getEdu_SharingSystemFolderUpdate();
			NodeRef updateFolder = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, eduSystemFolderUpdate);
			NodeServiceHelper.getChildrenChildAssociationRefType(
					updateFolder, CCConstants.CCM_TYPE_SYSUPDATE
			).stream().map(ChildAssociationRef::getChildRef).filter(child ->
				NodeServiceHelper.getProperty(child, CCConstants.CM_NAME).startsWith(SQLUpdater.ID) &&
				NodeServiceHelper.getProperty(child, CCConstants.CM_NAME)
						.endsWith(FILE_SUFFIX)
			).forEach(child -> {
				String name = NodeServiceHelper.getProperty(child, CCConstants.CM_NAME);
				String newName = NodeServiceHelper.getProperty(child, CCConstants.CM_NAME).substring(0, name.length() - FILE_SUFFIX.length());
				logInfo("Copy update info from "+ name + " to " + newName + " (" + child + ")");
				if(!test) {
					NodeRef newNode = copyService.copyAndRename(
							child,
							updateFolder,
							ContentModel.ASSOC_CONTAINS,
							QName.createQName(newName),
							false
					);
					NodeServiceHelper.setProperty(newNode, CCConstants.CM_NAME, newName, false);
				}
			});
			return true;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}


}
