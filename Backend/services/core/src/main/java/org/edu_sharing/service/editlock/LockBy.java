package org.edu_sharing.service.editlock;

import java.io.Serializable;
import java.util.Date;

public class LockBy implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	String userName;
	String sessionId;
	Date date;
	
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	
	
}
