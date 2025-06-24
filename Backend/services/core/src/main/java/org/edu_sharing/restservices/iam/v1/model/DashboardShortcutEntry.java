package org.edu_sharing.restservices.iam.v1.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.*;
import org.edu_sharing.restservices.shared.Node;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = "type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DashboardShortcutEntry.DefaultDashboardShortcutEntry.class, name = "default"),
        @JsonSubTypes.Type(value = DashboardShortcutEntry.RefDashboardShortcutEntry.class, name = "ref")
})
public abstract class DashboardShortcutEntry {
    protected String title;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class DefaultDashboardShortcutEntry extends DashboardShortcutEntry {
        private String id;

        public DefaultDashboardShortcutEntry(String id, String title) {
            super(title);
            this.id = id;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class RefDashboardShortcutEntry extends DashboardShortcutEntry {
        private Node node;

        public RefDashboardShortcutEntry(String title, Node node) {
            super(title);
            this.node = node;
        }
    }
}
