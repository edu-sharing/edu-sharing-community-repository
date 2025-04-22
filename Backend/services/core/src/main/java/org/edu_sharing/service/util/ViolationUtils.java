package org.edu_sharing.service.util;

import jakarta.validation.ConstraintViolation;

public class ViolationUtils {

    public static String createTemplateMessageObject(String violationKind, String message){
        return String.join(":", violationKind, message);
    }

    public static String getViolationKind(ConstraintViolation<?> violation){
        return violation.getMessage().split(":")[0];
    }
    public static String getViolationMessage(ConstraintViolation<?> violation){
        String[] split = violation.getMessage().split(":");
        return split.length > 1 ? split[1] : split[0];
    }

}
