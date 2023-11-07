package org.edu_sharing.rest.notification.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CollectionDTO extends NodeDataDTO {
    public CollectionDTO(String type, List<String> aspects, Map<String, Object> properties) {
        super(type, aspects, properties);
    }
}
