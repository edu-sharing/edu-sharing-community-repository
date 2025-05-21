package org.edu_sharing.service.qa;

import org.edu_sharing.restservices.qa.v1.domain.CreateQAEntryDTO;
import org.edu_sharing.restservices.qa.v1.domain.UpdateQAEntryDTO;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface QAService {

    List<QAEntry> createQAEntries(@NotNull String nodeId, List<CreateQAEntryDTO> qaEntries);
    List<QAEntry> updateQAEntries(@NotNull String nodeId, List<UpdateQAEntryDTO> qaEntries);

    @NotNull List<QAEntry> getAllQAEntriesOf(@NotNull String nodeId, @Nullable String creator);

    void delete(@NotNull String nodeId, @Nullable String creator);

    void delete(@NotNull List<String> ids);
}
