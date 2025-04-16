package org.edu_sharing.metadataset.v2;

import lombok.Data;

@Data
public class AiConfig {
    private String id;
    private String provider;
    private boolean useCaching;
    private String chatCompletion;
    private String createImage;
}
