package org.edu_sharing.rest.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import org.edu_sharing.rest.notification.data.WidgetData;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MetadataSuggestionEventDTO extends NodeBaseEventDTO {
    private String captionId;
    private String caption;
    private String parentId;
    private String parentCaption;
    private WidgetData widget;
}
