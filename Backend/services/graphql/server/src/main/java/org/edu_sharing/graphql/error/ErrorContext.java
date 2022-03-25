package org.edu_sharing.graphql.error;

import graphql.ErrorClassification;
import graphql.language.SourceLocation;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class ErrorContext {

    List<SourceLocation> locations;
    List<Object> path;
    Map<String, Object> extensions;
    ErrorClassification errorType;
}
