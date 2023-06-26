package org.edu_sharing.service.notification.events;

import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Jacksonized
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InviteSafeEventDTO extends NodeBaseEventDTO {
    private String name;
    private String userComment;
    @Singular
    private List<String> permissions;
}