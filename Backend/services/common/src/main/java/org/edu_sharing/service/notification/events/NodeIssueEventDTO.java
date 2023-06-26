package org.edu_sharing.service.notification.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NodeIssueEventDTO extends NodeBaseEventDTO {
    private String email;
    private String reason;
    private String userComment;
}