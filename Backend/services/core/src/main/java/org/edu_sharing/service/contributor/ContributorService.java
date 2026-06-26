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
