package org.edu_sharing.service.contributor;

import org.edu_sharing.service.search.SearchService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fallback {@link ContributorService} for application contexts that do not maintain a contributor
 * registry (e.g. remote repositories). Read/list operations return empty results so display code keeps
 * working, while write operations are rejected - there is no registry to write to in such contexts.
 */
@Lazy
@Service
public class ContributorServiceAdapter implements ContributorService {

    private static final String NOT_AVAILABLE = "contributor registry is not available in this context";

    @Override
    public List<ContributorEntry> search(String searchWord, SearchService.ContributorKind kind, int limit) {
        return List.of();
    }

    @Override
    public List<ContributorEntry> getAll(long skip, int limit) {
        return List.of();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public ContributorPage listManaged(String searchWord, SearchService.ContributorKind kind, List<ContributorIdType> hasIds,
                                       ContributorSortProperty sortBy, boolean ascending, long skip, int limit) {
        return new ContributorPage(List.of(), 0);
    }

    @Override
    public ContributorEntry getById(long id) {
        return null;
    }

    @Override
    public ContributorEntry create(ContributorEntry entry) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }

    @Override
    public ContributorEntry update(long id, ContributorEntry entry, boolean applyToExisting) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException(NOT_AVAILABLE);
    }
}
