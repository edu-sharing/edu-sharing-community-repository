package org.edu_sharing.restservices.suggestions.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.restservices.shared.UserSimple;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponseDTO {
    @JsonProperty(required = true)
    private String id;

    @JsonProperty(required = true)
    private String nodeId;

    @JsonProperty(required = true)
    private String version;

    @JsonProperty(required = true)
    private String propertyId;

    @JsonProperty(required = true)
    private Object value;

    @JsonProperty(required = true)
    private SuggestionType type;

    @JsonProperty(required = true)
    private SuggestionStatus status;

    private String description;

    @JsonProperty(required = true)
    private double confidence = 0;

    @JsonProperty(required = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date created;

    @JsonProperty(required = true)
    private UserSimple createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private Date modified;
    private UserSimple modifiedBy;
}
