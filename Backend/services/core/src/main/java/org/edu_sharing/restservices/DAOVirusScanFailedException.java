package org.edu_sharing.restservices;

public class DAOVirusScanFailedException extends DAOException {
    public DAOVirusScanFailedException(Throwable t, String nodeId){
            super(t,nodeId);
    }
}
