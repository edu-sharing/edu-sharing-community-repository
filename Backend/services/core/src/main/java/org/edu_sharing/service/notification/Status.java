package org.edu_sharing.service.notification;

public enum Status {
    ADDED("added"),
    CHANGED("changed"),
    REMOVED("removed");

    private final String id;

    Status(String id){

        this.id = id;
    }


    @Override
    public String toString() {
        return id;
    }
}
