package org.edu_sharing.repository.server.tools;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

public class RequestHelper {
    private HttpServletRequest req;

    public RequestHelper(HttpServletRequest req) {
        this.req = req;
    }

    /**
     * Read a required request parameter.
     * 
     * @param key parameter key
     * @return parsed value
     * @throws IllegalArgumentException if the parameter is empty
     */
    public String readParam(String key) {
        String value = req.getParameter(key);
        if (value.length() == 0) {
            throw new IllegalArgumentException(
                String.format("Required parameter \"%s\" is missing", key)
            );
        }
        return value;
    }

    /**
     * Read and parse a required request parameter.
     * 
     * @param key   parameter key
     * @param parse parsing function to apply to the parameter value
     * @return parsed value
     * @throws IllegalArgumentException if the parameter is empty or could not be
     *                                  parsed
     */
    public <T> T parseParam(String key, Function<String, T> parse) {
        String value = this.readParam(key);
        try {
            return parse.apply(value);
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                String.format("Failed to parse parameter \"%s\" with value \"%s\"", key, value),
                e
            );
        }
    }
}
