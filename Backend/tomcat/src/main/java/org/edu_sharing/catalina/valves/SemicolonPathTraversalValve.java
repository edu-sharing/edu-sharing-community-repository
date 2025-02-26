package org.edu_sharing.catalina.valves;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.connector.Request;
import org.apache.catalina.valves.ValveBase;

import java.io.IOException;
import java.util.regex.Pattern;

public class SemicolonPathTraversalValve extends ValveBase {
    // Pattern to detect semicolon-based path traversal
    private static final Pattern TRAVERSAL_PATTERN = Pattern.compile(".*[.]{2,}[/\\\\]*;.*");

    @Override
    public void invoke(Request request, org.apache.catalina.connector.Response response)
            throws IOException, ServletException {

        String requestURI = ((HttpServletRequest)request).getRequestURI(); // Get the decoded request URI

        if (TRAVERSAL_PATTERN.matcher(requestURI).matches()) {
            // Log the attack attempt
            getContainer().getLogger().warn("Blocked path traversal attempt: " + requestURI);

            // Send a 403 Forbidden response
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Blocked path traversal attempt.");
            return;
        }

        // Pass request to the next Valve in the chain
        getNext().invoke(request, response);
    }
}
