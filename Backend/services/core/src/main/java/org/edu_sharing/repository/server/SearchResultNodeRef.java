package org.edu_sharing.repository.server;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.edu_sharing.metadataset.v2.SearchCriterias;
import org.edu_sharing.repository.client.rpc.Result;
import org.edu_sharing.restservices.shared.NodeSearch;
import org.edu_sharing.service.model.NodeRef;

public class SearchResultNodeRef extends SearchResult<NodeRef> {
}
