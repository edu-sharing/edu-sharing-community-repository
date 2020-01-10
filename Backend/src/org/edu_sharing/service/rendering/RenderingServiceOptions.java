package org.edu_sharing.service.rendering;

import java.util.List;

public class RenderingServiceOptions {
    public String displayMode;
    public SavedSearch savedSearch = new SavedSearch();

    public static class SavedSearch {
        public int count;
        public List<String> sortBy;
        public List<Boolean> sortAscending;
    }
}
