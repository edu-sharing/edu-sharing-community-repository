/*
 * 
 */

package org.edu_sharing.webservices.permission;

public interface Permission extends java.rmi.Remote {
    public java.lang.String checkCourse(java.lang.String in0, int in1) throws java.rmi.RemoteException;
    public boolean getPermission(java.lang.String session, int courseid, java.lang.String action, java.lang.String resourceid) throws java.rmi.RemoteException;
}
