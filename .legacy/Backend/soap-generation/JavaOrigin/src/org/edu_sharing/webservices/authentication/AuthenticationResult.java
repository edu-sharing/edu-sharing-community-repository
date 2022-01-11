/*
 * 
 */

package org.edu_sharing.webservices.authentication;

public class AuthenticationResult  implements java.io.Serializable {
    private java.lang.String courseId;

    private java.lang.String email;

    private java.lang.String givenname;

    private java.lang.String sessionid;

    private java.lang.String surname;

    private java.lang.String ticket;

    private java.lang.String username;


    public AuthenticationResult() {
    }

    public AuthenticationResult(
           java.lang.String courseId,
           java.lang.String email,
           java.lang.String givenname,
           java.lang.String sessionid,
           java.lang.String surname,
           java.lang.String ticket,
           java.lang.String username,
           java.lang.String accessToken,
           java.lang.String refreshToken,
           long tokenExpiresIn) {
           this.courseId = courseId;
           this.email = email;
           this.givenname = givenname;
           this.sessionid = sessionid;
           this.surname = surname;
           this.ticket = ticket;
           this.username = username;
    }


    /**
     * Gets the courseId value for this AuthenticationResult.
     * 
     * @return courseId
     */
    public java.lang.String getCourseId() {
        return courseId;
    }


    /**
     * Sets the courseId value for this AuthenticationResult.
     * 
     * @param courseId
     */
    public void setCourseId(java.lang.String courseId) {
        this.courseId = courseId;
    }


    /**
     * Gets the email value for this AuthenticationResult.
     * 
     * @return email
     */
    public java.lang.String getEmail() {
        return email;
    }


    /**
     * Sets the email value for this AuthenticationResult.
     * 
     * @param email
     */
    public void setEmail(java.lang.String email) {
        this.email = email;
    }


    /**
     * Gets the givenname value for this AuthenticationResult.
     * 
     * @return givenname
     */
    public java.lang.String getGivenname() {
        return givenname;
    }


    /**
     * Sets the givenname value for this AuthenticationResult.
     * 
     * @param givenname
     */
    public void setGivenname(java.lang.String givenname) {
        this.givenname = givenname;
    }


    /**
     * Gets the sessionid value for this AuthenticationResult.
     * 
     * @return sessionid
     */
    public java.lang.String getSessionid() {
        return sessionid;
    }


    /**
     * Sets the sessionid value for this AuthenticationResult.
     * 
     * @param sessionid
     */
    public void setSessionid(java.lang.String sessionid) {
        this.sessionid = sessionid;
    }


    /**
     * Gets the surname value for this AuthenticationResult.
     * 
     * @return surname
     */
    public java.lang.String getSurname() {
        return surname;
    }


    /**
     * Sets the surname value for this AuthenticationResult.
     * 
     * @param surname
     */
    public void setSurname(java.lang.String surname) {
        this.surname = surname;
    }


    /**
     * Gets the ticket value for this AuthenticationResult.
     * 
     * @return ticket
     */
    public java.lang.String getTicket() {
        return ticket;
    }


    /**
     * Sets the ticket value for this AuthenticationResult.
     * 
     * @param ticket
     */
    public void setTicket(java.lang.String ticket) {
        this.ticket = ticket;
    }


    /**
     * Gets the username value for this AuthenticationResult.
     * 
     * @return username
     */
    public java.lang.String getUsername() {
        return username;
    }


    /**
     * Sets the username value for this AuthenticationResult.
     * 
     * @param username
     */
    public void setUsername(java.lang.String username) {
        this.username = username;
    }
}
