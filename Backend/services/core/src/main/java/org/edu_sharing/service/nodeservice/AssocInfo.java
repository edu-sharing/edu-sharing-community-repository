package org.edu_sharing.service.nodeservice;

public class AssocInfo {
    public static enum Direction{
        SOURCE,
        TARGET
    };
    private Direction direction;
    private String assocName;

    public AssocInfo(Direction direction, String assocName) {
        this.direction = direction;
        this.assocName = assocName;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getAssocName() {
        return assocName;
    }

    public void setAssocName(String assocName) {
        this.assocName = assocName;
    }
}
