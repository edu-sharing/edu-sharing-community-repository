package org.edu_sharing.service.nodeservice;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.authentication.NetworkAuthentication;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.io.Serializable;
import java.util.*;

import static org.activiti.engine.impl.util.CollectionUtil.map;

/**
 * Interceptor that will remove/ignore all configured properties
 */
public class PropertiesSetInterceptorValuespaceMapperAbstract implements PropertiesSetInterceptor{
    private String sourceProperty;
    private String targetProperty;
    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;


    private MetadataWidget widget;
    private Map<MetadataKey.MetadataKeyRelated.Relation,Map<String, Collection<MetadataKey.MetadataKeyRelated>>> relationCache;
    /**
     * Init this Interceptor with the given property information
     * @param sourceProperty the source property to read the data from
     * @param targetProperty the target property (where to map the data to)
     * @param relations (the relations to search for mappings, the first relation that returns result for a given value in the list will be used)
     */
    public PropertiesSetInterceptorValuespaceMapperAbstract(String sourceProperty,
                                                            String targetProperty,
                                                            List<MetadataKey.MetadataKeyRelated.Relation> relations) {
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.relations = relations;
    }

    /**
     * Set the widget
     * the widget will be used to fetch the relation mapping data
     */
    public void setWidget(MetadataWidget widget) {
        this.widget = widget;
        this.relations.forEach((r) -> relationCache.put(r, widget.getValuespaceMappingByRelation(r)));
    }


    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        mapValues(context);
        return context.getProperties();
    }

    private void mapValues(PropertiesGetInterceptor.PropertiesContext context) {
        Map<String, Object> map = context.getProperties();
        for(MetadataKey.MetadataKeyRelated.Relation relation : relations) {
            HashSet<String> mapped = MigrateMetadataValuespaceJob.mapValueToTarget(context.getNodeRef(),
                    relationCache.get(relation),
                    MigrateMetadataValuespaceJob.Mode.Replace,
                    map.get(sourceProperty),
                    map.get(targetProperty),
                    true
            );
            if(mapped != null && !mapped.isEmpty()) {
                map.put(targetProperty, mapped);
                return;
            }
        }
    }

    @Override
    public Serializable beforeSetProperty(PropertiesGetInterceptor.PropertiesContext context, String property) {
        mapValues(context);
        return (Serializable) context.getProperties().get(property);
    }
}
