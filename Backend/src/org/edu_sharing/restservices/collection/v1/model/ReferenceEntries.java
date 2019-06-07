package org.edu_sharing.restservices.collection.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.edu_sharing.restservices.shared.Pagination;

import java.util.ArrayList;
import java.util.List;

public class ReferenceEntries {
    private List<CollectionReference> references = new ArrayList<CollectionReference>();
    private Pagination pagination;


    /**
     **/
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("references")
    public List<CollectionReference> getReferences() {
        return references;
    }

    public void setReferences(List<CollectionReference> references) {
        this.references = references;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
