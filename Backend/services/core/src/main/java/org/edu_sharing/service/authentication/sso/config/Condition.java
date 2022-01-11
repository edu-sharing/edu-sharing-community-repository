package org.edu_sharing.service.authentication.sso.config;

import java.util.Map;

public interface Condition {
	boolean isTrue(Map<String,String> ssoAttributes);
}
