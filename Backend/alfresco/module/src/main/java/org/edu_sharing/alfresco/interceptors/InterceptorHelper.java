package org.edu_sharing.alfresco.interceptors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InterceptorHelper {
    public static<T> List<T> getInterceptors(List<String> className, ClassLoader classLoader) {
        try {
            ArrayList<Class<?>> clazz = className.stream().map((String className1) -> {
                try {
                    return Class.forName(className1, true, classLoader);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toCollection((Supplier<ArrayList<Class<?>>>) ArrayList::new));
            return ((List<T>)clazz.stream().map((c) -> {
                try {
                    return c.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList()));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }

    }
}
