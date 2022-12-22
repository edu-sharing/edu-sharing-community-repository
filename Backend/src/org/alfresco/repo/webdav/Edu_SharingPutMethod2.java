package org.alfresco.repo.webdav;

import org.edu_sharing.alfresco.policy.NodeMimetypeValidationException;

import javax.servlet.http.HttpServletResponse;

public class Edu_SharingPutMethod2 extends PutMethod {
    @Override
    protected void executeImpl() throws WebDAVServerException, Exception {
        try {
            super.executeImpl();
        } catch(WebDAVServerException e) {
            /*
             * edu-sharing customization
             */
            if(e.getHttpStatusCode() == HttpServletResponse.SC_INTERNAL_SERVER_ERROR && e.getCause() != null && e.getCause().getCause() instanceof NodeMimetypeValidationException) {
                throw new WebDAVServerException(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, e.getCause());
            }
            throw e;
        }
    }
}
