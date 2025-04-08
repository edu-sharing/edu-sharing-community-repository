package org.edu_sharing.restservices.suggestions.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSuggestionRequestDTO {
   @NotEmpty
   private String propertyId;
   @NotNull
   private Object value;
   @NotEmpty
   private String description;
   @Min(0)
   @Max(1)
   private double confidence = 0;
}
