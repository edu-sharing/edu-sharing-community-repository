package org.edu_sharing.service.search;

import java.io.Serializable;

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
}
