package org.edu_sharing.service.lti13.sso.config;

import lombok.Data;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;

@Data
public class LTIProperties {
    private Mapping mapping = new Mapping();
}
