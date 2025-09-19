package org.edu_sharing.repository.server.appcontext;


import java.lang.reflect.Proxy;

public final class ContextAwareProxyFactory {

    public static <T> T create(Class<T> type, AppContextServiceLocator locator) {
        Object proxy = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (p, method, args) -> {
                    T target = locator.get(type);
                    return method.invoke(target, args);
                }
        );
        @SuppressWarnings("unchecked")
        T typed = (T) proxy;
        return typed;
    }
}
