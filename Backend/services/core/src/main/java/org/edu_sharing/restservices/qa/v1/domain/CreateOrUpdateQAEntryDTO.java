package org.edu_sharing.restservices.qa.v1.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateQAEntryDTO {

    String id;

    @NotEmpty
    @JsonProperty(required = true)
    String question;

    @NotEmpty
    @JsonProperty(required = true)
    String answer;

    String usedText;
    String educationalLevel;
}
