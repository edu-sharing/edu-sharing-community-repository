package org.edu_sharing.service.config;

import java.util.Collection;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.config.model.Values;

public interface ConfigService {

	Values getConfigValues() throws Exception;

	Values getConfigValuesByDomain(String domain) throws Exception;;

	
}
