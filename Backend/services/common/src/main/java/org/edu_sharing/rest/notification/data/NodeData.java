package org.edu_sharing.rest.notification.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeData {
    @Singular
    private Map<String, Object> properties;
}
