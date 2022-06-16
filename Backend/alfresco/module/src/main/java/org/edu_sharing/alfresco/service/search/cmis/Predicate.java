package org.edu_sharing.alfresco.service.search.cmis;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Predicate extends Argument {
    @NonNull
    private final String operation;
    private final Argument lhs;
    private final Argument rhs;
}
