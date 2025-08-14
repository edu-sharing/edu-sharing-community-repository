package org.edu_sharing.service.share;

import lombok.Getter;

@Getter
public enum OpLogAction {
    CREATE(0),
    UPDATE(1),
    DELETE(2),
    ;

    private final int id;

    OpLogAction(int id) {
        this.id = id;
    }

    public static OpLogAction getAction(int id){
        for(OpLogAction a : values()){
            if(a.getId() == id) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown OpLogAction: " + id);
    }

    public static OpLogAction getAction(String action){
        for(OpLogAction a : values()){
            if(a.name().equals(action)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown OpLogAction: " + action);
    }

}
