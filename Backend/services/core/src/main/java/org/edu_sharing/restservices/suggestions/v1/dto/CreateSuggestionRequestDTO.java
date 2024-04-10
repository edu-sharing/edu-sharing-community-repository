package org.edu_sharing.restservices.suggestions.v1.dto;

import lombok.Value;
import org.edu_sharing.service.suggestion.SuggestionStatus;
import org.edu_sharing.service.suggestion.SuggestionType;

import java.io.Serializable;
import java.util.Date;

@Value
public class CreateSuggestionRequestDTO {
   String propertyId;
   Serializable value;

   String description;
   double confidence = 0;
}
