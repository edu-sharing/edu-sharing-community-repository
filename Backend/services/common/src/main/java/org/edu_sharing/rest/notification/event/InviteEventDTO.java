package org.edu_sharing.rest.notification.event;

import lombok.*;
import org.edu_sharing.rest.notification.data.NodeDataDTO;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.data.UserDataDTO;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InviteEventDTO extends NodeBaseEventDTO {
    public InviteEventDTO(String id, Date timestamp, UserDataDTO creator, UserDataDTO receiver, StatusDTO status, NodeDataDTO node, String name, String type, String userComment, List<String> permissions) {
        super(id, timestamp, creator, receiver, status, node);
        this.name = name;
        this.type = type;
        this.userComment = userComment;
        this.permissions = permissions;
    }

    private String name;
    private String type;
    private String userComment;
    private List<String> permissions;
}