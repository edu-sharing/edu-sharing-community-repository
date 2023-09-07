package org.edu_sharing.rest.notification.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeDataDTO {
    private String type;
    private List<String> aspects;
    private Map<String, Object> properties;
}
