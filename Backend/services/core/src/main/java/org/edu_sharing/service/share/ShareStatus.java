package org.edu_sharing.service.share;

import lombok.Getter;

@Getter
public enum ShareStatus {
    SHARED(0),
    REJECTED(1);

    private final int status;

    ShareStatus(int status){
        this.status = status;
    }

    public static ShareStatus getStatus(int status){
        for(ShareStatus s : values()){
            if(s.getStatus() == status) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ShareStatus: " + status);
    }

    public static ShareStatus getStatus(String status){
        for(ShareStatus s : values()){
            if(s.name().equals(status)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ShareStatus: " + status);
    }
}
