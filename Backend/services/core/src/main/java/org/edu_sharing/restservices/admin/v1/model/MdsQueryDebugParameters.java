package org.edu_sharing.restservices.admin.v1.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.search.v1.model.SearchParameters;

@Data
@EqualsAndHashCode(callSuper = true)
public class MdsQueryDebugParameters extends SearchParameters {
    @Schema(description = "include the global conditions (read permissions of the current user, store, scope and special filters of the query)", defaultValue = "true")
    private boolean includeGlobalConditions = true;
    @Schema(description = "include the basequery and the conditions of the query itself", defaultValue = "true")
    private boolean includeQueryBasequery = true;
    @Schema(description = "include the basequery and the conditions of the <queries> element which are shared by all queries", defaultValue = "true")
    private boolean includeSharedBasequery = true;
    @Schema(description = "also execute the generated dsl and return the result", defaultValue = "false")
    private boolean execute = false;
}
