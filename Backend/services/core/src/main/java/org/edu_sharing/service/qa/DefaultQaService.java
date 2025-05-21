package org.edu_sharing.service.qa;

import org.edu_sharing.restservices.qa.v1.domain.CreateOrUpdateQAEntryDTO;
import org.edu_sharing.service.qa.domain.QAEntry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DefaultQaService implements QAService {

    @Override
    public List<QAEntry> createQAEntries(@NotNull String nodeId, List<CreateOrUpdateQAEntryDTO> qaEntries) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QAEntry> updateQAEntries(@NotNull String nodeId, List<CreateOrUpdateQAEntryDTO> qaEntries) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @NotNull
    @Override
    public List<QAEntry> getAllQAEntriesOf(@NotNull String nodeId, String creator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(@NotNull String nodeId, String creator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void delete(@NotNull List<String> ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
