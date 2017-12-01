package org.edu_sharing.service.rendering;

import java.util.Map;

import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.InsufficientPermissionException;

public interface RenderingService {
	
	

	public String getDetails( String nodeId,String nodeVersion,Map<String,String> parameters) throws InsufficientPermissionException, Exception;
	
	
}
