package org.edu_sharing.service.config;

import org.json.JSONObject;

public class DynamicConfig {
    private String nodeId;
    private String value;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
