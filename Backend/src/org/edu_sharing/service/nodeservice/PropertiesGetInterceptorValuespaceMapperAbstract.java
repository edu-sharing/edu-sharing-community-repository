package org.edu_sharing.service.nodeservice;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.util.HashMap;
import java.util.Map;

public abstract class PropertiesGetInterceptorValuespaceMapperAbstract extends PropertiesGetInterceptorDefault {
    protected final MigrateMetadataValuespaceJob.Mode mode;
    /**
     * widget class
     */
    protected MetadataWidget widget;
    /**
     * property as defined in CCConstants
     */
    protected final String sourceProperty;
    protected final String targetProperty;
    protected final MetadataKey.MetadataKeyRelated.Relation relation;
    private Map<MetadataKey.MetadataKeyRelated, MetadataKey> relationCache;

    protected PropertiesGetInterceptorValuespaceMapperAbstract(MigrateMetadataValuespaceJob.Mode mode,
                                                               String sourceProperty,
                                                               String targetProperty,
                                                               MetadataKey.MetadataKeyRelated.Relation relation) {
        this.mode = mode;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.relation = relation;
    }

    public void setWidget(MetadataWidget widget) {
        this.widget = widget;
        this.relationCache = widget.getValuespaceMappingByRelation(relation);
    }

    /**
     * @TODO: may use beforeCacheProperties for higher performance
     */
    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        try {
            if(this.widget == null) {
                return super.beforeDeliverProperties(context);
            }
            if(!this.shouldProcess(context)) {
                return super.beforeDeliverProperties(context);
            }
            return this.onMap(context, widget);
        } catch (Exception ignored) {
        }
        return super.beforeDeliverProperties(context);
    }

    /**
     * override to control whether the input object is eligible and shall be processed
     * e.g. you can filter that only nodes with a particular mds shall be processed
     */
    protected abstract boolean shouldProcess(PropertiesContext context);

    /**
     * override this method to provide more advanced mapping logic
     */
    protected Map<String, Object> onMap(PropertiesContext context, MetadataWidget widget) {
        Map<String, Object> map = super.beforeDeliverProperties(context);
        map.put(targetProperty,
                MigrateMetadataValuespaceJob.mapValueToTarget(
                        context.getNodeRef(),
                        relationCache,
                        mode,
                        map.get(sourceProperty),
                        map.get(targetProperty)
                ));
        return map;
    }

}
