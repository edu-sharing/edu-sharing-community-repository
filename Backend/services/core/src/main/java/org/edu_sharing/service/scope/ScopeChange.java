package org.edu_sharing.service.scope;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Entry of the change feed (mongo collection scope_change) consumed by the scope tracker (ADR-1).
 * Ordered by (at, seq).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeChange implements Serializable {
    @JsonProperty(required = true)
    private long seq;
    /** null if the change affects all scopes of the nodes (node purged) */
    private String scopeId;
    private List<String> nodeIds;
    @JsonProperty(required = true)
    private ScopeChangeType type;
    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date at;
}
