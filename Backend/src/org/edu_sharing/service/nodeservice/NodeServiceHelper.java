package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;

public class NodeServiceHelper {
	/**
	 * Clean the CM_NAME property so it does not cause an org.alfresco.repo.node.integrity.IntegrityException
	 * @param cmNameReadableName
	 * @return
	 */
	public static String cleanupCmName(String cmNameReadableName){
		// replace chars that can lead to an
		// org.alfresco.repo.node.integrity.IntegrityException
		cmNameReadableName = cmNameReadableName.replaceAll(
			RepoFactory.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_VALIDATOR_REGEX_CM_NAME), "_");

		//replace ending dot with nothing
		//cmNameReadableName = cmNameReadableName.replaceAll("\\.$", "");
		cmNameReadableName = cmNameReadableName.replaceAll("[\\.]*$", "");
		return cmNameReadableName;
	}
}
