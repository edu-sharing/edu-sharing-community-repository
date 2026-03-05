package org.edu_sharing.service.nodeservice;

import org.alfresco.service.cmr.repository.NodeRef;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface PropertiesSetInterceptor {

    /**
     * called when the node properties are set/updated
     * You can add or remove data in the returned set
     */
    Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context);
    default SetInterceptorTiming getInterceptorTiming() {
        return SetInterceptorTiming.BeforeAlfrescoInterceptors;
    }

    enum SetInterceptorTiming {
        /**
         * run this interceptor before the alfresco interceptors are running
         * Note: This interceptors will not have aspects present when a node is in creation state!
         */
        BeforeAlfrescoInterceptors,
        /**
         * run this interceptor after the alfresco interecptors ran
         * Note: This type is more expensive since it needs to re-fetch properties from alfresco
         */
        AfterAlfrescoInterceptors,
        /**
         * run this interceptor after the properties have been already saved
         * Please note that in this stage, you might only trigger manual storage actions (like calling the NodeService manually)
         * The returned properties will be ignored and not set automatically
         */
        AfterPropertiesSet,
        /**
         * run this interceptor at any stage
         * You need to filter the current stage manually using the PropertiesContext
         * Note: Use the context object details to find out the state and keep track of custom state data
         */
        All,
    }

    enum ContextStage {
        BeforeAlfrescoInterceptors,
        AfterAlfrescoInterceptors,
        AfterPropertiesSet
    }
}
