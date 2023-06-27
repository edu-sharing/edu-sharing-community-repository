package org.edu_sharing.rest.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentEventDTO extends NodeBaseEventDTO {
    private  String commentContent;

    /**
     * the id this comment refers to, if any
     */
    private String commentReference;
    private String event;
}