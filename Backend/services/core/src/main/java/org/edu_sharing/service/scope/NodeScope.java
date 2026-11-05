package org.edu_sharing.service.scope;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Scope entry of a node as visible for the current user (F5, concept §5.4), part of the node dto.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NodeScope")
public class NodeScope implements Serializable {
    private String scopeId;
    private String displayName;
    private boolean published;
    private boolean excluded;
    /** visible scope properties (local names) */
    private Map<String, List<String>> properties;
    /**
     * original values of the properties replaced in the active scope, only for editors of the own scope.
     * The node properties contain the replaced (effective) values
     */
    private Map<String, List<String>> originals;
}
