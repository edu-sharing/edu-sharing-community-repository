package org.edu_sharing.service.tracking.ibatis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.json.JSONObject;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NodeTrackingEntry {
    private Long nodeId;
    private String node_uuid;
    private String original_node_uuid;
    private String node_version;
    private String authority;
    private String[] authority_organization;
    private String[] authority_mediacenter;
    private Date time;
    private ActivityOnNodeEventType type;
    private JSONObject data;
    private String license;
    private String[] shared_with_mediacenters;
}
