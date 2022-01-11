/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.edu_sharing.webservices.util;

import java.util.Date;

/**
 * Helper class to contain web service authentication credentials
 * 
 * @author Roy Wetherall
 */
public class AuthenticationDetails
{
    /** The user name */
    private String userName;
    
    /** The ticket **/
    private String ticket;
    
    /** The session id **/
    private String sessionId;
    
    private Date wsLastTimeUsed;
    
    /** Default timeoutInterval = -1 (Tiket is never expired) */
    private long timeoutInterval = -1;
    
    /**
     * Constructor with deafault timeoutInterval = -1 (Tiket is never expired)
     * 
     * @param userName  the user name
     * @param ticket    the ticket
     * @param sessionId the session id
     */
    public AuthenticationDetails(String userName, String ticket, String sessionId)
    {
        this.userName = userName;
        this.ticket = ticket;
        this.sessionId = sessionId;
        
        wsLastTimeUsed = new Date();
    }
    
    /**
     * Constructor
     * 
     * @param userName  the user name
     * @param ticket    the ticket
     * @param sessionId the session id
     * @param timeoutInterval timeout interval
     * 
     */
    public AuthenticationDetails(String userName, String ticket, String sessionId, long timeoutInterval)
    {
        this.userName = userName;
        this.ticket = ticket;
        this.sessionId = sessionId;
        this.timeoutInterval = timeoutInterval;
        
        wsLastTimeUsed = new Date();
    }
    
    /**
     * Gets the user name
     * 
     * @return  the user name
     */
    public String getUserName()
    {
        return userName;
    }
    
    /**
     * Gets the ticket
     * 
     * @return  the ticket
     */
    public String getTicket()
    {
        return ticket;
    }
    
    /**
     * Gets the session id
     * 
     * @return  the sessio id, may return null if no session id is set
     */
    public String getSessionId()
    {
        return sessionId;
    }

	public long getTimeoutInterval() {
		return timeoutInterval;
	}

	public void setTimeoutInterval(long timeoutInterval) {
		this.timeoutInterval = timeoutInterval;
	}
	
	/**
	 * @return if timeoutInterval is not set return false. If current time > (wsLastTimeUsed.getTime() + timeoutInterval) return true
	 */
	public final boolean isTimedOut() {
		if (timeoutInterval < 1)
			return false;
		
		long nowInMillis = (new Date()).getTime();
		long expirationTimeInMillis = (wsLastTimeUsed.getTime() + timeoutInterval);
		
		return (nowInMillis > expirationTimeInMillis);
	}

	public void resetTimeoutInterval() {
		wsLastTimeUsed = new Date();
	}
}
