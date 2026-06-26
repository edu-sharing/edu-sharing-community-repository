package org.edu_sharing.service.contributor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.service.search.SearchService;

import java.io.Serializable;
import java.util.Date;

/**
 * A managed contributor (author / organization) stored in the edu_contributor table.
 * Independent of the media nodes - it carries the canonical vcard plus the persistent ids
 * that identify the person/organization across the repository.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContributorEntry implements Serializable {
    private Long id;
    private SearchService.ContributorKind kind;
    private String title;
    private String givenname;
    private String surname;
    private String org;
    private String email;
    private String url;
    private String uid;
    private String orcid;
    private String gnduri;
    private String ror;
    private String wikidata;
    /** canonical vcard string (regenerated from the fields above) */
    private String vcard;
    private Date created;
    private Date lastUpdated;
}
