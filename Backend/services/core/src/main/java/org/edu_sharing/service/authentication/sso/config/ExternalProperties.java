package org.edu_sharing.service.authentication.sso.config;

import lombok.Data;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;

@Data
public class ExternalProperties {
    private Mapping mapping = new Mapping();
}
