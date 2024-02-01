package org.edu_sharing.service.usage;

import java.io.Serializable;

public class Usage2Exception extends Exception implements Serializable {
    private transient final Throwable cause;

    public Usage2Exception(Throwable cause) {
        this.cause = cause;
    }
}
