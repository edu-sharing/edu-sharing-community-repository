package org.edu_sharing.restservices.contributor.v1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.edu_sharing.service.search.SearchService;

import java.util.Date;

@Value
@Builder
public class ContributorData {
    @JsonProperty(required = true)
    Long id;
    @JsonProperty(required = true)
    SearchService.ContributorKind kind;
    String title;
    String givenname;
    String surname;
    String org;
    String email;
    String url;
    String uid;
    String orcid;
    String gnduri;
    String ror;
    String wikidata;
    @JsonProperty(required = true)
    String vcard;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date created;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    Date lastUpdated;
}
