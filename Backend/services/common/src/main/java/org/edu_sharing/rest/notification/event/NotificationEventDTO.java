package org.edu_sharing.rest.notification.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.data.UserDataDTO;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        property = "_class",
        visible = true)
@JsonSubTypes({
        // also register it in the @NotificationEventEnum
        @JsonSubTypes.Type(value = AddToCollectionEventDTO.class, name="AddToCollectionEvent"),
        @JsonSubTypes.Type(value = CommentEventDTO.class, name="CommentEvent"),
        @JsonSubTypes.Type(value = InviteEventDTO.class, name="InviteEvent"),
        @JsonSubTypes.Type(value = NodeIssueEventDTO.class, name="NodeIssueEvent"),
        @JsonSubTypes.Type(value = RatingEventDTO.class, name="RatingEvent"),
        @JsonSubTypes.Type(value = WorkflowEventDTO.class, name="WorkflowEvent"),
        @JsonSubTypes.Type(value = MetadataSuggestionEventDTO.class, name="MetadataSuggestionEvent"),
})
public abstract class NotificationEventDTO {
    @JsonProperty("_id")
    private String id;
    private Date timestamp;
    private UserDataDTO creator;
    private UserDataDTO receiver;
    private StatusDTO status;
}

