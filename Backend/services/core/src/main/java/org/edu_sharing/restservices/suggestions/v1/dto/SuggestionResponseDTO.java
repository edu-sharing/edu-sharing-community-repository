package org.edu_sharing.restservices.suggestions.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionResponseDTO {
   private String id;

   private String propertyId;
   private Object value;

   private SuggestionType type;
   private SuggestionStatus status;
   private String description;
   private double confidence = 0;

   private Date created;
   private String createdBy;
   private Date modified;
   private String modifiedBy;
}
