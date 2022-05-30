package org.edu_sharing.service.authentication.sso.config;

import java.util.List;
import java.util.Map;

public abstract class MappingGroupBuilder {

	protected abstract void init(Map<String,String> ssoAttributes);
	
	public abstract List<MappingGroup> getMapTo();
	
	public abstract MappingGroup getOrganisation();
	
}
