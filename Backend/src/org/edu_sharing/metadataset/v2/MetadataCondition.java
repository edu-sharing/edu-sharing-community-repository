package org.edu_sharing.metadataset.v2;

import java.io.Serializable;
import java.util.Objects;

public class MetadataCondition implements Serializable {
    public MetadataCondition(String value, CONDITION_TYPE type, boolean negate) {
        this.value = value;
        this.type = type;
        this.negate = negate;
    }
    public static enum CONDITION_TYPE{
        PROPERTY,
        TOOLPERMISSION
    };
    private String value;
    private CONDITION_TYPE type;
    private boolean negate;
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public CONDITION_TYPE getType() {
        return type;
    }
    public void setType(CONDITION_TYPE type) {
        this.type = type;
    }
    public boolean isNegate() {
        return negate;
    }
    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetadataCondition that = (MetadataCondition) o;
        return negate == that.negate &&
                Objects.equals(value, that.value) &&
                type == that.type;
    }
}
