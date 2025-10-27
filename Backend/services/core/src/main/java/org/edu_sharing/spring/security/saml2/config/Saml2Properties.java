package org.edu_sharing.spring.security.saml2.config;

import lombok.Data;
import org.edu_sharing.service.authentication.sso.mapping.Mapping;

@Data
public class Saml2Properties {
    private Mapping mapping = new Mapping();
}
