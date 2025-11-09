package org.edu_sharing.service.authentication.sso.mapping;

import com.typesafe.config.Optional;
import lombok.Data;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;


@Data
public class Mapping {
    @Optional
    boolean preferRemoteUser = true;
    private final BidiMap<String, String> person = new DualHashBidiMap<>();
    private final BidiMap<String, Group> group = new DualHashBidiMap<>();

    @Data
    public final static class Group {
        @Optional
        private String group;
        private String displayName;
        @Optional
        private String parentGroup;
        private String attribute;
        private String matcher;
    }
}
