package org.edu_sharing.service.authentication.sso.config;

import java.util.Map;

public class ConditionTrue extends ConditionSimple{

    @Override
    public boolean isTrue(Map<String, String> ssoAttributes) {
        return true;
    }
}
