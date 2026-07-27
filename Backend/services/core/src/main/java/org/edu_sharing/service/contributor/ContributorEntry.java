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
    /** username that introduced this entry (e.g. captured by the contributor registry policy); null for admin/migration */
    private String creator;
    private Date created;
    private Date lastUpdated;

    /**
     * Identity key built from the kind and all persistent ids. Used to deduplicate entries that
     * denote the same person/organization (e.g. the same id surfacing in several contributor roles
     * or vcard formattings) before they are inserted into the registry.
     */
    public String idKey() {
        return kind + "|" + orcid + "|" + gnduri + "|" + ror + "|" + wikidata + "|" + email;
    }
}
