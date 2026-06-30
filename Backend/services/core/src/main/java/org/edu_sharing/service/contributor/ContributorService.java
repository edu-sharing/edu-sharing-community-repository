package org.edu_sharing.service.contributor;

import org.edu_sharing.service.search.SearchService;

import java.util.List;

/**
 * Manages the registry of contributors (authors / organizations) with persistent ids,
 * stored autonomously in the {@code edu_contributor} table - independent of the media nodes.
 */
public interface ContributorService {

    /**
     * Autocomplete search over the registry. Public (used by the contributor autocomplete endpoint).
     */
    List<ContributorEntry> search(String searchWord, SearchService.ContributorKind kind, int limit);

    /** List all entries (management view). */
    List<ContributorEntry> getAll(long skip, int limit);

    long count();

    /**
     * Filtered, sorted and paginated management list with the total match count.
     * Management-only (requires {@code TOOLPERMISSION_MANAGE_CONTRIBUTORS}).
     *
     * @param searchWord    optional case-insensitive search over name/org/email
     * @param kind          optional kind filter
     * @param hasIds        optional filter: only entries carrying at least one of the given id types
     * @param sortBy        whitelisted sort column (defaults to {@link ContributorSortProperty#NAME} when null)
     * @param ascending     sort direction
     * @param skip          pagination offset
     * @param limit         page size
     */
    ContributorPage listManaged(String searchWord, SearchService.ContributorKind kind, List<ContributorIdType> hasIds,
                                ContributorSortProperty sortBy, boolean ascending, long skip, int limit);

    ContributorEntry getById(long id);

    /** Create a new entry. Requires at least one persistent id. */
    ContributorEntry create(ContributorEntry entry);

    /**
     * Update an entry. If {@code applyToExisting} is true, the change is asynchronously propagated to all
     * media nodes that currently carry this contributor (full-field match against the state before the change).
     */
    ContributorEntry update(long id, ContributorEntry entry, boolean applyToExisting);

    /** Delete an entry from the registry. Does NOT touch any media node. */
    void delete(long id);
}
