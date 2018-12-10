package org.edu_sharing.service.stream;

import org.edu_sharing.service.stream.model.ContentEntry;
import org.edu_sharing.service.stream.model.ScoreResult;
import org.edu_sharing.service.stream.model.StreamSearchRequest;
import org.edu_sharing.service.stream.model.StreamSearchResult;

import java.util.List;
import java.util.Map;

public class StreamServiceNone implements StreamService {
    @Override
    public String addEntry(ContentEntry entry) throws Exception {
        return null;
    }

    @Override
    public void updateEntry(ContentEntry entry) throws Exception {

    }

    @Override
    public ScoreResult getScoreByAuthority(String authority, ContentEntry.Audience.STATUS status) throws Exception {
        return null;
    }

    @Override
    public ContentEntry.Audience.STATUS getStatus(String id, List<String> list) throws Exception {
        return null;
    }

    @Override
    public void updateStatus(String id, String authority, ContentEntry.Audience.STATUS status) throws Exception {

    }

    @Override
    public StreamSearchResult search(StreamSearchRequest request) throws Exception {
        return null;
    }

    @Override
    public Map<String, Number> getTopValues(String property) throws Exception {
        return null;
    }

    @Override
    public ContentEntry getEntry(String entryId) throws Exception {
        return null;
    }

    @Override
    public boolean canAccessNode(List<String> authorities, String nodeId) throws Exception {
        return false;
    }

    @Override
    public void delete(String id) throws Exception {

    }
}
