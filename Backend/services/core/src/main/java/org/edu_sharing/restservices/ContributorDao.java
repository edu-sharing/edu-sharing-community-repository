package org.edu_sharing.restservices;

import org.edu_sharing.restservices.contributor.v1.model.ContributorData;
import org.edu_sharing.restservices.contributor.v1.model.ContributorSearchResult;
import org.edu_sharing.restservices.contributor.v1.model.CreateContributorRequest;
import org.edu_sharing.restservices.contributor.v1.model.UpdateContributorRequest;
import org.edu_sharing.restservices.shared.Pagination;
import org.edu_sharing.service.contributor.ContributorEntry;
import org.edu_sharing.service.contributor.ContributorIdType;
import org.edu_sharing.service.contributor.ContributorPage;
import org.edu_sharing.service.contributor.ContributorService;
import org.edu_sharing.service.contributor.ContributorSortProperty;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.spring.ApplicationContextFactory;

import java.util.List;

public class ContributorDao {

    private final ContributorService contributorService;

    public ContributorDao(RepositoryDao repoDao) {
        this.contributorService = ApplicationContextFactory.getApplicationContext().getBean(ContributorService.class);
    }

    public List<ContributorData> search(String searchWord, SearchService.ContributorKind kind, int limit) {
        return contributorService.search(searchWord, kind, limit).stream().map(ContributorDao::mapToData).toList();
    }

    public ContributorSearchResult searchManaged(String searchWord, SearchService.ContributorKind kind,
                                                  List<ContributorIdType> hasIds, ContributorSortProperty sortBy,
                                                  boolean ascending, int skip, int limit) {
        ContributorPage page = contributorService.listManaged(searchWord, kind, hasIds, sortBy, ascending, skip, limit);
        List<ContributorData> contributors = page.entries().stream().map(ContributorDao::mapToData).toList();
        return ContributorSearchResult.builder()
                .contributors(contributors)
                .pagination(new Pagination(skip, (int) page.total(), contributors.size()))
                .build();
    }

    public List<ContributorData> getAll(long skip, int limit) {
        return contributorService.getAll(skip, limit).stream().map(ContributorDao::mapToData).toList();
    }

    public long count() {
        return contributorService.count();
    }

    public ContributorData getById(long id) throws DAOMissingException {
        ContributorEntry entry = contributorService.getById(id);
        if (entry == null) {
            throw new DAOMissingException(new IllegalArgumentException("Contributor " + id + " not found"));
        }
        return mapToData(entry);
    }

    public ContributorData create(CreateContributorRequest request) throws DAOValidationException {
        try {
            ContributorEntry entry = ContributorEntry.builder()
                    .kind(request.kind())
                    .title(request.title())
                    .givenname(request.givenname())
                    .surname(request.surname())
                    .org(request.org())
                    .email(request.email())
                    .url(request.url())
                    .uid(request.uid())
                    .orcid(request.orcid())
                    .gnduri(request.gnduri())
                    .ror(request.ror())
                    .wikidata(request.wikidata())
                    .build();
            return mapToData(contributorService.create(entry));
        } catch (IllegalArgumentException e) {
            throw new DAOValidationException(e);
        }
    }

    public ContributorData update(long id, UpdateContributorRequest request) throws DAOValidationException {
        try {
            ContributorEntry entry = ContributorEntry.builder()
                    .kind(request.kind())
                    .title(request.title())
                    .givenname(request.givenname())
                    .surname(request.surname())
                    .org(request.org())
                    .email(request.email())
                    .url(request.url())
                    .uid(request.uid())
                    .orcid(request.orcid())
                    .gnduri(request.gnduri())
                    .ror(request.ror())
                    .wikidata(request.wikidata())
                    .build();
            return mapToData(contributorService.update(id, entry, request.applyToExisting()));
        } catch (IllegalArgumentException e) {
            throw new DAOValidationException(e);
        }
    }

    public void delete(long id) {
        contributorService.delete(id);
    }

    private static ContributorData mapToData(ContributorEntry entry) {
        return ContributorData.builder()
                .id(entry.getId())
                .kind(entry.getKind())
                .title(entry.getTitle())
                .givenname(entry.getGivenname())
                .surname(entry.getSurname())
                .org(entry.getOrg())
                .email(entry.getEmail())
                .url(entry.getUrl())
                .uid(entry.getUid())
                .orcid(entry.getOrcid())
                .gnduri(entry.getGnduri())
                .ror(entry.getRor())
                .wikidata(entry.getWikidata())
                .vcard(entry.getVcard())
                .created(entry.getCreated())
                .lastUpdated(entry.getLastUpdated())
                .build();
    }
}
