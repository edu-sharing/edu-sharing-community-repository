package org.edu_sharing.restservices.login.v1.model;

public class AuthenticationToken {
    String userId;
    String ticket;

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getTicket() {
        return ticket;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
