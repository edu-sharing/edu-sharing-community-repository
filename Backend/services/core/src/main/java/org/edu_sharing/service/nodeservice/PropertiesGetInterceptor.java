package org.edu_sharing.service.nodeservice;

import lombok.Getter;
import lombok.Setter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.permission.PermissionServiceHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public interface PropertiesGetInterceptor {
    @Getter
    @Setter
    class PropertiesContext {
        private NodeRef nodeRef;
        /**
         * the current properties of the node
         */
        Map<String, Object> properties;
        /**
         * the current aspects of the node
         */
        Collection<String> aspects;
        /**
         * the permissions of the node for the current user
         * Hint: Might be null depending on the source!
         */
        Map<String, Boolean> permissions;
        /**
         * custom payload for interceptors
         * can be used for set interceptors which may run before and after alfresco props set to keep their own state
         */
        Map<String, Object> customPayload = new HashMap<>();
        /**
         * the context source of the node
         * this can be useful because you might don't want to do "expensive" taks e.g. in the search context
         */
        private CallSourceHelper.CallSource source;
        /**
         * the stage (before or after alfresco properties have been set - only for set interceptors)
         */
        private PropertiesSetInterceptor.ContextStage stage;
        /**
         * The elasticsearch source map
         * Hint: Only Non-Null if data was fetched via elasticsearch!
         */
        private Map<String, Object> elasticsearchSource;

        /**
         * checks if a given permissions is available for the node
         * Uses the permission cache if available
         */
        protected boolean hasPermission(String permission) {
            if(permissions != null) {
                return getPermissions().getOrDefault(permission, false);
            } else {
                return PermissionServiceHelper.hasPermission(getNodeRef(), permission);
            }
        }
    }

    /**
     * called when the internal properties fetching has finished and the properties are about to get cached
     * You can add data to the properties Map which will also be cached for faster access later on
     */
    Map<String, Object> beforeCacheProperties(PropertiesContext context);
    /**
     * called when the internal properties have been fetched (this can be from cache or directly, depending on the state)
     * This endpoint will be called AFTER beforeCacheProperties. You can may filter properties you've cached previously
     * e.g. if the current user might not have all permissions to access them
     */
    Map<String, Object> beforeDeliverProperties(PropertiesContext context);
}
