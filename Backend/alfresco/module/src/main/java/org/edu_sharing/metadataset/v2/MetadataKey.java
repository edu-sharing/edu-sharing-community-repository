package org.edu_sharing.metadataset.v2;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MetadataKey extends MetadataTranslatable {

    @Getter
    public static class MetadataKeyRelated extends MetadataKey {
        public enum Relation {
            exactMatch,
            narrowMatch,
            relatedMatch,
            closeMatch,
            broadMatch,
        }

        private final Relation relation;

        public MetadataKeyRelated(Relation relation) {
            this.relation = relation;
        }

    }
    private String key;
    private String caption;
	private String icon;
    private String description;
    private String parent;
    private String locale;
    /**
     * List of other keys this child is a precedor of
     */
    private List<String> preceds;
    private final List<MetadataKeyRelated> related = new ArrayList<>();
    private List<String> alternativeKeys;
    private String url;

    public void setParent(String parent) {
        if (StringUtils.isBlank(parent)) {
            this.parent = null;
        } else {
            this.parent = parent;
        }
    }

    public void addRelated(MetadataKeyRelated related) {
        this.related.add(related);
    }
}
