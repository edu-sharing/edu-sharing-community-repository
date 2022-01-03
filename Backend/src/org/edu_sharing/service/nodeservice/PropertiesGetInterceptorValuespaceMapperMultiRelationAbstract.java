package org.edu_sharing.service.nodeservice;

import org.apache.log4j.Logger;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;
import org.edu_sharing.service.foldertemplates.LoggingErrorHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * this interceptor will map the first relation given in the input list which yields one or more hits
 */
public abstract class PropertiesGetInterceptorValuespaceMapperMultiRelationAbstract extends PropertiesGetInterceptorValuespaceMapperAbstract {

    private final List<MetadataKey.MetadataKeyRelated.Relation> relations;
    private Map<MetadataKey.MetadataKeyRelated.Relation,Map<MetadataKey.MetadataKeyRelated, MetadataKey>> relationCache = new HashMap<>();
    private Logger logger = Logger.getLogger(PropertiesGetInterceptorValuespaceMapperMultiRelationAbstract.class);

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
    public void setWidget(MetadataWidget widget) {
        super.setWidget(widget);
        this.relations.forEach((r) -> relationCache.put(r, widget.getValuespaceMappingByRelation(r)));
    }

    /**
     * override this method to provide more advanced mapping logic
     */
    protected Map<String, Object> onMap(PropertiesContext context, MetadataWidget widget) {
        Map<String, Object> map = context.getProperties();
        if(relationCache == null) {
            logger.error("setWidget() was not called");
        }
        for(MetadataKey.MetadataKeyRelated.Relation relation : relations) {
            HashSet<String> mapped = MigrateMetadataValuespaceJob.mapValueToTarget(
                    context.getNodeRef(),
                    relationCache.get(relation),
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
