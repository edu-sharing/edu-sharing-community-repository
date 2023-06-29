package org.edu_sharing.rest.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.edu_sharing.rest.notification.data.CollectionDTO;
import org.edu_sharing.rest.notification.data.NodeDataDTO;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.data.UserDataDTO;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddToCollectionEventDTO extends NodeBaseEventDTO {
    public AddToCollectionEventDTO(String id, Date timestamp, UserDataDTO creator, UserDataDTO receiver, StatusDTO status, NodeDataDTO node, CollectionDTO collection) {
        super(id, timestamp, creator, receiver, status, node);
        this.collection = collection;
    }

    /**
     * the collection the node has been added to
     */
    private CollectionDTO collection;
}