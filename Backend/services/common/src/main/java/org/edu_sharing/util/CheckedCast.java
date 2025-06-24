package org.edu_sharing.util;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface CheckedCast {

    static <T, R> List<R> toListOf(T prop, Class<R> type) {
        if(prop == null) {
            return null;
        }

        if (!(prop instanceof List<?>)) {
            throw new IllegalStateException("Property must be a List");
        }

        List<?> list = (List<?>) prop;
        if (list.stream().anyMatch(item -> !type.isInstance(item))) {
            throw new IllegalStateException("All elements in the list must be of type " + type.getSimpleName());
        }

        return list.stream()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    static <T> Function<Object, List<T>> wrapToListOf(Class<T> type) {
        return prop -> toListOf(prop, type);
    }
}
