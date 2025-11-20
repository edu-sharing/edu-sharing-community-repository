package org.edu_sharing.restservices;

public class MissingResourceException extends RuntimeException{
    public MissingResourceException(String s) {
        super(s);
    }
    public MissingResourceException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
