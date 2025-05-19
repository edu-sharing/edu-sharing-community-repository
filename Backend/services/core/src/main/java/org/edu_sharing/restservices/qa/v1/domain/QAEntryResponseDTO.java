package org.edu_sharing.restservices.qa.v1.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.restservices.shared.UserSimple;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QAEntryResponseDTO {
    @JsonProperty(required = true)
    String id;
    @JsonProperty(required = true)
    String nodeId;

    @JsonProperty(required = true)
    String question;
    @JsonProperty(required = true)
    String answer;

    String usedText;
    String educationalLevel;

    @JsonProperty(required = true)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    Date created;
    @JsonProperty(required = true)
    UserSimple createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX")
    Date lastReviewed;
    UserSimple reviewedBy;

    @JsonProperty(required = true)
    boolean modified;
}
