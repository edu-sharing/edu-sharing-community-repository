package org.edu_sharing.service.share;

import lombok.Getter;

@Getter
public enum ShareType {
    AUTHORITY(0),
    LINK(1);

    private final int type;

    ShareType(int type) {
        this.type = type;
    }

    public static ShareType getType(int type) {
        for (ShareType s : values()) {
            if (s.getType() == type) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ShareType: " + type);
    }

    public static ShareType getType(String type) {
        for (ShareType s : values()) {
            if (s.name().equals(type)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown ShareType: " + type);
    }
}

