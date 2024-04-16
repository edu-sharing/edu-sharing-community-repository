package org.edu_sharing.rest.notification.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.edu_sharing.rest.notification.data.NodeDataDTO;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.data.UserDataDTO;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NodeIssueEventDTO extends NodeBaseEventDTO {
    public NodeIssueEventDTO(String id, Date timestamp, UserDataDTO creator, UserDataDTO receiver, StatusDTO status, NodeDataDTO node, String reason, String userComment) {
        super(id, timestamp, creator, receiver, status, node);
        this.reason = reason;
        this.userComment = userComment;
    }

    private String reason;
    private String userComment;
}