package org.edu_sharing.service.nodeservice;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.util.*;
import java.util.stream.Collectors;

/**
 * this interceptor will map the first relation given in the input list which yields one or more hits
 */
public abstract class PropertiesGetInterceptorValuespaceMapperMultiRelationAbstract extends PropertiesGetInterceptorValuespaceMapperAbstract {

    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;
    private Map<MetadataKey.MetadataKeyRelated.Relation,Map<String, Collection<MetadataKey.MetadataKeyRelated>>> relationCache;
    private Logger logger = Logger.getLogger(PropertiesGetInterceptorValuespaceMapperMultiRelationAbstract.class);

    /**
     * Init this Interceptor with the given property information
     * @param sourceProperty the source property to read the data from
     * @param targetProperty the target property (where to map the data to)
     * @param relations (the relations to search for mappings, the first relation that returns result for a given value in the list will be used)
     */
    protected PropertiesGetInterceptorValuespaceMapperMultiRelationAbstract(
                                                               String sourceProperty,
                                                               String targetProperty,
                                                               List<MetadataKey.MetadataKeyRelated.Relation> relations) {
        super(MigrateMetadataValuespaceJob.Mode.Replace,
                sourceProperty, targetProperty, null
        );
        this.relations = relations;
    }

    @Override
    public void setWidget(MetadataWidget sourceWidget, MetadataWidget targetWidget) {
        super.setWidget(sourceWidget, targetWidget);
        relationCache = new HashMap<>();
        this.relations.forEach((r) -> relationCache.put(r, sourceWidget.getValuespaceMappingByRelation(r)));
    }

    /**
     * override this method to provide more advanced mapping logic
     */
    protected Map<String, Object> onMap(PropertiesContext context, MetadataWidget widget) {
        Map<String, Object> map = context.getProperties();
        if(relationCache == null) {
            logger.error("setWidget() was not called");
        }
        List<Map<String, Collection<MetadataKey.MetadataKeyRelated>>> mappedList = relations.stream().map((m) -> relationCache.get(m)).collect(Collectors.toList());

        HashSet<String> mapped = MigrateMetadataValuespaceJob.mapValueToTarget(
                context.getNodeRef(),
                mappedList,
                targetWidget.getValues(),
                mode,
                map.get(sourceProperty),
                map.get(targetProperty),
                false,
                null
        );
        if(mapped != null && !mapped.isEmpty()) {
            map.put(targetProperty, mapped);
            return map;
        }
        return map;
    }

}
