package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.UserEventDao;
import org.edu_sharing.restservices.shared.NodeSearch.Facet;

import java.util.ArrayList;
import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResultEvent extends SearchResult<UserEventDao.UserEvent> {
}
