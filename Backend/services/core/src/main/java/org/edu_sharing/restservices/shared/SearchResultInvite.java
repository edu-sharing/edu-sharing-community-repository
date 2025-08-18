package org.edu_sharing.restservices.shared;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.edu_sharing.restservices.UserShareDao;


@Data
@EqualsAndHashCode(callSuper = true)
public class SearchResultInvite extends SearchResult<UserShareDao.InviteEvent> {
}
