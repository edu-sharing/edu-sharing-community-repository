package org.edu_sharing.metadataset.v2;

import lombok.Data;
import org.apache.xpath.operations.Bool;

import java.io.Serializable;

@Data
public class AiConfig implements Serializable {
    private String id;
    private String provider;
    private Boolean useCaching;
    private String chatCompletion;
    private String createImage;
}
