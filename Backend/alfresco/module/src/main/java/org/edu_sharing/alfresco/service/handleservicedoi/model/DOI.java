package org.edu_sharing.alfresco.service.handleservicedoi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

//@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DOI {
    private Data data;
}
