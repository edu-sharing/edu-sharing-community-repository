package org.edu_sharing.service.scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * One change of one property of an assignment (mongo collection scope_assignment_history, append only).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScopeHistoryEntry implements Serializable {
    private String scopeId;
    private String nodeId;
    /** local property name, null for the deletion of the whole assignment */
    private String property;
    private List<String> from;
    private List<String> to;
    /** username, "skill:&lt;id&gt;" or "bulk:&lt;jobId&gt;" */
    private String actor;
    private Date at;
    private String reason;
    private Long baseQueryVersion;
    /** cm:versionLabel of the original at the time of the change */
    private String contentVersion;
}
