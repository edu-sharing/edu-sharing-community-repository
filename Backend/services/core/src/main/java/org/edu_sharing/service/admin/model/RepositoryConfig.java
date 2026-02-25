package org.edu_sharing.service.admin.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class RepositoryConfig implements Serializable {
    private Frontpage frontpage = new Frontpage();
    private List<RepositoryMessage> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepositoryMessage implements Serializable{
        @Schema(maxLength = 256)
        @Size(max = 256)
        private List<String> contexts,toolpermissions,components;
        private UserMode userMode;
        private Mode mode;
        private Repeat repeat;
        private Severity severity;
        @Schema(description = "optional start date for message")
        private Long from;
        @Schema(description = "optional end date for message")
        private Long to;
        @Schema(description = "uuid of message")
        private UUID uuid;
        @Size(max = 51)
        @Schema(description = "Message to display", maxLength = 1024*512)
        @JsonPropertyDescription("message to display")
        private String message;
        public enum Repeat {
            @JsonPropertyDescription("show only once")
            once,
            @JsonPropertyDescription("show next time, but temporary closable")
            repeat,
            @JsonPropertyDescription("Always show, not closable (only for mode bar)")
            always
        }
        public enum Severity {
            info,
            warning,
            error
        }
        public enum UserMode {
            @JsonPropertyDescription("for all users")
            all,
            @JsonPropertyDescription("for guest users only")
            guest,
            @JsonPropertyDescription("For users only")
            user
        }
        public enum Mode {
            @JsonPropertyDescription("show as a temporary bar")
            bar,
            @JsonPropertyDescription("show as modal dialog")
            modal,

        }
    }
    @Data
    @NoArgsConstructor
    public static class Frontpage implements Serializable{
        public enum Mode{
            collection,
            rating,
            views,
            downloads
        };

        private int totalCount=50;
        private int displayCount=12;
        private Mode mode=Mode.rating;
        private int timespan=30;
        private boolean timespanAll = false;
        private List<Query> queries;
        // the id of the collection, if mode == collection
        private String collection;

        @Data
        public static class Query {
            private Condition condition=new Condition();
            private String query;
        }
    }
    @Data
    public static class Condition{
        public enum Type{
            TOOLPERMISSION
        }
        private Type type;
        private boolean negate;
        private String value;
    }
}
