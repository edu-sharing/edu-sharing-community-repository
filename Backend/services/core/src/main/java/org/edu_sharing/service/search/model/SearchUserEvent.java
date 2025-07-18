package org.edu_sharing.service.search.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.edu_sharing.service.model.NodeRef;

@AllArgsConstructor
@Data
public class SearchUserEvent {
    NodeRef nodeRef;
}
