package org.edu_sharing.service.authentication.sso.mapping;

import com.typesafe.config.Optional;
import lombok.Data;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.ArrayList;
import java.util.List;


@Data
public class Mapping {
    @Optional
    boolean preferRemoteUser = true;
    private final BidiMap<String, String> person = new DualHashBidiMap<>();
    private final BidiMap<String, Group> group = new DualHashBidiMap<>();

    /**
     * used by external auth (i.e. shibboleth serviceprovider) where attributes are provided over ajp.
     * apj provided attributes are not present in request.getAttributNames() method
     */
    @Optional
    private List<String> additionalAttributes = new ArrayList<>();

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
