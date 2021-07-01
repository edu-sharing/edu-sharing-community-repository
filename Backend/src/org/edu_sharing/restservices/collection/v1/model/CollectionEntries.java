package org.edu_sharing.restservices.collection.v1.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.Pagination;

@ApiModel(description = "")
public class CollectionEntries extends CollectionAbstractEntries<Node> {

}
