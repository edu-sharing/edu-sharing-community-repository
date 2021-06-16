package org.edu_sharing.service.rendering;

import org.edu_sharing.restservices.shared.*;
import org.edu_sharing.alfresco.service.config.model.Values;

import java.io.Serializable;
import java.util.List;

public class RenderingServiceData implements Serializable {
    private Node node;
    private List<Node> children;
    private UserRender user;
    private String metadataHTML;
    private Values configValues;

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void setUser(UserRender user) {
        this.user = user;
    }

    public UserRender getUser() {
        return user;
    }

    public void setMetadataHTML(String metadataHTML) {
        this.metadataHTML = metadataHTML;
    }

    public String getMetadataHTML() {
        return metadataHTML;
    }

    public void setConfigValues(Values configValues) {
        this.configValues = configValues;
    }

    public Values getConfigValues() {
        return configValues;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }

    public List<Node> getChildren() {
        return children;
    }
}
