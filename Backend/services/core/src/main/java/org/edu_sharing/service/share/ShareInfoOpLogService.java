package org.edu_sharing.service.share;

import java.util.Date;
import java.util.List;

public interface ShareInfoOpLogService {
    /**
     * @param afterTxId when afterDate is null: a standalone id cursor (id &gt; afterTxId).
     *                  When afterDate is also given: a tiebreaker for oplog rows sharing the
     *                  exact same afterDate timestamp, forming a combined (timestamp, id) cursor.
     */
    List<ShareInfoOplog> getOplogs(Long afterTxId, Date afterDate, Date untilDate, int limit);
}
