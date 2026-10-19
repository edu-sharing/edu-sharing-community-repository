package org.edu_sharing.restservices.scope.v1.dto;

import lombok.Data;
import org.edu_sharing.service.scope.ScopeField;
import org.edu_sharing.service.scope.ScopePresentation;

import java.util.List;

@Data
public class ScopeCreateRequestDTO {
    /** name of the group (GROUP_ORG_&lt;name&gt;) */
    private String name;
    private String displayName;
    /** mds providing widgets, valuespaces and i18n of the scope properties */
    private String mds;
    private List<ScopeField> fields;
    /** presentation settings (card columns ...), null for none */
    private ScopePresentation presentation;
}
