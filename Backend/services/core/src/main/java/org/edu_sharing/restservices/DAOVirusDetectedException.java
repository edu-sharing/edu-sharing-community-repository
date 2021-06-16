package org.edu_sharing.restservices;

public class DAOVirusDetectedException extends DAOException {
    public DAOVirusDetectedException(Throwable t, String nodeId){
        super(t,nodeId);
    }
}
