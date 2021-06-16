package org.edu_sharing.service.rendering;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

public class RenderingServiceOptions {
    public String displayMode;
    public SavedSearchOptions savedSearch;

    public static RenderingServiceOptions fromRequestParameters(HttpServletRequest req) throws ParameterException {
        RenderingServiceOptions options = new RenderingServiceOptions();
        options.displayMode = req.getParameter("display");
        options.savedSearch = SavedSearchOptions.fromRequestParameters(req);
        return options;
    }

    /**
     * Options required for saved searches.
     *
     * Always save the relevant parameters, but only try to parse them, when they
     * are accessed. This way we get meaningful error messages about missing or
     * malformed parameters without disrupting rendering of other media, where these
     * parameters are not required.
     */
    public static class SavedSearchOptions {
        private String maxItems;
        private String sortBy;
        private String sortAscending;

        public static SavedSearchOptions fromRequestParameters(HttpServletRequest req) {
            SavedSearchOptions savedSearch = new SavedSearchOptions();
            savedSearch.maxItems = req.getParameter("maxItems");
            savedSearch.sortBy = req.getParameter("sortBy");
            savedSearch.sortAscending = req.getParameter("sortAscending");
            return savedSearch;
        }

        public Integer getMaxItems() throws ParameterException {
            return parseParam("maxItems", maxItems, Integer::parseInt);
        }

        public String getSortBy() throws ParameterException {
            return readParam("sortBy", sortBy);
        }

        public Boolean getSortAscending() throws ParameterException {
            return parseParam("sortAscending", sortAscending, Boolean::valueOf);
        }
    }

    public static class ParameterException extends Exception {
        public ParameterException(String message) {
            super(message);
        }
        public ParameterException(String message, Exception cause) {
            super(message, cause);
        }
    }

    private static String readParam(String key, String value) throws ParameterException {
        if (value == null || value.length() == 0) {
            throw new ParameterException(String.format("No such parameter: %s", key));
        }
        return value;
    }

    private static <T> T parseParam(String key, String value, Function<String, T> parse)
            throws ParameterException {
        value = readParam(key, value);
        try {
            return parse.apply(value);
        } catch (Exception e) {
            throw new ParameterException(
                    String.format("Failed to parse parameter \"%s\" with value \"%s\"", key, value), e);
        }
    }

}
