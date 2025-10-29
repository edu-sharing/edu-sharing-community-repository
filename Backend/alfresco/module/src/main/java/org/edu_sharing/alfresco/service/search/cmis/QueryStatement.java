package org.edu_sharing.alfresco.service.search.cmis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class QueryStatement {
    private final Selection selection;
    private final String from;
    @Setter(AccessLevel.PROTECTED)
    private Predicate where;
    private final List<Aspect> aspects = new ArrayList<>();

    protected void addAspect(Aspect aspect){
        aspects.add(aspect);
    }

    public List<Aspect> getAspects() {
        return Collections.unmodifiableList(aspects);
    }
}
