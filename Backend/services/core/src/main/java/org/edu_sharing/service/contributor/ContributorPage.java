package org.edu_sharing.service.contributor;

import java.util.List;

/**
 * A page of the contributor registry management list: the entries of the requested page
 * together with the total number of entries matching the filter (ignoring pagination).
 */
public record ContributorPage(List<ContributorEntry> entries, long total) {
}
