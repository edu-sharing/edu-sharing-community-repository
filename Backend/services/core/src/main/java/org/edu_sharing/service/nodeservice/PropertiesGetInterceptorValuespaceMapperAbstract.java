package org.edu_sharing.service.nodeservice;

import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.metadataset.v2.MetadataWidget;
import org.edu_sharing.repository.server.jobs.quartz.MigrateMetadataValuespaceJob;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class PropertiesGetInterceptorValuespaceMapperAbstract extends PropertiesGetInterceptorDefault {
    protected final MigrateMetadataValuespaceJob.Mode mode;
    /**
     * widget class
     */
    protected MetadataWidget sourceWidget;
    protected MetadataWidget targetWidget;
    /**
     * property as defined in CCConstants
     */
    protected final String sourceProperty;
    protected final String targetProperty;
    protected final MetadataKey.MetadataKeyRelated.Relation relation;
    private Map<String, Collection<MetadataKey.MetadataKeyRelated>> relationCache;

    /**
     * Init this Interceptor with the given property information
     * @param mode The mode to use in order to fill the target property
     * @param sourceProperty the source property to read the data from
     * @param targetProperty the target property (where to map the data to)
     * @param relation The relation to use
     */
    protected PropertiesGetInterceptorValuespaceMapperAbstract(MigrateMetadataValuespaceJob.Mode mode,
                                                               String sourceProperty,
                                                               String targetProperty,
                                                               MetadataKey.MetadataKeyRelated.Relation relation) {
        this.mode = mode;
        this.sourceProperty = sourceProperty;
        this.targetProperty = targetProperty;
        this.relation = relation;
    }

    /**
     * Set the widget
     * the widget will be used to fetch the relation mapping data
     * The target widget is optional
     * if it is set, the interceptor will make sure that only valid mappings for the target valuespace are added
     */
    public void setWidget(MetadataWidget sourceWidget, MetadataWidget targetWidget) {
        this.sourceWidget = sourceWidget;
        this.targetWidget = targetWidget;
        this.relationCache = this.sourceWidget.getValuespaceMappingByRelation(relation);
    }

    /**
     * @TODO: may use beforeCacheProperties for higher performance
     */
    @Override
    public Map<String, Object> beforeDeliverProperties(PropertiesContext context) {
        try {
            if(this.sourceWidget == null) {
                return super.beforeDeliverProperties(context);
            }
            if(!this.shouldProcess(context)) {
                return super.beforeDeliverProperties(context);
            }
            return this.onMap(context, sourceWidget);
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
                        Collections.singletonList(relationCache),
                        this.targetWidget.getValues(),
                        mode,
                        map.get(sourceProperty),
                        map.get(targetProperty),
                        false,
                        null
                ));
        return map;
    }

}
