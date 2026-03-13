package org.edu_sharing.service.authentication.authByApp.config;

import lombok.Data;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;

@Data
public class AuthByAppProperties {
    private Mapping mapping = new Mapping();
}
