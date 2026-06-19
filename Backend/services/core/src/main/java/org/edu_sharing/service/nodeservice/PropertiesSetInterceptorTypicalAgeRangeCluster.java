package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.client.tools.CCConstants;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Interceptor that handles copying data from ccm:educationaltypicalagerangecluster into ccm:educationaltypicalagerange
 */
public class PropertiesSetInterceptorTypicalAgeRangeCluster implements PropertiesSetInterceptor {

    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        Map<String, Object> properties = context.getProperties();
        Object clusterValue = properties.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGECLUSTER);
        if (clusterValue == null) {
            return properties;
        }

        Collection<?> clusters = clusterValue instanceof Collection
                ? (Collection<?>) clusterValue
                : Collections.singletonList(clusterValue.toString());

        OptionalInt min = clusters.stream()
                .map(v -> v.toString().split("-"))
                .filter(p -> p.length == 2)
                .mapToInt(p -> Integer.parseInt(p[0]))
                .min();
        OptionalInt max = clusters.stream()
                .map(v -> v.toString().split("-"))
                .filter(p -> p.length == 2)
                .mapToInt(p -> Integer.parseInt(p[1]))
                .max();

        if (min.isPresent() && max.isPresent()) {
            properties.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE, min.getAsInt() + "-" + max.getAsInt());
            properties.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM, String.valueOf(min.getAsInt()));
            properties.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO, String.valueOf(max.getAsInt()));
        }

        return properties;
    }

    @Override
    public SetInterceptorTiming getInterceptorTiming() {
        return SetInterceptorTiming.AfterAlfrescoInterceptors;
    }
}
