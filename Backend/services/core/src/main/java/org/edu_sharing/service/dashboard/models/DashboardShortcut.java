package org.edu_sharing.service.dashboard.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = "type")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DashboardShortcut.DefaultDashboardShortcut.class, name = "default"),
        @JsonSubTypes.Type(value = DashboardShortcut.RefDashboardShortcut.class, name = "ref")
})
public abstract class DashboardShortcut {
    protected String title;

    @Data
    @Validated
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class DefaultDashboardShortcut extends DashboardShortcut {
        @NotEmpty
        private String id;

        public DefaultDashboardShortcut(String id, String title) {
            super(title);
            this.id = id;
        }
    }

    @Data
    @Validated
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class RefDashboardShortcut extends DashboardShortcut {

        @NotEmpty
        private String ref;
        public RefDashboardShortcut(String title, String ref) {
            super(title);
            this.ref = ref;
        }
    }
}
