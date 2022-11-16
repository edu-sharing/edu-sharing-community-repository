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
}
