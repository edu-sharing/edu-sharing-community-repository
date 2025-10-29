package org.edu_sharing.alfresco.service.search.cmis;

import java.util.regex.Pattern;

public record Aspect(String value) {

    private static final Pattern pattern = Pattern.compile("^\\{http://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*[-a-zA-Z\\d+&@#/%=~_|]}.+");

    public Aspect {
        if (!check(value)) {
            throw new IllegalArgumentException("invalid format for value");
        }

    }

    public static boolean check(String s) {
        return pattern.matcher(s).matches();
    }
}
