package org.edu_sharing.service.search;

import java.io.Serializable;
import java.util.Objects;

public class Suggestion implements Serializable {
    private String key;
    private String displayString;

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setDisplayString(String displayString) {
        this.displayString = displayString;
    }

    public String getDisplayString() {
        return displayString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Suggestion that = (Suggestion) o;
        return Objects.equals(key, that.key) && Objects.equals(displayString, that.displayString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, displayString);
    }
}
