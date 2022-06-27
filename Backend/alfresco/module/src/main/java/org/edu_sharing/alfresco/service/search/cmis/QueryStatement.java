package org.edu_sharing.alfresco.service.search.cmis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class QueryStatement {
    private final Selection selection;
    private final String from;
    @Setter(AccessLevel.PROTECTED)
    private Predicate where;
}
