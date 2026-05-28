package org.edu_sharing.metadataset.v2;

import lombok.Data;

import java.io.Serializable;

@Data
public class AiConfig implements Serializable {
    private String id;
    private String provider;
    private Boolean useCaching;
    private Boolean clearCache;
    private String prompt;
}
