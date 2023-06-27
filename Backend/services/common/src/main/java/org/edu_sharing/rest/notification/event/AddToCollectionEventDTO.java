package org.edu_sharing.rest.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.edu_sharing.rest.notification.data.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddToCollectionEventDTO extends NodeBaseEventDTO {
    /**
     * the collection the node has been added to
     */
    private Collection collection;
}