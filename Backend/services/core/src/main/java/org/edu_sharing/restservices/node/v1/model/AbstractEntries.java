package org.edu_sharing.restservices.node.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

public class AbstractEntries<T>  {

    private List<T> nodes = new ArrayList<>();
    private Pagination pagination = null;

    public AbstractEntries(){
    }

    /**
     **/
    @Schema(required = true, description = "")
    @JsonProperty("nodes")
    public List<T> getNodes() {
        return nodes;
    }
    public void setNodes(List<T> nodes) {
        this.nodes = nodes;
    }

    /**
     **/
    @Schema(required = true, description = "")
    @JsonProperty("pagination")
    public Pagination getPagination() {
        return pagination;
    }
    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("class NodeEntries {\n");

        sb.append("  nodes: ").append(nodes).append("\n");
        sb.append("  pagination: ").append(pagination).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
