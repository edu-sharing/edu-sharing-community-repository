package org.edu_sharing.repository.server.jobs;

import lombok.Getter;

@Getter
public enum JobStatus {
    PENDING(0),
    RUNNING(1);

    private final int status;

    JobStatus(int status){
        this.status = status;
    }

    public static JobStatus getStatus(int status){
        for(JobStatus s : values()){
            if(s.getStatus() == status) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown JobStatus: " + status);
    }

    public static JobStatus getStatus(String status){
        for(JobStatus s : values()){
            if(s.name().equals(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown JobStatus: " + status);
    }
}
