package org.edu_sharing.service.rendering;

import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.repository.client.tools.I18nAngular;
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

    private NodeUrls nodeUrls;
    
    
    private List<Editor> editors;

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

    public void setNodeUrls(NodeUrls nodeUrls) {
        this.nodeUrls = nodeUrls;
    }

    public NodeUrls getNodeUrls() {
        return nodeUrls;
    }

    public void setEditors(List<Editor> editors) {
        this.editors = editors;
    }

    public List<Editor> getEditors() {
        return editors;
    }

    static class Editor implements Serializable {

        private String id;
        private String label;
        private boolean onlyDesktop;

        @JsonProperty
        public void setId(String id) {
            this.id = id;
            this.label = I18nAngular.getTranslationAngular("common", "CONNECTOR." + getId() + ".NAME");
        }

        public String getId() {
            return id;
        }
        public String getLabel() {
            return label;
        }

        public void setOnlyDesktop(boolean onlyDesktop) {
            this.onlyDesktop = onlyDesktop;
        }

        public boolean isOnlyDesktop() {
            return onlyDesktop;
        }
    }
}
