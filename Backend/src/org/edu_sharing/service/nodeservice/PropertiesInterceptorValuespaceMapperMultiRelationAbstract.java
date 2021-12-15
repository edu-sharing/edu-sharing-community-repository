package org.edu_sharing.service.nodeservice;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * this interceptor will map the first relation given in the input list which yields one or more hits
 */
public abstract class PropertiesInterceptorValuespaceMapperMultiRelationAbstract extends PropertiesInterceptorValuespaceMapperAbstract {

    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;

    protected PropertiesInterceptorValuespaceMapperMultiRelationAbstract(
                                                               String sourceProperty,
                                                               String targetProperty,
                                                               List<MetadataKey.MetadataKeyRelated.Relation> relations) {
        super(MigrateMetadataValuespaceJob.Mode.Replace,
                sourceProperty, targetProperty, null
        );
        this.relations = relations;
    }


    /**
     * override this method to provide more advanced mapping logic
     */
    protected Map<String, Object> onMap(PropertiesContext context, MetadataWidget widget) {
        Map<String, Object> map = super.beforeDeliverProperties(context);
        for(MetadataKey.MetadataKeyRelated.Relation relation : relations) {
            HashSet<String> mapped = MigrateMetadataValuespaceJob.mapValueToTarget(
                    context.getNodeRef(),
                    widget.getValuespaceMappingByRelation(relation),
                    mode,
                    map.get(sourceProperty),
                    map.get(targetProperty)
            );
            if(mapped != null && !mapped.isEmpty()) {
                map.put(targetProperty, mapped);
                return map;
            }
        }
        return map;
    }

}
