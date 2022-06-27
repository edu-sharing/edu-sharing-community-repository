package org.edu_sharing.alfresco.service.search.cmis;

import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class Property extends Value {

    private static final Pattern pattern = Pattern.compile("^\\{http://[-a-zA-Z\\d+&@#/%?=~_|!:,.;]*[-a-zA-Z\\d+&@#/%=~_|]}.+");

    public Property(String value) {
        super(value);
        if (!check(value)) {
            throw new IllegalArgumentException("invalid format for value");
        }
    }

    public static boolean check(String s) {
        return pattern.matcher(s).matches();
    }
}
