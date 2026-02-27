package org.edu_sharing.service.share;

import java.util.Date;
import java.util.List;

public interface ShareInfoOpLogService {
    List<ShareInfoOplog> getOplogs(Long afterTxId, Date afterDate, Date untilDate, int limit);
}
