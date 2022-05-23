package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;

import java.util.Collection;
import java.util.Map;

public interface PropertiesGetInterceptor {

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
         * the context source of the node
         * this can be useful because you might don't want to do "expensive" taks e.g. in the search context
         */
        private CallSourceHelper.CallSource source;

        public NodeRef getNodeRef() {
            return nodeRef;
        }

        public void setNodeRef(NodeRef nodeRef) {
            this.nodeRef = nodeRef;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public Collection<String> getAspects() {
            return aspects;
        }

        public void setAspects(Collection<String> aspects) {
            this.aspects = aspects;
        }

        public CallSourceHelper.CallSource getSource() {
            return source;
        }

        public void setSource(CallSourceHelper.CallSource source) {
            this.source = source;
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
