/*
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.repo.webdav;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * Create a suitable HttpServletResponse when face with an exception.
 * 
 * @author Matt Ward
 */
public class ExceptionHandler
{
    private static final Log logger = LogFactory.getLog(ExceptionHandler.class); 
    private Throwable e;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    
    /**
     * Create an ExceptionHandler.
     * 
     * @param e Throwable
     * @param request HttpServletRequest
     * @param response HttpServletResponse
     */
    public ExceptionHandler(Throwable e, HttpServletRequest request, HttpServletResponse response)
    {
        this.e = e;
        this.request = request;
        this.response = response;
    }
    

    /**
     * edu-sharing fix: status to answer a rejected upload with, honouring the User-Agent dependent
     * mapping WebDAVMethod already maintains.
     */
    private int rejectedStatus()
    {
        int status = WebDAVMethod.getStatusForAccessDeniedException(request);
        // no or unrecognized User-Agent: a 401 would look like an authentication problem
        return (status == HttpServletResponse.SC_UNAUTHORIZED) ? HttpServletResponse.SC_FORBIDDEN : status;
    }

    public void handle() throws IOException
    {
        if (!(e instanceof WebDAVServerException) && e.getCause() != null)
        {
            if (e.getCause() instanceof WebDAVServerException)
            {
                e = e.getCause();
            }
        }
        //edu-sharing fix: a content rejection reaches us as a plain 500 whenever it was raised at
        //transaction commit rather than inside executeImpl - the antivirus behaviour binds itself with
        //NotificationFrequency.TRANSACTION_COMMIT, so WebDAVMethod.execute only ever sees an exception
        //it does not recognize and wraps it. Mapping it here covers both timings and every method, so
        //the client is told the actual reason instead of "an unexpected error".
        if (!(e instanceof Edu_SharingWebDAVServerException))
        {
            Edu_SharingWebDAVServerException rejection =
                    Edu_SharingWebDAVServerException.classify(e, rejectedStatus());
            if (rejection != null)
            {
                e = rejection;
            }
        }
        // Work out how to handle the error
        if (e instanceof WebDAVServerException)
        {
            WebDAVServerException error = (WebDAVServerException) e;
            if (error.getCause() != null)
            {
                if (logger.isDebugEnabled()) {
                    logger.error("Exception thrown.", e);
                }
            }

            if (logger.isDebugEnabled())
            {
                // Show what status code the method sent back
                
                logger.debug(request.getMethod() + " is returning status code: " + error.getHttpStatusCode());
            }

            if (response.isCommitted())
            {
                logger.warn("Could not return the status code to the client as the response has already been committed!");
            }
            else
            {
                //edu-sharing fix: MS-WDV extended error handling, so a client that understands it can
                //display the actual reason instead of its own generic message. Set before sendError(),
                //which keeps headers that are already on the response.
                if (error instanceof Edu_SharingWebDAVServerException)
                {
                    ((Edu_SharingWebDAVServerException) error).applyExtendedErrorHeader(response);
                }
                response.sendError(error.getHttpStatusCode());
            }
        }
        else
        {
            if (logger.isDebugEnabled()) {
                logger.error("Exception thrown.", e);
            }

            if (response.isCommitted())
            {
                logger.warn("Could not return the internal server error code to the client as the response has already been committed!");
            }
            else
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}
