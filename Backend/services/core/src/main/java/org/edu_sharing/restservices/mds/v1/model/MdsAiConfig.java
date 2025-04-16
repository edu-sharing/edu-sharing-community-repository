package org.edu_sharing.restservices.mds.v1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MdsAiConfig {
    private String id;
    private String provider;
    private boolean useCaching;
    private String chatCompletion;
    private String createImage;
}
