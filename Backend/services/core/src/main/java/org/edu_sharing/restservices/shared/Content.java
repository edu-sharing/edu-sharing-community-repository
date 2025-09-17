package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.io.Serializable;

@Data
public class Content implements Serializable {
    private String url;
    @JsonPropertyDescription("Primary url of the original; Might be null if the element is NOT a collection reference")
    private String originalUrl;
    private String hash;
    private String version;

}
