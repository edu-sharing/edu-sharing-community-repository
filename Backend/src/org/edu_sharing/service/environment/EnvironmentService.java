package org.edu_sharing.service.environment;

import org.edu_sharing.repository.client.rpc.EnvInfo;

public interface EnvironmentService {

	EnvInfo getEntInfo(String repositoryId);
		
}
