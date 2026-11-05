package org.edu_sharing.restservices.search.v1.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.service.scope.ScopeRole;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class SearchParameters extends SearchParametersFacets {

	private List<String> permissions;
	private boolean resolveCollections = false;
	private boolean resolveUsernames = false;
	private boolean returnSuggestions = false;
	private List<String> excludes = new ArrayList<>();
	@Schema(description = "scopes: search the active scope with this role instead of the own one. Only reduces: an editor may request "
			+ "a reader role (\"view as reader\": only published content), other roles are never changed")
	private ScopeRole scopeRole;

	@Override
	@JsonProperty(required = false) // explicit set required false
	public List<SearchFacet> getFacets() {
		return super.getFacets();
	}
}
