package org.edu_sharing.rest.notification.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.edu_sharing.rest.notification.data.NodeDataDTO;
import org.edu_sharing.rest.notification.data.StatusDTO;
import org.edu_sharing.rest.notification.data.UserDataDTO;
import org.edu_sharing.rest.notification.data.WidgetDataDTO;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetadataSuggestionEventDTO extends NodeBaseEventDTO {

    public MetadataSuggestionEventDTO(String id, Date timestamp, UserDataDTO creator, UserDataDTO receiver, StatusDTO status, NodeDataDTO node, String captionId, String caption, String parentId, String parentCaption, WidgetDataDTO widget) {
        super(id, timestamp, creator, receiver, status, node);
        this.captionId = captionId;
        this.caption = caption;
        this.parentId = parentId;
        this.parentCaption = parentCaption;
        this.widget = widget;
    }

    private String captionId;
    private String caption;
    private String parentId;
    private String parentCaption;
    private WidgetDataDTO widget;
}
