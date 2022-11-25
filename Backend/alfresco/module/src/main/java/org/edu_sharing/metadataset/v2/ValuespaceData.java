package org.edu_sharing.metadataset.v2;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.List;

public class ValuespaceData implements Serializable {
    private static Logger logger = Logger.getLogger(ValuespaceData.class);

    private MetadataKey title;
    private List<MetadataKey> entries;

    public ValuespaceData(MetadataKey title, List<MetadataKey> entries) {
        this.title = title;
        this.entries = entries;
    }

    public MetadataKey getTitle() {
        return title;
    }

    public void setTitle(MetadataKey title) {
        this.title = title;
    }

    public List<MetadataKey> getEntries() {
        return entries;
    }

    public void setEntries(List<MetadataKey> entries) {
        this.entries = entries;
    }

    public List<MetadataKey> sort(MetadataWidget widget) {
        if(!"default".equals(widget.getValuespaceSort())){
            entries.sort((o1, o2) -> {
                if ("caption".equals(widget.getValuespaceSort())) {
                    return StringUtils.compareIgnoreCase(o1.getCaption(), o2.getCaption());
                }
                logger.error("Invalid value for valuespaceSort '" + widget.getValuespaceSort() + "' for widget '" + widget.getId() + "'");
                return 0;
            });
        }
        return entries;
    }
}
