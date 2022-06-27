package org.edu_sharing.alfresco.service.search.cmis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Value extends Argument {
    private final String value;

    public static <T> Value create(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value required");
        }

        if (value instanceof Value) {
            return (Value) value;
        }

        if(value instanceof Argument){
            throw new IllegalArgumentException(String.format("Unknown argument type: %s", value.getClass().getName()));
        }

        if (value instanceof String) {
            if (Property.check((String) value)) {
                return new Property((String) value);
            }
            return new Value((String) value);
        }

        return new Value(value.toString());
    }
}
