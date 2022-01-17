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
    private boolean clearSourceProperty;
    private MigrateMetadataValuespaceJob.Mode mode;
    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;


    private MetadataWidget widget;
    private Map<MetadataKey.MetadataKeyRelated.Relation,Map<String, Collection<MetadataKey.MetadataKeyRelated>>> relationCache;
    /**
     * Init this Interceptor with the given property information
     * @param sourceProperty the source property to read the data from
     * @param targetProperty the target property (where to map the data to)
     * @param clearSourceProperty if true, the content of the sourceProperty will be removed
     * @param relations (the relations to search for mappings, the first relation that returns result for a given value in the list will be used)
     */
    public PropertiesSetInterceptorValuespaceMapperAbstract(String sourceProperty,
                                                            String targetProperty,
                                                            boolean clearSourceProperty,
                                                            MigrateMetadataValuespaceJob.Mode mode,
                                                            List<MetadataKey.MetadataKeyRelated.Relation> relations) {
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.clearSourceProperty = clearSourceProperty;
        this.mode = mode;
        this.relations = relations;
    }

    /**
     * Set the widget
     * the widget will be used to fetch the relation mapping data
     */
    public void setWidget(MetadataWidget widget) {
        this.widget = widget;
        relationCache = new HashMap<>();
        this.relations.forEach((r) -> relationCache.put(r, widget.getValuespaceMappingByRelation(r)));
    }


    @Override
    public Map<String, Object> beforeSetProperties(PropertiesGetInterceptor.PropertiesContext context) {
        mapValues(context);
        return context.getProperties();
    }

    private void mapValues(PropertiesGetInterceptor.PropertiesContext context) {
        Map<String, Object> map = context.getProperties();
        Object value = map.get(sourceProperty);
        if(clearSourceProperty) {
            map.put(sourceProperty, null);
        }
        for(MetadataKey.MetadataKeyRelated.Relation relation : relations) {
            HashSet<String> mapped = MigrateMetadataValuespaceJob.mapValueToTarget(context.getNodeRef(),
                    relationCache.get(relation),
                    MigrateMetadataValuespaceJob.Mode.Replace,
                    value,
                    map.get(targetProperty),
                    true
            );
            if(mapped != null && !mapped.isEmpty()) {
                if(mode.equals(MigrateMetadataValuespaceJob.Mode.Merge)) {
                    Object target = map.get(targetProperty);
                    if(!(target instanceof Collection)) {
                        target = Collections.singletonList(target);
                    }
                    mapped.addAll((Collection<? extends String>) target);
                }
                map.put(targetProperty, mapped);
                return;
            }
        }
    }
}
