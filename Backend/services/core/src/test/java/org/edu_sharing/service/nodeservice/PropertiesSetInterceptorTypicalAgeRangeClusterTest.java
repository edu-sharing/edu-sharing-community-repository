package org.edu_sharing.service.nodeservice;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PropertiesSetInterceptorTypicalAgeRangeClusterTest {

    private PropertiesSetInterceptorTypicalAgeRangeCluster interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new PropertiesSetInterceptorTypicalAgeRangeCluster();
    }

    private Map<String, Object> runWithClusters(Object clusterValue) {
        PropertiesGetInterceptor.PropertiesContext context = new PropertiesGetInterceptor.PropertiesContext();
        Map<String, Object> props = new HashMap<>();
        if (clusterValue != null) {
            props.put(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGECLUSTER, clusterValue);
        }
        context.setProperties(props);
        return interceptor.beforeSetProperties(context);
    }

    @Test
    void singleCluster_derivesRange() {
        Map<String, Object> result = runWithClusters(Arrays.asList("5-6"));
        assertEquals("5-6", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        assertEquals("5", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
        assertEquals("6", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
    }

    @Test
    void multipleClusters_derivesMinMaxRange() {
        Map<String, Object> result = runWithClusters(Arrays.asList("8-10", "5-6", "14-16"));
        assertEquals("5-16", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        assertEquals("5", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
        assertEquals("16", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
    }

    @Test
    void allClusters_spansFullRange() {
        Map<String, Object> result = runWithClusters(Arrays.asList("5-6", "6-8", "8-10", "10-12", "12-14", "14-16", "16-19", "19-99"));
        assertEquals("5-99", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        assertEquals("5", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
        assertEquals("99", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
    }

    @Test
    void noCluster_doesNotSetRange() {
        Map<String, Object> result = runWithClusters(null);
        assertFalse(result.containsKey(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        assertFalse(result.containsKey(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
        assertFalse(result.containsKey(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
    }

    @Test
    void stringClusterValue_derivesRange() {
        Map<String, Object> result = runWithClusters("10-12");
        assertEquals("10-12", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE));
        assertEquals("10", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM));
        assertEquals("12", result.get(CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO));
    }
}
