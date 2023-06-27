package org.edu_sharing.rest.notification.event;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InviteSafeEventDTO extends NodeBaseEventDTO {
    private String name;
    private String userComment;
    private List<String> permissions;
}